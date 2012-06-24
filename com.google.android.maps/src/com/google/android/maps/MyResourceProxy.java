package com.google.android.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

class MyResourceProxy implements ResourceProxy {

	private static ResourceProxy resourceProxy;

	public static ResourceProxy getInstance() {
		if (resourceProxy == null) {
			resourceProxy = new MyResourceProxy();
		}
		return resourceProxy;
	}

	public static ResourceProxy getInstance(Context ctx) {
		resourceProxy = new MyResourceProxy(ctx);
		return resourceProxy;
	}

	private Context ctx;

	public MyResourceProxy() {
	}

	public MyResourceProxy(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public Bitmap getBitmap(bitmap pResId) {
		return null;
	}

	@Override
	public float getDisplayMetricsDensity() {
		if (ctx != null) {
			return ctx.getResources().getDisplayMetrics().density;
		}
		return 1;
	}

	@Override
	public Drawable getDrawable(bitmap pResId) {
		return null;
	}

	@Override
	public String getString(string pResId) {
		return null;
	}

	@Override
	public String getString(string pResId, Object... formatArgs) {
		return null;
	}

}
