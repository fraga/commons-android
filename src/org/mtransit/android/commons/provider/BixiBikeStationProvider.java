package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.BikeStationAvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.database.Cursor;

public class BixiBikeStationProvider extends BikeStationProvider {

	private static final String TAG = BixiBikeStationProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link BixiBikeStationProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_LAST_UPDATE_MS;

	@Override
	public void updateBikeStationDataIfRequired() {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l);
		long nowInMs = TimeUtils.currentTimeMillis();
		// MAX VALIDITY (too old to display?)
		if (lastUpdateInMs + getBIKE_STATION_MAX_VALIDITY_IN_MS() < nowInMs) {
			deleteAllBikeStationData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		// VALIDITY (try to refresh?)
		if (lastUpdateInMs + getBIKE_STATION_VALIDITY_IN_MS() < nowInMs) {
			updateAllDataFromWWW(lastUpdateInMs);
		}
		// ELSE USE CURRENT DATA
	}

	@Override
	public Cursor getPOIBikeStations(POIFilter poiFilter) {
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(String targetUUID) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l);
		long nowInMs = TimeUtils.currentTimeMillis();
		// MAX VALIDITY (too old to display?)
		if (lastUpdateInMs + getStatusMaxValidityInMs() < nowInMs) {
			deleteAllBikeStationStatusData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		// VALIDITY (try to refresh?)
		if (lastUpdateInMs + getStatusValidityInMs() < nowInMs) {
			updateAllDataFromWWW(lastUpdateInMs);
		}
		// ELSE USE CURRENT DATA
	}

	@Override
	public POIStatus getNewBikeStationStatus(AvailabilityPercentStatusFilter filter) {
		updateBikeStationStatusDataIfRequired(filter.getTargetUUID());// getBikeStationId());
		return getCachedStatus(filter.getTargetUUID());
	}

