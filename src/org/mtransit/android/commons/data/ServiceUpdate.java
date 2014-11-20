package org.mtransit.android.commons.data;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.ServiceUpdateProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.TextUtils;

public class ServiceUpdate implements MTLog.Loggable {

	private static final String TAG = ServiceUpdate.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final HigherSeverityFirstComparator HIGHER_SEVERITY_FIRST_COMPARATOR = new HigherSeverityFirstComparator();

	// order and values are important (force DB reset when changing values)
	public static final int SEVERITY_NONE = 0; // no message
	public static final int SEVERITY_INFO_UNKNOWN = 1; // unexpected information message
	public static final int SEVERITY_INFO_AGENCY = 2; // concerns most if not all POIs in this agency
	public static final int SEVERITY_INFO_RELATED_POI = 3; // RTS other stops on the same route [trip]
	public static final int SEVERITY_INFO_POI = 4; // related to this POI but not warning
	public static final int SEVERITY_WARNING_UNKNOWN = 5; // unexpected warning message
	public static final int SEVERITY_WARNING_POI = 6; // related to this POI and it's important enough to bother user with it

	private Integer id; // internal DB ID (useful to delete) OR NULL
	private String targetUUID;
	private long lastUpdateInMs;
	private long maxValidityInMs;
	private String text;
	private String textHTML;
	private int severity;
	private String language;
	private String sourceLabel;
	private String sourceId;

	public ServiceUpdate(Integer optId, String targetUUID, long lastUpdateInMs, long maxValidityInMs, String text, String optTextHTML, int severity,
			String sourceId, String sourceLabel, String language) {
		this.id = optId;
		this.targetUUID = targetUUID;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
		this.text = text;
		this.textHTML = optTextHTML;
		this.severity = severity;
		this.sourceId = sourceId;
		this.sourceLabel = sourceLabel;
		this.language = language;
	}

	public boolean isSeverityWarning() {
		return isSeverityWarning(this.severity);
	}

	public static boolean isSeverityWarning(int severity) {
		return severity == SEVERITY_WARNING_UNKNOWN //
				|| severity == SEVERITY_WARNING_POI; //
	}

	public boolean isSeverityInfo() {
		return isSeverityInfo(severity);
	}

	public static boolean isSeverityInfo(int severity) {
		if (isSeverityWarning(severity)) {
			return false;
		}
		return severity == SEVERITY_INFO_UNKNOWN //
				|| severity == SEVERITY_INFO_AGENCY //
				|| severity == SEVERITY_INFO_RELATED_POI //
				|| severity == SEVERITY_INFO_POI; //
	}

	public static final boolean isSeverityWarning(Collection<ServiceUpdate> serviceUpdates) {
		if (serviceUpdates != null) {
			for (ServiceUpdate serviceUpdate : serviceUpdates) {
				if (serviceUpdate.isSeverityWarning()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasMessage() {
		return this.severity != SEVERITY_NONE;
	}

	public int getSeverity() {
		return severity;
	}

	public String getSourceId() {
		return sourceId;
	}

	public String getSourceLabel() {
		return sourceLabel;
	}

	public String getText() {
		return text;
	}

	public String getTextHTML() {
		if (TextUtils.isEmpty(textHTML)) {
			return getText();
		}
		return textHTML;
	}

	public String getLanguage() {
		return language;
	}

	public boolean isLanguage() {
		return TextUtils.isEmpty(this.language) // no language = all language
				|| Locale.getDefault().getLanguage().equals(this.language);
	}

	@Override
	public String toString() {
		return new StringBuilder(ServiceUpdate.class.getSimpleName()).append('[') //
				.append("id:").append(this.id) //
				.append(',') //
				.append("targetUUID:").append(this.targetUUID) //
				.append(',') //
				.append("text:").append(this.text) //
				.append(']').toString();
	}

	public boolean isUseful() {
		return this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis();
	}

	public Integer getId() {
		return this.id;
	}

	public long getLastUpdateInMs() {
		return lastUpdateInMs;
	}

	public static ServiceUpdate fromCursor(Cursor cursor) {
		final int idIdx = cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_ID);
		final Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		final String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID));
		final long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_LAST_UPDATE));
		final long maxValidityInMs = cursor.getLong(cursor
				.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS));
		final int severity = cursor.getInt(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_SEVERITY));
		final String text = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT));
		final String htmlText = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT_HTML));
		final String language = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_LANGUAGE));
		final String sourceLabel = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_LABEL));
		final String sourceId = cursor.getString(cursor.getColumnIndexOrThrow(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID));
		return new ServiceUpdate(id, targetUUID, lastUpdateInMs, maxValidityInMs, text, htmlText, severity, sourceId, sourceLabel, language);
	}

	public Cursor toCursor() {
		MatrixCursor cursor = new MatrixCursor(ServiceUpdateProvider.PROJECTION_SERVICE_UPDATE);
		cursor.addRow(getCursorRow());
		return cursor;
	}

	public Object[] getCursorRow() {
		return new Object[] { //
		id, //
				targetUUID, //
				lastUpdateInMs,//
				maxValidityInMs, //
				severity,//
				text, //
				textHTML, //
				language,//
				sourceLabel,//
				sourceId //
		};
	}

	public ContentValues toContentValues() {
		final ContentValues contentValues = new ContentValues();
		if (this.id != null) {
			contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_ID, this.id);
		} // ELSE AUTO INCREMENT
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID, this.targetUUID);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, this.maxValidityInMs);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_SEVERITY, this.severity);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT, this.text);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT_HTML, this.textHTML);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_LANGUAGE, this.language);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_LABEL, this.sourceLabel);
		contentValues.put(ServiceUpdateProvider.ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID, this.sourceId);
		return contentValues;
	}

	private static class HigherSeverityFirstComparator implements Comparator<ServiceUpdate> {

		@Override
		public int compare(ServiceUpdate lhs, ServiceUpdate rhs) {
			if (lhs == null && rhs == null) {
				return ComparatorUtils.SAME;
			} else if (lhs == null) {
				return ComparatorUtils.AFTER;
			} else if (rhs == null) {
				return ComparatorUtils.BEFORE;
			}
			return rhs.severity - lhs.severity; // higher severity before
		}

	}
}