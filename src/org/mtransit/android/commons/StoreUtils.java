package org.mtransit.android.commons;

import android.app.Activity;
import android.net.Uri;

public final class StoreUtils implements MTLog.Loggable {

	private static final String TAG = StoreUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String HTTP_SCHEME = "http";
	private static final String HTTPS_SCHEME = "https";
	private static final String MARKET_SCHEME = "market";
	private static final String ANDROID_MARKET_WWW_AUTHORITY = "market.android.com"; // old
	private static final String GOOGLE_PLAY_STORE_WWW_AUTHORITY = "play.google.com";
	private static final String GOOGLE_PLAY_STORE_BASE_URI_AND_PKG = MARKET_SCHEME + "://details?id=%s";
	private static final String GOOGLE_PLAY_STORE_BASE_WWW_URI_AND_PKG = HTTPS_SCHEME + "://play.google.com/store/apps/details?id=%s";

	public static void viewAppPage(Activity activity, String pkg, String label) {
		boolean success = LinkUtils.open(activity, Uri.parse(String.format(GOOGLE_PLAY_STORE_BASE_URI_AND_PKG, pkg)), label);
		if (!success) {
			LinkUtils.open(activity, Uri.parse(String.format(GOOGLE_PLAY_STORE_BASE_WWW_URI_AND_PKG, pkg)), label);
		}
	}

	public static boolean isStoreIntent(String url) {
		return isStoreIntent(Uri.parse(url));
	}

	public static boolean isStoreIntent(Uri uri) {
		if (uri != null) {
			if (MARKET_SCHEME.equals(uri.getScheme())) {
				return true;
			} else if (HTTPS_SCHEME.equals(uri.getScheme()) || HTTP_SCHEME.equals(uri.getScheme())) {
				if (GOOGLE_PLAY_STORE_WWW_AUTHORITY.equals(uri.getAuthority()) || ANDROID_MARKET_WWW_AUTHORITY.equals(uri.getAuthority())) {
					return true;
				}
			}
		}
		return false;
	}
}
