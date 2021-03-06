package org.mtransit.android.commons.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProvider.StopColumns;

import android.database.Cursor;

public class Stop {

	private static final String TAG = Stop.class.getSimpleName();

	private int id;

	private String code;
	private String name;

	private double lat;
	private double lng;

	public Stop() {
	}

	public Stop(Stop stop) {
		setId(stop.getId());
		setCode(stop.getCode());
		setName(stop.getName());
		setLat(stop.getLat());
		setLng(stop.getLng());
	}

	public static Stop fromCursor(Cursor c) {
		Stop stop = new Stop();
		stop.setId(c.getInt(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_ID)));
		stop.setCode(c.getString(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_CODE)));
		stop.setName(c.getString(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_NAME)));
		stop.setLat(c.getDouble(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_LAT)));
		stop.setLng(c.getDouble(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_LNG)));
		return stop;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Stop.class.getSimpleName()).append(":[") //
				.append("id:").append(getId()).append(',') //
				.append("code:").append(getCode()).append(',') //
				.append("name:").append(getName()).append(',') //
				.append("lat:").append(getLat()).append(',') //
				.append("lng:").append(getLng()) //
				.append(']').toString();
	}

	private static final String JSON_ID = "id";
	private static final String JSON_CODE = "code";
	private static final String JSON_NAME = "name";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LNG = "lng";

	public static JSONObject toJSON(Stop stop) {
		try {
			return new JSONObject() //
					.put(JSON_ID, stop.getId()) //
					.put(JSON_CODE, stop.getCode()) //
					.put(JSON_NAME, stop.getName()) //
					.put(JSON_LAT, stop.getLat()) //
					.put(JSON_LNG, stop.getLng());
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while converting to JSON (%s)!", stop);
			return null;
		}
	}

	public static Stop fromJSON(JSONObject jStop) {
		try {
			Stop stop = new Stop();
			stop.setId(jStop.getInt(JSON_ID));
			stop.setCode(jStop.getString(JSON_CODE));
			stop.setName(jStop.getString(JSON_NAME));
			stop.setLat(jStop.getDouble(JSON_LAT));
			stop.setLng(jStop.getDouble(JSON_LNG));
			return stop;
		} catch (JSONException jsone) {
			MTLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jStop);
			return null;
		}
	}

	public int getId() {
		return id;
	}

	protected void setId(int id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	protected void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public Double getLat() {
		return this.lat;
	}

	protected void setLat(double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return this.lng;
	}

	protected void setLng(double lng) {
		this.lng = lng;
	}
}
