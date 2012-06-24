// Created by plusminus on 21:37:08 - 27.09.2008
package com.google.android.maps;

import microsoft.mappoint.TileSystem;
import android.graphics.Point;
import android.util.Log;

/**
 * 
 * @author Nicolas Gramlich
 */
public class MapController implements IMapController, MapViewConstants {

	// ===========================================================
	// OpenMaps added
	// ===========================================================

	private abstract class AbstractAnimationRunner extends Thread {

		// ===========================================================
		// Fields
		// ===========================================================

		protected final int mSmoothness;
		protected final int mTargetLatitudeE6, mTargetLongitudeE6;
		protected boolean mDone = false;

		protected final int mStepDuration;

		protected final int mPanTotalLatitudeE6, mPanTotalLongitudeE6;

		// ===========================================================
		// Constructors
		// ===========================================================

		public AbstractAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6, final int aSmoothness,
				final int aDuration) {
			mTargetLatitudeE6 = aTargetLatitudeE6;
			mTargetLongitudeE6 = aTargetLongitudeE6;
			mSmoothness = aSmoothness;
			mStepDuration = aDuration / aSmoothness;

			/* Get the current mapview-center. */
			final MapView mapview = mOsmv;
			final IGeoPoint mapCenter = mapview.getMapCenter();

			mPanTotalLatitudeE6 = mapCenter.getLatitudeE6() - aTargetLatitudeE6;
			mPanTotalLongitudeE6 = mapCenter.getLongitudeE6()
					- aTargetLongitudeE6;
		}

