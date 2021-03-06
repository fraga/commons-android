package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class BikeStationDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = BikeStationDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link BikeStationDbHelper} implementations in same app.
	 */
	protected static final String DB_NAME = "bikestation.db";

	/**
	 * Override if multiple {@link BikeStationDbHelper} implementations in same app.
	 */
	protected static final String PREF_KEY_LAST_UPDATE_MS = "pBikeStationLastUpdate";

	public static final String T_BIKE_STATION = POIDbHelper.T_POI;
	private static final String T_BIKE_STATION_SQL_CREATE = POIDbHelper.getSqlCreate(T_BIKE_STATION);
	private static final String T_BIKE_STATION_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_BIKE_STATION);

	public static final String T_BIKE_STATION_STATUS = StatusDbHelper.T_STATUS;
	private static final String T_BIKE_STATION_STATUS_SQL_CREATE = StatusDbHelper.getSqlCreate(T_BIKE_STATION_STATUS);
	private static final String T_BIKE_STATION_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_BIKE_STATION_STATUS);

	private static int dbVersion = -1;

	/**
	 * Override if multiple {@link BikeStationDbHelper} in same app.
	 */
	public static int getDbVersion(Context context) {
		if (dbVersion < 0) {
			dbVersion = context.getResources().getInteger(R.integer.bike_station_db_version);
		}
		return dbVersion;
	}

	private Context context;

	public BikeStationDbHelper(Context context) {
		super(context, DB_NAME, null, getDbVersion(context));
		this.context = context;
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_BIKE_STATION_SQL_DROP);
		db.execSQL(T_BIKE_STATION_STATUS_SQL_DROP);
		PreferenceUtils.savePrefLcl(this.context, PREF_KEY_LAST_UPDATE_MS, 0l, true);
		initAllDbTables(db);
	}

	public boolean isDbExist(Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	private void initAllDbTables(SQLiteDatabase db) {
		MTLog.v(this, "initAllDbTables()");
		db.execSQL(T_BIKE_STATION_SQL_CREATE);
		db.execSQL(T_BIKE_STATION_STATUS_SQL_CREATE);
	}

}
