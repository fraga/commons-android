package org.mtransit.android.commons;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;

public final class FileUtils implements MTLog.Loggable {

	private static final String TAG = FileUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static String fromFileRes(Context context, int fileResId) {
		StringBuilder resultSb = new StringBuilder();
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(context.getResources().openRawResource(fileResId), "UTF8");
			br = new BufferedReader(isr, 8192);
			String line;
			while ((line = br.readLine()) != null) {
				resultSb.append(line);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reading resource file ID '%s'!", fileResId);
		} finally {
			closeQuietly(br);
			closeQuietly(isr);
		}
		return resultSb.toString();
	}

	public static void copyToPrivateFile(Context context, String fileName, InputStream inputStream) {
		FileOutputStream outputStream = null;
		BufferedReader br = null;
		try {
			outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			br = new BufferedReader(new InputStreamReader(inputStream, "UTF8"), 8192);
			String line;
			while ((line = br.readLine()) != null) {
				outputStream.write(line.getBytes());
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while copying to file '%s'!", fileName);
		} finally {
			closeQuietly(outputStream);
			closeQuietly(br);
		}
	}

	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			MTLog.d(TAG, e, "Error while closing '%s'!", closeable);
		}
	}

	public static String getString(InputStream inputStream) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8192);
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reading json!");
		} finally {
			closeQuietly(reader);
		}
		return sb.toString();
	}

}
