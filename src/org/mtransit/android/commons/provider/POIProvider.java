package org.mtransit.android.commons.provider;

import java.util.Collection;
import java.util.HashMap;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI.POIUtils;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

@SuppressLint("Registered")
public class POIProvider extends MTContentProvider implements POIProviderContract {

	private static final String TAG = POIProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, POIProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, POIProviderContract.POI_PATH, ContentProviderConstants.POI);
		uriMatcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, ContentProviderConstants.SEARCH_SUGGEST_EMPTY);
		uriMatcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", ContentProviderConstants.SEARCH_SUGGEST_QUERY);
	}

	private static final String[] SUGGEST_SEARCHABLE_COLUMNS = new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1 };

	private static final String[] SEARCHABLE_LIKE_COLUMNS = new String[] { POIProviderContract.Columns.T_POI_K_NAME };

	private static final String[] SEARCHABLE_EQUALS_COLUMNS = new String[] {};

	public static final HashMap<String, String> POI_SEARCH_SUGGEST_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew() //
			.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_NAME, SearchManager.SUGGEST_COLUMN_TEXT_1) //
			.build();

	private static POIDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.poi_authority);
		}
		return authority;
	}

	private static Integer dataSourceTypeId = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public static int getTYPE_ID(Context context) {
		if (dataSourceTypeId == null) {
			dataSourceTypeId = context.getResources().getInteger(R.integer.poi_agency_type);
		}
		return dataSourceTypeId;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		PackageManagerUtils.removeModuleLauncherIcon(getContext());
	}

	private POIDbHelper getDBHelper(Context context) {
		if (dbHelper == null) { // initialize
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Exception e) { // fail if locked, will try again later
				MTLog.w(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return POIDbHelper.getDbVersion();
	}

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public POIDbHelper getNewDbHelper(Context context) {
		return new POIDbHelper(context.getApplicationContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return queryS(this, uri, selection);
	}

	public static Cursor queryS(POIProviderContract provider, Uri uri, String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.POI:
			return getPOI(provider, selection);
		case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
			return getSearchSuggest(provider, null);
		case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
			return getSearchSuggest(provider, uri.getLastPathSegment());
		default:
			return null; // not processed
		}
	}

	private static Cursor getSearchSuggest(POIProviderContract provider, String query) {
		Cursor cursor = provider.getSearchSuggest(query);
		if (cursor == null) {
			cursor = ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		}
		return cursor;
	}

	@Override
	public Cursor getSearchSuggest(String query) {
		return getDefaultSearchSuggest(query, this);
	}

	public static Cursor getDefaultSearchSuggest(String query, POIProviderContract provider) {
		SQLiteDatabase db = null;
		try {
			String selection = POIProviderContract.Filter.getSearchSelection(new String[] { query }, SUGGEST_SEARCHABLE_COLUMNS, null);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getSearchSuggestTable());
			qb.setProjectionMap(provider.getSearchSuggestProjectionMap());
			db = provider.getDBHelper().getReadableDatabase();
			return qb.query(db, PROJECTION_POI_SEARCH_SUGGEST, selection, null, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while loading search suggests '%s'!", query);
			return null;
		} finally {
			SqlUtils.closeQuietly(db);
		}
	}

	private static Cursor getPOI(POIProviderContract provider, String selection) {
		POIProviderContract.Filter poiFilter = POIProviderContract.Filter.fromJSONString(selection);
		return provider.getPOI(poiFilter);
	}

	@Override
	public Cursor getPOI(POIProviderContract.Filter poiFilter) {
		return getDefaultPOIFromDB(poiFilter, this);
	}

	@Override
	public Cursor getPOIFromDB(POIProviderContract.Filter poiFilter) {
		return getDefaultPOIFromDB(poiFilter, this);
	}

	public static Cursor getDefaultPOIFromDB(POIProviderContract.Filter poiFilter, POIProviderContract provider) {
		SQLiteDatabase db = null;
		try {
			if (poiFilter == null || provider == null) {
				return null;
			}
			String selection = poiFilter.getSqlSelection(POIProviderContract.Columns.T_POI_K_UUID_META, POIProviderContract.Columns.T_POI_K_LAT,
					POIProviderContract.Columns.T_POI_K_LNG, SEARCHABLE_LIKE_COLUMNS, SEARCHABLE_EQUALS_COLUMNS);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getPOITable());
			HashMap<String, String> poiProjectionMap = provider.getPOIProjectionMap();
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				poiProjectionMap.put(POIProviderContract.Columns.T_POI_K_SCORE_META_OPT,
						POIProviderContract.Filter.getSearchSelectionScore(poiFilter.getSearchKeywords(), SEARCHABLE_LIKE_COLUMNS, SEARCHABLE_EQUALS_COLUMNS)
								+ "AS " + POIProviderContract.Columns.T_POI_K_SCORE_META_OPT);
			}
			qb.setProjectionMap(poiProjectionMap);
			String[] poiProjection = provider.getPOIProjection();
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				poiProjection = ArrayUtils.addAll(poiProjection, new String[] { POIProviderContract.Columns.T_POI_K_SCORE_META_OPT });
			}
			String groupBy = null;
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				groupBy = POIProviderContract.Columns.T_POI_K_UUID_META;
			}
			String sortOrder = poiFilter.getExtraString(POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER, null);
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				sortOrder = SqlUtils.getSortOrderDescending(POIProviderContract.Columns.T_POI_K_SCORE_META_OPT);
			}
			db = provider.getDBHelper().getReadableDatabase();
			return qb.query(db, poiProjection, selection, null, groupBy, null, sortOrder, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while loading POIs '%s'!", poiFilter);
			return null;
		} finally {
			SqlUtils.closeQuietly(db);
		}
	}

	@Override
	public String[] getPOIProjection() {
		return PROJECTION_POI;
	}

	private static HashMap<String, String> poiProjectionMap;

	@Override
	public HashMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(getContext()), getTYPE_ID(getContext()));
		}
		return poiProjectionMap;
	}

	public static HashMap<String, String> getNewPoiProjectionMap(String authority, int dataSourceTypeId) {
		return SqlUtils.ProjectionMapBuilder.getNew() //
				.appendValue(SqlUtils.concatenate( //
						SqlUtils.escapeString(POIUtils.UID_SEPARATOR), //
						SqlUtils.escapeString(authority), //
						SqlUtils.getTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ID) //
						), POIProviderContract.Columns.T_POI_K_UUID_META) //
				.appendValue(dataSourceTypeId, POIProviderContract.Columns.T_POI_K_DST_ID_META) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ID, POIProviderContract.Columns.T_POI_K_ID) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_NAME, POIProviderContract.Columns.T_POI_K_NAME) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_LAT, POIProviderContract.Columns.T_POI_K_LAT) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_LNG, POIProviderContract.Columns.T_POI_K_LNG) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_TYPE, POIProviderContract.Columns.T_POI_K_TYPE) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_STATUS_TYPE, POIProviderContract.Columns.T_POI_K_STATUS_TYPE) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ACTIONS_TYPE, POIProviderContract.Columns.T_POI_K_ACTIONS_TYPE) //
				.build();
	}

	@Override
	public String getPOITable() {
		return POIDbHelper.T_POI;
	}

	@Override
	public String getSearchSuggestTable() {
		return getPOITable();
	}

	@Override
	public HashMap<String, String> getSearchSuggestProjectionMap() {
		return POIProvider.POI_SEARCH_SUGGEST_PROJECTION_MAP;
	}

	public static String getSortOrderS(POIProviderContract provider, Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.POI:
		case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
		case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@Override
	public String getTypeMT(Uri uri) {
		return getTypeS(this, uri);
	}

	public static String getTypeS(POIProviderContract provider, Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.POI:
			return StringUtils.EMPTY; // empty string = processed
		case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
		case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
			return SearchManager.SUGGEST_MIME_TYPE;
		default:
			return null; // not processed
		}
	}

	@Override
	public int deleteMT(Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insertMT(Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	protected static synchronized int insertDefaultPOIs(POIProviderContract provider, Collection<DefaultPOI> defaultPOIs) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.beginTransaction(); // start the transaction
			if (defaultPOIs != null) {
				for (DefaultPOI defaultPOI : defaultPOIs) {
					long rowId = db.insert(provider.getPOITable(), POIDbHelper.T_POI_K_ID, defaultPOI.toContentValues());
					if (rowId > 0) {
						affectedRows++;
					}
				}
			}
			db.setTransactionSuccessful(); // mark the transaction as successful
		} catch (Exception e) {
			MTLog.w(TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			SqlUtils.endTransactionAndCloseQuietly(db);
		}
		return affectedRows;
	}

	public static class POIDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = POIDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link POIDbHelper} implementations in same app.
		 */
		public static final String DB_NAME = "poi.db";

		public static final int DB_VERSION = 3;

		public static final String T_POI = "poi";
		public static final String T_POI_K_ID = BaseColumns._ID;
		public static final String T_POI_K_NAME = "name";
		public static final String T_POI_K_LAT = "lat";
		public static final String T_POI_K_LNG = "lng";
		public static final String T_POI_K_TYPE = "type";
		public static final String T_POI_K_STATUS_TYPE = "statustype";
		public static final String T_POI_K_ACTIONS_TYPE = "actionstype";

		public static final String T_POI_SQL_CREATE = getSqlCreateBuilder(T_POI).build();
		public static final String T_POI_SQL_INSERT = getSqlInsertBuilder(T_POI).build();
		public static final String T_POI_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_POI);

		public POIDbHelper(Context context) {
			this(context, DB_NAME, DB_VERSION);
		}

		public POIDbHelper(Context context, String dbName, int dbVersion) {
			super(context, dbName, null, dbVersion);
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_POI_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_POI_SQL_CREATE);
		}

		/**
		 * Override if multiple {@link POIDbHelper} implementations in same app.
		 */
		public String getDbName() {
			return DB_NAME;
		}

		/**
		 * Override if multiple {@link POIDbHelper} in same app.
		 */
		public static int getDbVersion() {
			return DB_VERSION;
		}

		public static String getFkColumnName(String columnName) {
			return "fk" + "_" + columnName;
		}

		public static SqlUtils.SQLCreateBuilder getSqlCreateBuilder(String table) {
			SqlUtils.SQLCreateBuilder b = SqlUtils.SQLCreateBuilder.getNew(table) //
					.appendColumn(T_POI_K_ID, SqlUtils.INT_PK) //
					.appendColumn(T_POI_K_NAME, SqlUtils.TXT) //
					.appendColumn(T_POI_K_LAT, SqlUtils.REAL) //
					.appendColumn(T_POI_K_LNG, SqlUtils.REAL) //
					.appendColumn(T_POI_K_TYPE, SqlUtils.INT) //
					.appendColumn(T_POI_K_STATUS_TYPE, SqlUtils.INT) //
					.appendColumn(T_POI_K_ACTIONS_TYPE, SqlUtils.INT);
			return b;
		}

		public static SqlUtils.SQLInsertBuilder getSqlInsertBuilder(String table) {
			SqlUtils.SQLInsertBuilder b = SqlUtils.SQLInsertBuilder.getNew(table) //
					.appendColumn(T_POI_K_ID) //
					.appendColumn(T_POI_K_NAME)//
					.appendColumn(T_POI_K_LAT) //
					.appendColumn(T_POI_K_LNG) //
					.appendColumn(T_POI_K_TYPE) //
					.appendColumn(T_POI_K_STATUS_TYPE) //
					.appendColumn(T_POI_K_ACTIONS_TYPE);
			return b;
		}
	}
}