		@SuppressWarnings("unused")
		public AbstractAnimationRunner(final MapController mapViewController,
				final int aTargetLatitudeE6, final int aTargetLongitudeE6) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					MapViewConstants.ANIMATION_SMOOTHNESS_DEFAULT,
					MapViewConstants.ANIMATION_DURATION_DEFAULT);
		}

		public boolean isDone() {
			return mDone;
		}

		public abstract void onRunAnimation();

		@Override
		public void run() {
			onRunAnimation();
			mDone = true;
		}
	}

	/**
	 * Choose on of the Styles of approacing the target Coordinates.
	 * <ul>
	 * <li><code>LINEAR</code>
	 * <ul>
	 * <li>Uses ses linear interpolation</li>
	 * <li>Values produced: 10%, 20%, 30%, 40%, 50%, ...</li>
	 * <li>Style: Always average speed.</li>
	 * </ul>
	 * </li>
	 * <li><code>EXPONENTIALDECELERATING</code>
	 * <ul>
	 * <li>Uses a exponential interpolation/li>
	 * <li>Values produced: 50%, 75%, 87.5%, 93.5%, ...</li>
	 * <li>Style: Starts very fast, really slow in the end.</li>
	 * </ul>
	 * </li>
	 * <li><code>QUARTERCOSINUSALDECELERATING</code>
	 * <ul>
	 * <li>Uses the first quarter of the cos curve (from zero to PI/2) for
	 * interpolation.</li>
	 * <li>Values produced: See cos curve :)</li>
	 * <li>Style: Average speed, slows out medium.</li>
	 * </ul>
	 * </li>
	 * <li><code>HALFCOSINUSALDECELERATING</code>
	 * <ul>
	 * <li>Uses the first half of the cos curve (from zero to PI) for
	 * interpolation</li>
	 * <li>Values produced: See cos curve :)</li>
	 * <li>Style: Average speed, slows out smoothly.</li>
	 * </ul>
	 * </li>
	 * <li><code>MIDDLEPEAKSPEED</code>
	 * <ul>
	 * <li>Uses the values of cos around the 0 (from -PI/2 to +PI/2) for
	 * interpolation</li>
	 * <li>Values produced: See cos curve :)</li>
	 * <li>Style: Starts medium, speeds high in middle, slows out medium.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 */
	public static enum AnimationType {
		/**
		 * <ul>
		 * <li><code>LINEAR</code>
		 * <ul>
		 * <li>Uses ses linear interpolation</li>
		 * <li>Values produced: 10%, 20%, 30%, 40%, 50%, ...</li>
		 * <li>Style: Always average speed.</li>
		 * </ul>
		 * </li>
		 * </ul>
		 */
		LINEAR,
		/**
		 * <ul>
		 * <li><code>EXPONENTIALDECELERATING</code>
		 * <ul>
		 * <li>Uses a exponential interpolation/li>
		 * <li>Values produced: 50%, 75%, 87.5%, 93.5%, ...</li>
		 * <li>Style: Starts very fast, really slow in the end.</li>
		 * </ul>
		 * </li>
		 * </ul>
		 */
		EXPONENTIALDECELERATING,
		/**
		 * <ul>
		 * <li><code>QUARTERCOSINUSALDECELERATING</code>
		 * <ul>
		 * <li>Uses the first quarter of the cos curve (from zero to PI/2) for
		 * interpolation.</li>
		 * <li>Values produced: See cos curve :)</li>
		 * <li>Style: Average speed, slows out medium.</li>
		 * </ul>
		 * </li>
		 * </ul>
		 */
		QUARTERCOSINUSALDECELERATING,
		/**
		 * <ul>
		 * <li><code>HALFCOSINUSALDECELERATING</code>
		 * <ul>
		 * <li>Uses the first half of the cos curve (from zero to PI) for
		 * interpolation</li>
		 * <li>Values produced: See cos curve :)</li>
		 * <li>Style: Average speed, slows out smoothly.</li>
		 * </ul>
		 * </li>
		 * </ul>
		 */
		HALFCOSINUSALDECELERATING,
		/**
		 * <ul>
		 * <li><code>MIDDLEPEAKSPEED</code>
		 * <ul>
		 * <li>Uses the values of cos around the 0 (from -PI/2 to +PI/2) for
		 * interpolation</li>
		 * <li>Values produced: See cos curve :)</li>
		 * <li>Style: Starts medium, speeds high in middle, slows out medium.</li>
		 * </ul>
		 * </li>
		 * </ul>
		 */
		MIDDLEPEAKSPEED;
	}

	private class CosinusalBasedAnimationRunner extends AbstractAnimationRunner
			implements MathConstants {
		// ===========================================================
		// Fields
		// ===========================================================

		protected final float mStepIncrement, mAmountStretch;
		protected final float mYOffset, mStart;

		// ===========================================================
		// Constructors
		// ===========================================================

		@SuppressWarnings("unused")
		public CosinusalBasedAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6, final float aStart,
				final float aRange, final float aYOffset) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT,
					aStart, aRange, aYOffset);
		}

		public CosinusalBasedAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6, final int aSmoothness,
				final int aDuration, final float aStart, final float aRange,
				final float aYOffset) {
			super(aTargetLatitudeE6, aTargetLongitudeE6, aSmoothness, aDuration);
			mYOffset = aYOffset;
			mStart = aStart;

			mStepIncrement = aRange / aSmoothness;

			/*
			 * We need to normalize the amount in the end, so wee need the the:
			 * sum^(-1) .
			 */
			float amountSum = 0;
			for (int i = 0; i < aSmoothness; i++) {
				amountSum += aYOffset + Math.cos(mStepIncrement * i + aStart);
			}

			mAmountStretch = 1 / amountSum;

			setName("QuarterCosinusalDeceleratingAnimationRunner");
		}

		// ===========================================================
		// Methods from SuperClass/Interfaces
		// ===========================================================

		@Override
		public void onRunAnimation() {
			final MapView mapview = mOsmv;
			final IGeoPoint mapCenter = mapview.getMapCenter();
			final int stepDuration = mStepDuration;
			final float amountStretch = mAmountStretch;
			try {
				int newMapCenterLatE6;
				int newMapCenterLonE6;

				for (int i = 0; i < mSmoothness; i++) {

					final double delta = (mYOffset + Math.cos(mStepIncrement
							* i + mStart))
							* amountStretch;
					final int deltaLatitudeE6 = (int) (mPanTotalLatitudeE6 * delta);
					final int deltaLongitudeE6 = (int) (mPanTotalLongitudeE6 * delta);

					newMapCenterLatE6 = mapCenter.getLatitudeE6()
							- deltaLatitudeE6;
					newMapCenterLonE6 = mapCenter.getLongitudeE6()
							- deltaLongitudeE6;
					mapview.setMapCenter(newMapCenterLatE6, newMapCenterLonE6);

					Thread.sleep(stepDuration);
				}
				mapview.setMapCenter(super.mTargetLatitudeE6,
						super.mTargetLongitudeE6);
			} catch (final Exception e) {
				interrupt();
			}
		}
	}

	private class ExponentialDeceleratingAnimationRunner extends
			AbstractAnimationRunner {

		// ===========================================================
		// Fields
		// ===========================================================

		// ===========================================================
		// Constructors
		// ===========================================================

		@SuppressWarnings("unused")
		public ExponentialDeceleratingAnimationRunner(
				final int aTargetLatitudeE6, final int aTargetLongitudeE6) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT);
		}

		public ExponentialDeceleratingAnimationRunner(
				final int aTargetLatitudeE6, final int aTargetLongitudeE6,
				final int aSmoothness, final int aDuration) {
			super(aTargetLatitudeE6, aTargetLongitudeE6, aSmoothness, aDuration);

			setName("ExponentialDeceleratingAnimationRunner");
		}

		// ===========================================================
		// Methods from SuperClass/Interfaces
		// ===========================================================

		@Override
		public void onRunAnimation() {
			final MapView mapview = mOsmv;
			final IGeoPoint mapCenter = mapview.getMapCenter();
			final int stepDuration = mStepDuration;
			try {
				int newMapCenterLatE6;
				int newMapCenterLonE6;

				for (int i = 0; i < mSmoothness; i++) {

					final double delta = Math.pow(0.5, i + 1);
					final int deltaLatitudeE6 = (int) (mPanTotalLatitudeE6 * delta);
					final int detlaLongitudeE6 = (int) (mPanTotalLongitudeE6 * delta);

					newMapCenterLatE6 = mapCenter.getLatitudeE6()
							- deltaLatitudeE6;
					newMapCenterLonE6 = mapCenter.getLongitudeE6()
							- detlaLongitudeE6;
					mapview.setMapCenter(newMapCenterLatE6, newMapCenterLonE6);

					Thread.sleep(stepDuration);
				}
				mapview.setMapCenter(super.mTargetLatitudeE6,
						super.mTargetLongitudeE6);
			} catch (final Exception e) {
				interrupt();
			}
		}
	}

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected class HalfCosinusalDeceleratingAnimationRunner extends
			CosinusalBasedAnimationRunner implements MathConstants {
		// ===========================================================
		// Constructors
		// ===========================================================

		protected HalfCosinusalDeceleratingAnimationRunner(
				final int aTargetLatitudeE6, final int aTargetLongitudeE6) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT);
		}

		protected HalfCosinusalDeceleratingAnimationRunner(
				final int aTargetLatitudeE6, final int aTargetLongitudeE6,
				final int aSmoothness, final int aDuration) {
			super(aTargetLatitudeE6, aTargetLongitudeE6, aSmoothness,
					aDuration, 0, PI, 1);
		}
	}

	private class LinearAnimationRunner extends AbstractAnimationRunner {

		// ===========================================================
		// Fields
		// ===========================================================

		protected final int mPanPerStepLatitudeE6, mPanPerStepLongitudeE6;

		// ===========================================================
		// Constructors
		// ===========================================================

		@SuppressWarnings("unused")
		public LinearAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT);
		}

		public LinearAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6, final int aSmoothness,
				final int aDuration) {
			super(aTargetLatitudeE6, aTargetLongitudeE6, aSmoothness, aDuration);

			/* Get the current mapview-center. */
			final MapView mapview = mOsmv;
			final IGeoPoint mapCenter = mapview.getMapCenter();

			mPanPerStepLatitudeE6 = (mapCenter.getLatitudeE6() - aTargetLatitudeE6)
					/ aSmoothness;
			mPanPerStepLongitudeE6 = (mapCenter.getLongitudeE6() - aTargetLongitudeE6)
					/ aSmoothness;

			setName("LinearAnimationRunner");
		}

		// ===========================================================
		// Methods from SuperClass/Interfaces
		// ===========================================================

		@Override
		public void onRunAnimation() {
			final MapView mapview = mOsmv;
			final IGeoPoint mapCenter = mapview.getMapCenter();
			final int panPerStepLatitudeE6 = mPanPerStepLatitudeE6;
			final int panPerStepLongitudeE6 = mPanPerStepLongitudeE6;
			final int stepDuration = mStepDuration;
			try {
				int newMapCenterLatE6;
				int newMapCenterLonE6;

				for (int i = mSmoothness; i > 0; i--) {

					newMapCenterLatE6 = mapCenter.getLatitudeE6()
							- panPerStepLatitudeE6;
					newMapCenterLonE6 = mapCenter.getLongitudeE6()
							- panPerStepLongitudeE6;
					mapview.setMapCenter(newMapCenterLatE6, newMapCenterLonE6);

					Thread.sleep(stepDuration);
				}
			} catch (final Exception e) {
				interrupt();
			}
		}
	}

	// ===========================================================
	// Constructors
	// ===========================================================

	protected class MiddlePeakSpeedAnimationRunner extends
			CosinusalBasedAnimationRunner implements MathConstants {
		// ===========================================================
		// Constructors
		// ===========================================================

		protected MiddlePeakSpeedAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT);
		}

		protected MiddlePeakSpeedAnimationRunner(final int aTargetLatitudeE6,
				final int aTargetLongitudeE6, final int aSmoothness,
				final int aDuration) {
			super(aTargetLatitudeE6, aTargetLongitudeE6, aSmoothness,
					aDuration, -PI_2, PI, 0);
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	protected class QuarterCosinusalDeceleratingAnimationRunner extends
			CosinusalBasedAnimationRunner implements MathConstants {
		// ===========================================================
		// Constructors
		// ===========================================================

		protected QuarterCosinusalDeceleratingAnimationRunner(
				final int aTargetLatitudeE6, final int aTargetLongitudeE6) {
			this(aTargetLatitudeE6, aTargetLongitudeE6,
					ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT);
		}

		protected QuarterCosinusalDeceleratingAnimationRunner(
				final int aTargetLatitudeE6, final int aTargetLongitudeE6,
				final int aSmoothness, final int aDuration) {
			super(aTargetLatitudeE6, aTargetLongitudeE6, aSmoothness,
					aDuration, 0, PI_2, 0);
		}
	}

	private final MapView mOsmv;

	private AbstractAnimationRunner mCurrentAnimationRunner;

	public MapController(final MapView osmv) {
		mOsmv = osmv;
	}

	/**
	 * Start animating the map towards the given point.
	 */
	public void animateTo(final double latitude, final double longitude) {
		final int x = mOsmv.getScrollX();
		final int y = mOsmv.getScrollY();
		final Point p = TileSystem.LatLongToPixelXY(latitude, longitude,
				mOsmv.getZoomLevel(), null);
		final int worldSize_2 = TileSystem.MapSize(mOsmv.getZoomLevel()) / 2;
		mOsmv.getScroller().startScroll(x, y, p.x - worldSize_2 - x,
				p.y - worldSize_2 - y, ANIMATION_DURATION_DEFAULT);
		mOsmv.postInvalidate();
	}

	public void animateTo(GeoPoint point) {
		animateTo(point.getLatitudeE6() / 1E6, point.getLongitudeE6() / 1E6);
	}

	public void animateTo(GeoPoint point, android.os.Message message) {
		animateTo(point);
	}

	/**
	 * Animates the underlying {@link MapView} that it centers the passed
	 * {@link GeoPoint} in the end. Uses:
	 * {@link MapController.ANIMATION_SMOOTHNESS_DEFAULT} and
	 * {@link MapController.ANIMATION_DURATION_DEFAULT}.
	 * 
	 * @param gp
	 */
	public void animateTo(final GeoPoint gp, final AnimationType aAnimationType) {
		animateTo(gp.getLatitudeE6(), gp.getLongitudeE6(), aAnimationType,
				ANIMATION_DURATION_DEFAULT, ANIMATION_SMOOTHNESS_DEFAULT);
	}

	/**
	 * Animates the underlying {@link MapView} that it centers the passed
	 * {@link GeoPoint} in the end.
	 * 
	 * @param gp
	 *            GeoPoint to be centered in the end.
	 * @param aSmoothness
	 *            steps made during animation. I.e.:
	 *            {@link MapController.ANIMATION_SMOOTHNESS_LOW},
	 *            {@link MapController.ANIMATION_SMOOTHNESS_DEFAULT},
	 *            {@link MapController.ANIMATION_SMOOTHNESS_HIGH}
	 * @param aDuration
	 *            in Milliseconds. I.e.:
	 *            {@link MapController.ANIMATION_DURATION_SHORT},
	 *            {@link MapController.ANIMATION_DURATION_DEFAULT},
	 *            {@link MapController.ANIMATION_DURATION_LONG}
	 */
	public void animateTo(final GeoPoint gp,
			final AnimationType aAnimationType, final int aSmoothness,
			final int aDuration) {
		animateTo(gp.getLatitudeE6(), gp.getLongitudeE6(), aAnimationType,
				aSmoothness, aDuration);
	}

	public void animateTo(GeoPoint point, java.lang.Runnable runnable) {
		animateTo(point);
	}

	/**
	 * Start animating the map towards the given point.
	 */
	@Override
	public void animateTo(final IGeoPoint point) {
		animateTo(point.getLatitudeE6() / 1E6, point.getLongitudeE6() / 1E6);
	}

	/**
	 * Animates the underlying {@link MapView} that it centers the passed
	 * coordinates in the end. Uses:
	 * {@link MapController.ANIMATION_SMOOTHNESS_DEFAULT} and
	 * {@link MapController.ANIMATION_DURATION_DEFAULT}.
	 * 
	 * @param aLatitudeE6
	 * @param aLongitudeE6
	 */
	public void animateTo(final int aLatitudeE6, final int aLongitudeE6,
			final AnimationType aAnimationType) {
		animateTo(aLatitudeE6, aLongitudeE6, aAnimationType,
				ANIMATION_SMOOTHNESS_DEFAULT, ANIMATION_DURATION_DEFAULT);
	}

	/**
	 * Animates the underlying {@link MapView} that it centers the passed
	 * coordinates in the end.
	 * 
	 * @param aLatitudeE6
	 * @param aLongitudeE6
	 * @param aSmoothness
	 *            steps made during animation. I.e.:
	 *            {@link MapController.ANIMATION_SMOOTHNESS_LOW},
	 *            {@link MapController.ANIMATION_SMOOTHNESS_DEFAULT},
	 *            {@link MapController.ANIMATION_SMOOTHNESS_HIGH}
	 * @param aDuration
	 *            in Milliseconds. I.e.:
	 *            {@link MapController.ANIMATION_DURATION_SHORT},
	 *            {@link MapController.ANIMATION_DURATION_DEFAULT},
	 *            {@link MapController.ANIMATION_DURATION_LONG}
	 */
	public void animateTo(final int aLatitudeE6, final int aLongitudeE6,
			final AnimationType aAnimationType, final int aSmoothness,
			final int aDuration) {
		stopAnimation(false);

		switch (aAnimationType) {
		case LINEAR:
			mCurrentAnimationRunner = new LinearAnimationRunner(aLatitudeE6,
					aLongitudeE6, aSmoothness, aDuration);
			break;
		case EXPONENTIALDECELERATING:
			mCurrentAnimationRunner = new ExponentialDeceleratingAnimationRunner(
					aLatitudeE6, aLongitudeE6, aSmoothness, aDuration);
			break;
		case QUARTERCOSINUSALDECELERATING:
			mCurrentAnimationRunner = new QuarterCosinusalDeceleratingAnimationRunner(
					aLatitudeE6, aLongitudeE6, aSmoothness, aDuration);
			break;
		case HALFCOSINUSALDECELERATING:
			mCurrentAnimationRunner = new HalfCosinusalDeceleratingAnimationRunner(
					aLatitudeE6, aLongitudeE6, aSmoothness, aDuration);
			break;
		case MIDDLEPEAKSPEED:
			mCurrentAnimationRunner = new MiddlePeakSpeedAnimationRunner(
					aLatitudeE6, aLongitudeE6, aSmoothness, aDuration);
			break;
		}

		mCurrentAnimationRunner.start();
	}

	public void scrollBy(final int x, final int y) {
		mOsmv.scrollBy(x, y);
	}

	public void setCenter(GeoPoint point) {
		final Point p = TileSystem.LatLongToPixelXY(
				point.getLatitudeE6() / 1E6, point.getLongitudeE6() / 1E6,
				mOsmv.getZoomLevel(), null);
		final int worldSize_2 = TileSystem.MapSize(mOsmv.getZoomLevel()) / 2;
		mOsmv.scrollTo(p.x - worldSize_2, p.y - worldSize_2);
	}

	/**
	 * Set the map view to the given center. There will be no animation.
	 */
	@Override
	public void setCenter(final IGeoPoint point) {
		final Point p = TileSystem.LatLongToPixelXY(
				point.getLatitudeE6() / 1E6, point.getLongitudeE6() / 1E6,
				mOsmv.getZoomLevel(), null);
		final int worldSize_2 = TileSystem.MapSize(mOsmv.getZoomLevel()) / 2;
		mOsmv.scrollTo(p.x - worldSize_2, p.y - worldSize_2);
	}

	@Override
	public int setZoom(final int zoomlevel) {
		return mOsmv.setZoomLevel(zoomlevel);
	}

	/**
	 * Stops a running animation.
	 * 
	 * @param jumpToTarget
	 */
	public void stopAnimation(final boolean jumpToTarget) {
		final AbstractAnimationRunner currentAnimationRunner = mCurrentAnimationRunner;

		if (currentAnimationRunner != null && !currentAnimationRunner.isDone()) {
			currentAnimationRunner.interrupt();
			if (jumpToTarget) {
				setCenter(new GeoPoint(
						currentAnimationRunner.mTargetLatitudeE6,
						currentAnimationRunner.mTargetLongitudeE6));
			}
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Zoom in by one zoom level.
	 */
	@Override
	public boolean zoomIn() {
		return mOsmv.zoomIn();
	}

	public boolean zoomInFixing(final GeoPoint point) {
		return mOsmv.zoomInFixing(point);
	}

	@Override
	public boolean zoomInFixing(final int xPixel, final int yPixel) {
		return mOsmv.zoomInFixing(xPixel, yPixel);
	}

	/**
	 * Zoom out by one zoom level.
	 */
	@Override
	public boolean zoomOut() {
		return mOsmv.zoomOut();
	}

	public boolean zoomOutFixing(final GeoPoint point) {
		return mOsmv.zoomOutFixing(point);
	}

	@Override
	public boolean zoomOutFixing(final int xPixel, final int yPixel) {
		return mOsmv.zoomOutFixing(xPixel, yPixel);
	}

	public void zoomToSpan(final BoundingBoxE6 bb) {
		zoomToSpan(bb.getLatitudeSpanE6(), bb.getLongitudeSpanE6());
	}

	// TODO rework zoomToSpan
	@Override
	public void zoomToSpan(final int reqLatSpan, final int reqLonSpan) {
		try {
			if (reqLatSpan <= 0 || reqLonSpan <= 0) {
				return;
			}

			final BoundingBoxE6 bb = mOsmv.getBoundingBox();
			final int curZoomLevel = mOsmv.getZoomLevel();

			final int curLatSpan = bb.getLatitudeSpanE6();
			final int curLonSpan = bb.getLongitudeSpanE6();

			final float diffNeededLat = (float) reqLatSpan / curLatSpan; // i.e.
																			// 600/500
																			// =
																			// 1,2
			final float diffNeededLon = (float) reqLonSpan / curLonSpan; // i.e.
																			// 300/400
																			// =
																			// 0,75
			final float diffNeeded = Math.max(diffNeededLat, diffNeededLon); // i.e.
																				// 1,2

			if (diffNeeded > 1 && diffNeeded != Float.POSITIVE_INFINITY) { // Zoom
																			// Out
				mOsmv.setZoomLevel(curZoomLevel
						- MyMath.getNextSquareNumberAbove(diffNeeded));
			} else if (diffNeeded < 0.5
					&& diffNeeded != Float.POSITIVE_INFINITY) { // Can Zoom in
				mOsmv.setZoomLevel(curZoomLevel
						+ MyMath.getNextSquareNumberAbove(1 / diffNeeded) - 1);
			}
		} catch (final Exception ex) {
			Log.d("MapsController", "zoomToSpan", ex);
		}
	}
}
