package com.google.android.maps;

import android.graphics.Point;
import android.graphics.Rect;

public interface Projection extends IProjection {
	public abstract Point fromMapPixels(final int x, final int y,
			final Point reuse);

	public abstract GeoPoint fromPixels(float i, float j);

	@Override
	public abstract GeoPoint fromPixels(int i, int j);

	public abstract Rect fromPixelsToProjected(final Rect in);

	public Rect getScreenRect();

	public int getTileSizePixels();

	public int getZoomLevel();

	@Override
	public abstract float metersToEquatorPixels(float f);

	public abstract Point toMapPixels(final IGeoPoint in, final Point reuse);

	public abstract Point toMapPixelsProjected(final int latituteE6,
			final int longitudeE6, final Point reuse);

	public abstract Point toMapPixelsTranslated(final Point in,
			final Point reuse);

	public abstract Point toPixels(GeoPoint geopoint, Point point);

	public abstract Point toPixels(GeoPoint geopoint, Point point, boolean flag);

}
