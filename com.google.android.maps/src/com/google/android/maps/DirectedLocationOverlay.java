// Created by plusminus on 22:01:11 - 29.09.2008
package com.google.android.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;

/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public class DirectedLocationOverlay extends Overlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final Paint mPaint = new Paint();
	protected final Paint mAccuracyPaint = new Paint();

	protected final Bitmap DIRECTION_ARROW;

	protected GeoPoint mLocation;
	protected float mBearing;

	private final Matrix directionRotater = new Matrix();
	private final Point screenCoords = new Point();

	private final float DIRECTION_ARROW_CENTER_X;
	private final float DIRECTION_ARROW_CENTER_Y;
	private final int DIRECTION_ARROW_WIDTH;
	private final int DIRECTION_ARROW_HEIGHT;

	private int mAccuracy = 0;
	private boolean mShowAccuracy = true;

	// ===========================================================
	// Constructors
	// ===========================================================

	public DirectedLocationOverlay(final Context ctx) {
		this(ctx, new DefaultResourceProxyImpl(ctx));
	}

	public DirectedLocationOverlay(final Context ctx,
			final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		DIRECTION_ARROW = mResourceProxy
				.getBitmap(ResourceProxy.bitmap.direction_arrow);

		DIRECTION_ARROW_CENTER_X = DIRECTION_ARROW.getWidth() / 2 - 0.5f;
		DIRECTION_ARROW_CENTER_Y = DIRECTION_ARROW.getHeight() / 2 - 0.5f;
		DIRECTION_ARROW_HEIGHT = DIRECTION_ARROW.getHeight();
		DIRECTION_ARROW_WIDTH = DIRECTION_ARROW.getWidth();

		mAccuracyPaint.setStrokeWidth(2);
		mAccuracyPaint.setColor(Color.BLUE);
		mAccuracyPaint.setAntiAlias(true);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public void draw(final Canvas c, final MapView osmv, final boolean shadow) {

		if (shadow) {
			return;
		}

		if (mLocation != null) {
			final Projection pj = osmv.getProjection();
			pj.toMapPixels(mLocation, screenCoords);

			if (mShowAccuracy && mAccuracy > 10) {
				final float accuracyRadius = pj
						.metersToEquatorPixels(mAccuracy);
				/* Only draw if the DirectionArrow doesn't cover it. */
				if (accuracyRadius > 8) {
					/* Draw the inner shadow. */
					mAccuracyPaint.setAntiAlias(false);
					mAccuracyPaint.setAlpha(30);
					mAccuracyPaint.setStyle(Style.FILL);
					c.drawCircle(screenCoords.x, screenCoords.y,
							accuracyRadius, mAccuracyPaint);

					/* Draw the edge. */
					mAccuracyPaint.setAntiAlias(true);
					mAccuracyPaint.setAlpha(150);
					mAccuracyPaint.setStyle(Style.STROKE);
					c.drawCircle(screenCoords.x, screenCoords.y,
							accuracyRadius, mAccuracyPaint);
				}
			}

			/*
			 * Rotate the direction-Arrow according to the bearing we are
			 * driving. And draw it to the canvas.
			 */
			directionRotater.setRotate(mBearing, DIRECTION_ARROW_CENTER_X,
					DIRECTION_ARROW_CENTER_Y);
			final Bitmap rotatedDirection = Bitmap.createBitmap(
					DIRECTION_ARROW, 0, 0, DIRECTION_ARROW_WIDTH,
					DIRECTION_ARROW_HEIGHT, directionRotater, false);
			c.drawBitmap(rotatedDirection,
					screenCoords.x - rotatedDirection.getWidth() / 2,
					screenCoords.y - rotatedDirection.getHeight() / 2, mPaint);
		}
	}

	public GeoPoint getLocation() {
		return mLocation;
	}

	/**
	 * 
	 * @param pAccuracy
	 *            in Meters
	 */
	public void setAccuracy(final int pAccuracy) {
		mAccuracy = pAccuracy;
	}

	public void setBearing(final float aHeading) {
		mBearing = aHeading;
	}

	public void setLocation(final GeoPoint mp) {
		mLocation = mp;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	public void setShowAccuracy(final boolean pShowIt) {
		mShowAccuracy = pShowIt;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
