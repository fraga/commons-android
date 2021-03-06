package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public abstract class AgencyProvider extends MTContentProvider implements AgencyProviderContract {

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "ping", ContentProviderConstants.PING);
		uriMatcher.addURI(authority, "version", ContentProviderConstants.VERSION);
		uriMatcher.addURI(authority, "deployed", ContentProviderConstants.DEPLOYED);
		uriMatcher.addURI(authority, "label", ContentProviderConstants.LABEL);
		uriMatcher.addURI(authority, "color", ContentProviderConstants.COLOR);
		uriMatcher.addURI(authority, "shortName", ContentProviderConstants.SHORT_NAME);
		uriMatcher.addURI(authority, "setuprequired", ContentProviderConstants.SETUP_REQUIRED);
		uriMatcher.addURI(authority, "area", ContentProviderConstants.AREA);
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
			ping();
			deploySync();
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.VERSION:
			return getVersion();
		case ContentProviderConstants.LABEL:
			return getLabel();
		case ContentProviderConstants.COLOR:
			return getColor();
		case ContentProviderConstants.SHORT_NAME:
			return getShortName();
		case ContentProviderConstants.DEPLOYED:
			return isDeployed();
		case ContentProviderConstants.SETUP_REQUIRED:
			return isSetupRequired();
		case ContentProviderConstants.AREA:
			return getArea();
		default:
			return null; // not processed
		}
	}

	private void deploySync() {
		SQLiteDatabase db = null;
		try {
			db = getDBHelper().getReadableDatabase(); // trigger create/update DB if necessary
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deploying DB!");
		} finally {
			SqlUtils.close(db);
		}
	}

	public String getSortOrder(Uri uri) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.DEPLOYED:
		case ContentProviderConstants.LABEL:
		case ContentProviderConstants.COLOR:
		case ContentProviderConstants.SHORT_NAME:
		case ContentProviderConstants.VERSION:
		case ContentProviderConstants.SETUP_REQUIRED:
		case ContentProviderConstants.AREA:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
		}
	}

	@Override
	public String getTypeMT(Uri uri) {
		switch (getAgencyUriMatcher().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.DEPLOYED:
		case ContentProviderConstants.LABEL:
		case ContentProviderConstants.COLOR:
		case ContentProviderConstants.SHORT_NAME:
		case ContentProviderConstants.VERSION:
		case ContentProviderConstants.SETUP_REQUIRED:
		case ContentProviderConstants.AREA:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	public abstract UriMatcher getAgencyUriMatcher();

	private Cursor getVersion() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "version" });
		matrixCursor.addRow(new Object[] { getAgencyVersion() });
		return matrixCursor;
	}

	public abstract int getAgencyVersion();

	private Cursor getLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "label" });
		matrixCursor.addRow(new Object[] { getContext().getString(getAgencyLabelResId()) });
		return matrixCursor;
	}

	public abstract int getAgencyLabelResId();

	public Cursor getColor() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "color" });
		matrixCursor.addRow(new Object[] { getAgencyColorString(getContext()) });
		return matrixCursor;
	}

	public abstract String getAgencyColorString(Context context);

	private Cursor getShortName() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "shortName" });
		matrixCursor.addRow(new Object[] { getContext().getString(getAgencyShortNameResId()) });
		return matrixCursor;
	}

	public abstract int getAgencyShortNameResId();

	private Cursor isDeployed() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "deployed" });
		matrixCursor.addRow(new Object[] { isAgencyDeployed() ? 1 : 0 });
		return matrixCursor;
	}

	public abstract boolean isAgencyDeployed();

	private Cursor isSetupRequired() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "setuprequired" });
		matrixCursor.addRow(new Object[] { isAgencySetupRequired() ? 1 : 0 });
		return matrixCursor;
	}

	public abstract boolean isAgencySetupRequired();

	private Cursor getArea() {
		Area area = getAgencyArea(getContext());
		return Area.toCursor(area);
	}

	public abstract Area getAgencyArea(Context context);
}
