package org.mtransit.android.commons.api;

import java.util.Locale;

import org.mtransit.android.commons.MTLog;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class IceCreamSandwichSupport implements SupportUtil {

	private static final String TAG = IceCreamSandwichSupport.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public IceCreamSandwichSupport() {
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeGlobalOnLayoutListener(onGlobalLayoutListener);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setBackground(View view, Drawable background) {
		view.setBackgroundDrawable(background);
	}

	@Override
	public Locale localeForLanguageTag(String languageTag) {
		try {
			if (!TextUtils.isEmpty(languageTag)) {
				String[] split = languageTag.split("-");
				if (split.length == 1) {
					return new Locale(split[0]);
				} else if (split.length == 2) {
					return new Locale(split[0], split[1]);
				} else if (split.length == 3) {
					return new Locale(split[0], split[1], split[2]);
				}
			}
			MTLog.w(this, "Unexpected language tag '%s'!", languageTag);
			return Locale.ENGLISH; // default
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing locale language tag '%s'!", languageTag);
			return Locale.ENGLISH; // default
		}
	}
}
