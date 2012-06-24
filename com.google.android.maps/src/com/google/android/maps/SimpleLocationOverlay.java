// Created by plusminus on 22:01:11 - 29.09.2008
package com.google.android.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public class SimpleLocationOverlay extends Overlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();

	protected final Bitmap PERSON_ICON;
	/** Coordinates the feet of the person are located. */
	protected final android.graphics.Point PERSON_HOTSPOT = new android.graphics.Point(
			24, 39);

	protected GeoPoint mLocation;
	private final Point screenCoords = new Point();

	// ===========================================================
	// Constructors
	// ===========================================================

	public SimpleLocationOverlay(final Context ctx) {
		this(ctx, new DefaultResourceProxyImpl(ctx));
	}

	public SimpleLocationOverlay(final Context ctx,
			final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		PERSON_ICON = mResourceProxy.getBitmap(ResourceProxy.bitmap.person);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public void draw(final Canvas c, final MapView osmv, final boolean shadow) {
		if (!shadow && mLocation != null) {
			final Projection pj = osmv.getProjection();
			pj.toMapPixels(mLocation, screenCoords);

			c.drawBitmap(PERSON_ICON, screenCoords.x - PERSON_HOTSPOT.x,
					screenCoords.y - PERSON_HOTSPOT.y, mPaint);
		}
	}

	public GeoPoint getMyLocation() {
		return mLocation;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	public void setLocation(final GeoPoint mp) {
		mLocation = mp;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