	private synchronized void updateAllDataFromWWW(long oldLastUpdatedInMs) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l) > oldLastUpdatedInMs) {
			return; // too late, another thread already updated
		}
		// fetch data form WWW
		loadDataFromWWW(0); // 0 = 1st try
	}

	private static final int MAX_RETRY = 1;

	private List<DefaultPOI> loadDataFromWWW(int tried) {
		try {
			final String urlString = getDATA_URL(getContext());
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			urlc.addRequestProperty("Cache-Control", "no-cache"); // IMPORTANT!
			HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlc;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				BixiBikeStationsDataHandler handler = new BixiBikeStationsDataHandler(this, getContext(), newLastUpdateInMs, getStatusMaxValidityInMs(),
						getValue1Color(getContext()), getValue1ColorBg(getContext()), getValue2Color(getContext()), getValue2ColorBg(getContext()));
				xr.setContentHandler(handler);
				xr.parse(new InputSource(urlc.getInputStream()));
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, handler.getBikeStations());
				deleteAllBikeStationStatusData();
				insertBikeStationStatus(handler.getBikeStationsStatus());
				PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
				return handler.getBikeStations();
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpsUrlConnection.getResponseCode(),
						httpsUrlConnection.getResponseMessage());
				if (tried < MAX_RETRY) {
					return loadDataFromWWW(++tried);
				} else {
					return null;
				}
			}
		} catch (SSLHandshakeException sslhe) {
			MTLog.w(this, sslhe, "SSL error!");
			if (tried < MAX_RETRY) {
				return loadDataFromWWW(++tried);
			} else {
				return null;
			}
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
			return null;
		} catch (SocketException se) {
			MTLog.w(TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			if (tried < MAX_RETRY) {
				return loadDataFromWWW(++tried);
			} else {
				return null;
			}
		}
	}

	private static final String PLACE_CHAR_D = "d'";
	private static final String PLACE_CHAR_DE = "de ";
	private static final String PLACE_CHAR_DES = "des ";
	private static final String PLACE_CHAR_DU = "du ";
	private static final String PLACE_CHAR_LA = "la ";
	private static final String PLACE_CHAR_LE = "le ";
	private static final String PLACE_CHAR_LES = "les ";
	private static final String PLACE_CHAR_L = "l'";

	private static final String[] REMOVE_CHARS = new String[] { PLACE_CHAR_D, PLACE_CHAR_DE, PLACE_CHAR_DES, PLACE_CHAR_DU, PLACE_CHAR_LA, PLACE_CHAR_LE,
			PLACE_CHAR_LES, PLACE_CHAR_L };

	private static final String[] REPLACE_CHARS = new String[] { " " + PLACE_CHAR_D, " " + PLACE_CHAR_DE, " " + PLACE_CHAR_DES, " " + PLACE_CHAR_DU,
			" " + PLACE_CHAR_LA, " " + PLACE_CHAR_LE, " " + PLACE_CHAR_LES, " " + PLACE_CHAR_L };

	private static final String PLACE_CHAR_SAINT = "saint";
	private static final String PLACE_CHAR_SAINT_REPLACEMENT = "st";

	private static final String SLASH = "/";
	private static final Pattern CLEAN_SUBWAY = Pattern.compile("(métro)([^" + PARENTHESE1 + "]*)" + PARENTHESE1 + "([^" + SLASH + "]*)" + SLASH + "([^"
			+ PARENTHESE2 + "]*)" + PARENTHESE2);
	private static final String CLEAN_SUBWAY_REPLACEMENT = "$3 " + SLASH + " $4 " + PARENTHESE1 + "$2" + PARENTHESE2 + "";

	protected static String cleanBixiBikeStationName(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		name = CLEAN_SLASHES.matcher(name).replaceAll(CLEAN_SLASHES_REPLACEMENT);
		// clean words
		name = name.toLowerCase(Locale.ENGLISH);
		name = StringUtils.removeStartWith(name, REMOVE_CHARS, 1); // 1 = keep space
		name = StringUtils.replaceAll(name, REPLACE_CHARS, " ");
		name = name.replace(PLACE_CHAR_SAINT, PLACE_CHAR_SAINT_REPLACEMENT);
		name = CLEAN_SUBWAY.matcher(name).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		return cleanBikeStationName(name);
	}

	private static class BixiBikeStationsDataHandler extends MTDefaultHandler implements ContentHandler {

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final String STATIONS = "stations";
		private static final String STATIONS_VERSION = "version"; // 2.0
		private static final String STATION = "station";
		private static final String ID = "id";
		private static final String NAME = "name";
		private static final String TERMINAL_NAME = "terminalName";
		private static final String LAST_COMM_WITH_SERVER = "lastCommWithServer";
		private static final String LAT = "lat";
		private static final String LONG = "long";
		private static final String INSTALLED = "installed";
		private static final String LOCKED = "locked";
		private static final String INSTALL_DATE = "installDate";
		private static final String REMOVAL_DATE = "removalDate";
		private static final String TEMPORARY = "temporary";
		private static final String PUBLIC = "public";
		private static final String NB_BIKES = "nbBikes";
		private static final String NB_EMPTY_DOCKS = "nbEmptyDocks";
		private static final String LATEST_UPDATE_TIME = "latestUpdateTime";

		private static final String SUPPORTED_VERSIONS = "2.0";

		private String currentLocalName = STATIONS;

		private Context context;
		private long newLastUpdateInMs;
		private long maxValidityInMs;

		private List<DefaultPOI> bikeStations = new ArrayList<DefaultPOI>();

		private List<BikeStationAvailabilityPercent> bikeStationsStatus = new ArrayList<BikeStationAvailabilityPercent>();

		private DefaultPOI currentBikeStation = null;
		private BikeStationAvailabilityPercent currentBikeStationStatus = null;
		private int value1Color;
		private int value1ColorBg;
		private int value2Color;
		private int value2ColorBg;

		public BixiBikeStationsDataHandler(BixiBikeStationProvider provider, Context context, long newLastUpdateInMs, long maxValidityInMs, int value1Color,
				int value1ColorBg, int value2Color, int value2ColorBg) {
			this.context = context;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.maxValidityInMs = maxValidityInMs;
			this.value1Color = value1Color;
			this.value1ColorBg = value1ColorBg;
			this.value2Color = value2Color;
			this.value2ColorBg = value2ColorBg;
		}

		public List<DefaultPOI> getBikeStations() {
			return this.bikeStations;
		}

		public List<BikeStationAvailabilityPercent> getBikeStationsStatus() {
			return bikeStationsStatus;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (STATIONS.equals(localName)) {
				String version = attributes.getValue(STATIONS_VERSION);
				if (version == null || !SUPPORTED_VERSIONS.equals(version)) {
					MTLog.w(this, "XML version '%s' not supported!", version);
				}
			} else if (STATION.equals(localName)) {
				this.currentBikeStation = new DefaultPOI(getAUTHORITY(this.context), POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT);
				this.currentBikeStationStatus = new BikeStationAvailabilityPercent(-1, null, this.newLastUpdateInMs, this.maxValidityInMs, this.value1Color,
						this.value1ColorBg, this.value2Color, this.value2ColorBg);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			if (this.currentBikeStation != null && this.currentBikeStationStatus != null) {
				try {
					String string = new String(ch, start, length).trim();
					if (ID.equals(this.currentLocalName)) {
						// do not store source ID as it only represents the current position in the XML list
					} else if (NAME.equals(this.currentLocalName)) {
						this.currentBikeStation.setName(cleanBixiBikeStationName(string));
					} else if (TERMINAL_NAME.equals(this.currentLocalName)) {
						final int bikeStationId = Integer.parseInt(string);
						this.currentBikeStation.setId(bikeStationId);
					} else if (LAST_COMM_WITH_SERVER.equals(this.currentLocalName)) {
					} else if (LAT.equals(this.currentLocalName)) {
						this.currentBikeStation.setLat(Double.valueOf(string));
					} else if (LONG.equals(this.currentLocalName)) {
						this.currentBikeStation.setLng(Double.valueOf(string));
					} else if (INSTALLED.equals(this.currentLocalName)) {
						this.currentBikeStationStatus.setStatusInstalled(Boolean.parseBoolean(string));
					} else if (LOCKED.equals(this.currentLocalName)) {
						this.currentBikeStationStatus.setStatusLocked(Boolean.parseBoolean(string));
					} else if (INSTALL_DATE.equals(this.currentLocalName)) {
					} else if (REMOVAL_DATE.equals(this.currentLocalName)) {
					} else if (TEMPORARY.equals(this.currentLocalName)) {
					} else if (PUBLIC.equals(this.currentLocalName)) {
						this.currentBikeStationStatus.setStatusPublic(Boolean.parseBoolean(string));
					} else if (NB_BIKES.equals(this.currentLocalName)) {
						this.currentBikeStationStatus.setValue1(Integer.parseInt(string));
					} else if (NB_EMPTY_DOCKS.equals(this.currentLocalName)) {
						this.currentBikeStationStatus.setValue2(Integer.parseInt(string));
					} else if (LATEST_UPDATE_TIME.equals(this.currentLocalName)) {
						// using local device time instead of web server time
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while parsing '%s, %s, %s'!", ch, start, length);
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (STATION.equals(localName)) {
				this.bikeStations.add(this.currentBikeStation);
				this.currentBikeStationStatus.setTargetUUID(this.currentBikeStation.getUUID());
				this.bikeStationsStatus.add(this.currentBikeStationStatus);
				//
				this.currentBikeStation = null;
				this.currentBikeStationStatus = null;
			}
		}
	}

}
