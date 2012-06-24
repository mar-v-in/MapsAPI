// Created by plusminus on 17:45:56 - 25.09.2008
package com.google.android.maps;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import microsoft.mappoint.TileSystem;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Scroller;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;
import android.widget.ZoomControls;

public class MapView extends ViewGroup implements IMapView, MapViewConstants,
		MultiTouchObjectCanvas<Object> {

	// ===========================================================
	// OpenMaps added
	// ===========================================================

	/**
	 * Per-child layout information associated with OpenStreetMapView.
	 */
	public static class LayoutParams extends ViewGroup.LayoutParams {

		/**
		 * Special value for the alignment requested by a View. TOP_LEFT means
		 * that the location will at the top left the View.
		 */
		public static final int TOP_LEFT = 1;
		/**
		 * Special value for the alignment requested by a View. TOP_RIGHT means
		 * that the location will be centered at the top of the View.
		 */
		public static final int TOP_CENTER = 2;
		/**
		 * Special value for the alignment requested by a View. TOP_RIGHT means
		 * that the location will at the top right the View.
		 */
		public static final int TOP_RIGHT = 3;
		/**
		 * Special value for the alignment requested by a View. CENTER_LEFT
		 * means that the location will at the center left the View.
		 */
		public static final int CENTER_LEFT = 4;
		/**
		 * Special value for the alignment requested by a View. CENTER means
		 * that the location will be centered at the center of the View.
		 */
		public static final int CENTER = 5;
		/**
		 * Special value for the alignment requested by a View. CENTER_RIGHT
		 * means that the location will at the center right the View.
		 */
		public static final int CENTER_RIGHT = 6;
		/**
		 * Special value for the alignment requested by a View. BOTTOM_LEFT
		 * means that the location will be at the bottom left of the View.
		 */
		public static final int BOTTOM_LEFT = 7;
		/**
		 * Special value for the alignment requested by a View. BOTTOM_CENTER
		 * means that the location will be centered at the bottom of the view.
		 */
		public static final int BOTTOM_CENTER = 8;
		/**
		 * Special value for the alignment requested by a View. BOTTOM_RIGHT
		 * means that the location will be at the bottom right of the View.
		 */
		public static final int BOTTOM_RIGHT = 9;
		/**
		 * The location of the child within the map view.
		 */
		public GeoPoint geoPoint;

		/**
		 * The alignment the alignment of the view compared to the location.
		 */
		public int alignment;

		public int offsetX;
		public int offsetY;

		/**
		 * Since we cannot use XML files in this project this constructor is
		 * useless. Creates a new set of layout parameters. The values are
		 * extracted from the supplied attributes set and context.
		 * 
		 * @param c
		 *            the application environment
		 * @param attrs
		 *            the set of attributes fom which to extract the layout
		 *            parameters values
		 */
		public LayoutParams(final Context c, final AttributeSet attrs) {
			super(c, attrs);
			geoPoint = new GeoPoint(0, 0);
			alignment = BOTTOM_CENTER;
		}

		/**
		 * Creates a new set of layout parameters with the specified width,
		 * height and location.
		 * 
		 * @param width
		 *            the width, either {@link #FILL_PARENT},
		 *            {@link #WRAP_CONTENT} or a fixed size in pixels
		 * @param height
		 *            the height, either {@link #FILL_PARENT},
		 *            {@link #WRAP_CONTENT} or a fixed size in pixels
		 * @param geoPoint
		 *            the location of the child within the map view
		 * @param alignment
		 *            the alignment of the view compared to the location
		 *            {@link #BOTTOM_CENTER}, {@link #BOTTOM_LEFT},
		 *            {@link #BOTTOM_RIGHT} {@link #TOP_CENTER},
		 *            {@link #TOP_LEFT}, {@link #TOP_RIGHT}
		 * @param offsetX
		 *            the additional X offset from the alignment location to
		 *            draw the child within the map view
		 * @param offsetY
		 *            the additional Y offset from the alignment location to
		 *            draw the child within the map view
		 */
		public LayoutParams(final int width, final int height,
				final GeoPoint geoPoint, final int alignment,
				final int offsetX, final int offsetY) {
			super(width, height);
			if (geoPoint != null) {
				this.geoPoint = geoPoint;
			} else {
				this.geoPoint = new GeoPoint(0, 0);
			}
			this.alignment = alignment;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}

		/**
		 * {@inheritDoc}
		 */
		public LayoutParams(final ViewGroup.LayoutParams source) {
			super(source);
		}
	}

	private class MapViewDoubleClickListener implements
			GestureDetector.OnDoubleTapListener {
		@Override
		public boolean onDoubleTap(final MotionEvent e) {
			if (getOverlayManager().onDoubleTap(e, MapView.this)) {
				return true;
			}

			final IGeoPoint center = getProjection().fromPixels(e.getX(),
					e.getY());
			return zoomInFixing(center);
		}

		@Override
		public boolean onDoubleTapEvent(final MotionEvent e) {
			if (getOverlayManager().onDoubleTapEvent(e, MapView.this)) {
				return true;
			}

			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(final MotionEvent e) {
			if (getOverlayManager().onSingleTapConfirmed(e, MapView.this)) {
				return true;
			}

			return false;
		}
	}

	private class MapViewGestureDetectorListener implements OnGestureListener {

		@Override
		public boolean onDown(final MotionEvent e) {
			if (getOverlayManager().onDown(e, MapView.this)) {
				return true;
			}

			mZoomController.setVisible(mEnableZoomController);
			return true;
		}

		@Override
		public boolean onFling(final MotionEvent e1, final MotionEvent e2,
				final float velocityX, final float velocityY) {
			if (getOverlayManager().onFling(e1, e2, velocityX, velocityY,
					MapView.this)) {
				return true;
			}

			final int worldSize = TileSystem.MapSize(mZoomLevel);
			mScroller.fling(getScrollX(), getScrollY(), (int) -velocityX,
					(int) -velocityY, -worldSize, worldSize, -worldSize,
					worldSize);
			return true;
		}

		@Override
		public void onLongPress(final MotionEvent e) {
			if (mMultiTouchController != null
					&& mMultiTouchController.isPinching()) {
				return;
			}
			getOverlayManager().onLongPress(e, MapView.this);
		}

		@Override
		public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
				final float distanceX, final float distanceY) {
			if (getOverlayManager().onScroll(e1, e2, distanceX, distanceY,
					MapView.this)) {
				return true;
			}

			scrollBy((int) distanceX, (int) distanceY);
			return true;
		}

		@Override
		public void onShowPress(final MotionEvent e) {
			getOverlayManager().onShowPress(e, MapView.this);
		}

		@Override
		public boolean onSingleTapUp(final MotionEvent e) {
			if (getOverlayManager().onSingleTapUp(e, MapView.this)) {
				return true;
			}

			return false;
		}

	}

	private class MapViewZoomListener implements OnZoomListener {
		@Override
		public void onVisibilityChanged(final boolean visible) {
		}

		@Override
		public void onZoom(final boolean zoomIn) {
			if (zoomIn) {
				getController().zoomIn();
			} else {
				getController().zoomOut();
			}
		}
	}

	enum Mode {
		Normal, Satellite, StreetView, Traffic
	}

	/**
	 * A Projection serves to translate between the coordinate system of x/y
	 * on-screen pixel coordinates and that of latitude/longitude points on the
	 * surface of the earth. You obtain a Projection from
	 * MapView.getProjection(). You should not hold on to this object for more
	 * than one draw, since the projection of the map could change. <br />
	 * <br />
	 * <I>Screen coordinates</I> are in the coordinate system of the screen's
	 * Canvas. The origin is in the center of the plane. <I>Screen
	 * coordinates</I> are appropriate for using to draw to the screen.<br />
	 * <br />
	 * <I>Map coordinates</I> are in the coordinate system of the standard
	 * Mercator projection. The origin is in the upper-left corner of the plane.
	 * <I>Map coordinates</I> are appropriate for use in the TileSystem class.<br />
	 * <br />
	 * <I>Intermediate coordinates</I> are used to cache the computationally
	 * heavy part of the projection. They aren't suitable for use until
	 * translated into <I>screen coordinates</I> or <I>map coordinates</I>.
	 * 
	 * @author Nicolas Gramlich
	 * @author Manuel Stahl
	 */
	public class ProjectionImplementation implements IProjection, GeoConstants,
			Projection {

		// ===========================================================
		// OpenMaps added
		// ===========================================================

		/**
		 * 
		 */
		private final MapView mapView;

		ProjectionImplementation(MapView mapView) {

			this.mapView = mapView;
		}

		@Override
		public Point fromMapPixels(final int x, final int y, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();
			out.set(x - getViewWidth2(), y - getViewHeight2());
			out.offset(mapView.getScrollX(), mapView.getScrollY());
			return out;
		}

		/**
		 * Converts <I>screen coordinates</I> to the underlying GeoPoint.
		 * 
		 * @param x
		 * @param y
		 * @return GeoPoint under x/y.
		 */
		@Override
		public GeoPoint fromPixels(final float x, final float y) {
			final Rect screenRect = getScreenRect();
			return TileSystem.PixelXYToLatLong(screenRect.left + (int) x
					+ getWorldSize2(), screenRect.top + (int) y
					+ getWorldSize2(), getZoomLevel(), null);
		}

		@Override
		public GeoPoint fromPixels(final int x, final int y) {
			return fromPixels((float) x, (float) y);
		}

		/**
		 * Translates a rectangle from <I>screen coordinates</I> to
		 * <I>intermediate coordinates</I>.
		 * 
		 * @param in
		 *            the rectangle in <I>screen coordinates</I>
		 * @return a rectangle in </I>intermediate coordindates</I>.
		 */
		@Override
		public Rect fromPixelsToProjected(final Rect in) {
			final Rect result = new Rect();

			final int zoomDifference = MapViewConstants.MAXIMUM_ZOOMLEVEL
					- getZoomLevel();

			final int x0 = in.left - getOffsetX() << zoomDifference;
			final int x1 = in.right - getOffsetX() << zoomDifference;
			final int y0 = in.bottom - getOffsetY() << zoomDifference;
			final int y1 = in.top - getOffsetY() << zoomDifference;

			result.set(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1),
					Math.max(y0, y1));
			return result;
		}

		public BoundingBoxE6 getBoundingBox() {
			return mapView.getBoundingBox();
		}

		/**
		 * @deprecated Use
		 *             <code>Point out = TileSystem.PixelXYToTileXY(screenRect.centerX(), screenRect.centerY(), null);</code>
		 *             instead.
		 */
		@Deprecated
		public Point getCenterMapTileCoords() {
			final Rect rect = getScreenRect();
			return TileSystem.PixelXYToTileXY(rect.centerX(), rect.centerY(),
					null);
		}

		private int getOffsetX() {
			return -getWorldSize2();
		}

		private int getOffsetY() {
			return -getWorldSize2();
		}

		@Override
		public Rect getScreenRect() {
			return mapView.getScreenRect(null);
		}

		/**
		 * @deprecated Use TileSystem.getTileSize() instead.
		 */
		@Override
		@Deprecated
		public int getTileSizePixels() {
			return TileSystem.getTileSize();
		}

		/**
		 * @deprecated Use
		 *             <code>final Point out = TileSystem.TileXYToPixelXY(centerMapTileCoords.x, centerMapTileCoords.y, null);</code>
		 *             instead.
		 */
		@Deprecated
		public Point getUpperLeftCornerOfCenterMapTile() {
			final Point centerMapTileCoords = getCenterMapTileCoords();
			return TileSystem.TileXYToPixelXY(centerMapTileCoords.x,
					centerMapTileCoords.y, null);
		}

		private int getViewHeight2() {
			return mapView.getHeight() / 2;
		}

		private int getViewWidth2() {
			return mapView.getWidth() / 2;
		}

		private int getWorldSize2() {
			return TileSystem.MapSize(mapView.mZoomLevel) / 2;
		}

		@Override
		public int getZoomLevel() {
			return mapView.mZoomLevel;
		}

		@Override
		public float metersToEquatorPixels(final float meters) {
			return meters
					/ (float) TileSystem.GroundResolution(0, getZoomLevel());
		}

		/**
		 * Converts a GeoPoint to its <I>screen coordinates</I>.
		 * 
		 * @param in
		 *            the GeoPoint you want the <I>screen coordinates</I> of
		 * @param reuse
		 *            just pass null if you do not have a Point to be
		 *            'recycled'.
		 * @return the Point containing the <I>screen coordinates</I> of the
		 *         GeoPoint passed.
		 */
		@Override
		public Point toMapPixels(final IGeoPoint in, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();

			TileSystem.LatLongToPixelXY(in.getLatitudeE6() / 1E6,
					in.getLongitudeE6() / 1E6, getZoomLevel(), out);
			out.offset(getOffsetX(), getOffsetY());
			if (Math.abs(out.x - getScrollX()) > Math.abs(out.x
					- TileSystem.MapSize(getZoomLevel()) - getScrollX())) {
				out.x -= TileSystem.MapSize(getZoomLevel());
			}
			if (Math.abs(out.y - getScrollY()) > Math.abs(out.y
					- TileSystem.MapSize(getZoomLevel()) - getScrollY())) {
				out.y -= TileSystem.MapSize(getZoomLevel());
			}
			return out;
		}

		/**
		 * Performs only the first computationally heavy part of the projection.
		 * Call toMapPixelsTranslated to get the final position.
		 * 
		 * @param latituteE6
		 *            the latitute of the point
		 * @param longitudeE6
		 *            the longitude of the point
		 * @param reuse
		 *            just pass null if you do not have a Point to be
		 *            'recycled'.
		 * @return intermediate value to be stored and passed to
		 *         toMapPixelsTranslated.
		 */
		@Override
		public Point toMapPixelsProjected(final int latituteE6,
				final int longitudeE6, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();

			TileSystem.LatLongToPixelXY(latituteE6 / 1E6, longitudeE6 / 1E6,
					MapViewConstants.MAXIMUM_ZOOMLEVEL, out);
			return out;
		}

		/**
		 * Performs the second computationally light part of the projection.
		 * Returns results in <I>screen coordinates</I>.
		 * 
		 * @param in
		 *            the Point calculated by the toMapPixelsProjected
		 * @param reuse
		 *            just pass null if you do not have a Point to be
		 *            'recycled'.
		 * @return the Point containing the <I>Screen coordinates</I> of the
		 *         initial GeoPoint passed to the toMapPixelsProjected.
		 */
		@Override
		public Point toMapPixelsTranslated(final Point in, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();

			final int zoomDifference = MapViewConstants.MAXIMUM_ZOOMLEVEL
					- getZoomLevel();
			out.set((in.x >> zoomDifference) + getOffsetX(),
					(in.y >> zoomDifference) + getOffsetY());
			return out;
		}

		// not presently used
		public Rect toPixels(final BoundingBoxE6 pBoundingBoxE6) {
			final Rect rect = new Rect();

			final Point reuse = new Point();

			toMapPixels(new GeoPoint(pBoundingBoxE6.getLatNorthE6(),
					pBoundingBoxE6.getLonWestE6()), reuse);
			rect.left = reuse.x;
			rect.top = reuse.y;

			toMapPixels(new GeoPoint(pBoundingBoxE6.getLatSouthE6(),
					pBoundingBoxE6.getLonEastE6()), reuse);
			rect.right = reuse.x;
			rect.bottom = reuse.y;

			return rect;
		}

		@Override
		public Point toPixels(GeoPoint in, Point out) {
			return toPixels(in, out, false);
		}

		@Override
		public Point toPixels(GeoPoint in, Point out, boolean flag) {
			toMapPixels(in, out);
			if (flag) {
				out.offset(-mapView.getScrollX(), -mapView.getScrollY());
				out.offset(getViewWidth2(), getViewHeight2());
			}
			return out;
		}

		@Override
		public Point toPixels(final IGeoPoint in, final Point out) {
			return toMapPixels(in, out);
		}

		/**
		 * @deprecated Use TileSystem.TileXYToPixelXY
		 */
		@Deprecated
		public Point toPixels(final int tileX, final int tileY,
				final Point reuse) {
			return TileSystem.TileXYToPixelXY(tileX, tileY, reuse);
		}

		/**
		 * @deprecated Use TileSystem.TileXYToPixelXY
		 */
		@Deprecated
		public Point toPixels(final Point tileCoords, final Point reuse) {
			return toPixels(tileCoords.x, tileCoords.y, reuse);
		}
	}

	private static final String KEY_CENTER_LATITUDE = (new StringBuilder())
			.append(MapView.class.getName()).append(".centerLatitude")
			.toString();

	private static final String KEY_CENTER_LONGITUDE = (new StringBuilder())
			.append(MapView.class.getName()).append(".centerLongitude")
			.toString();

	private static final String KEY_ZOOM_DISPLAYED = (new StringBuilder())
			.append(MapView.class.getName()).append(".zoomDisplayed")
			.toString();

	private static final String KEY_ZOOM_LEVEL = (new StringBuilder())
			.append(MapView.class.getName()).append(".zoomLevel").toString();

	private ZoomControls zoomControls;

	Mode mode = Mode.Normal;

	private static final double ZOOM_SENSITIVITY = 1.3;

	private static final double ZOOM_LOG_BASE_INV = 1.0 / Math
			.log(2.0 / ZOOM_SENSITIVITY);

	/** Current zoom level for map tiles. */
	int mZoomLevel = 0;

	private final OverlayManager mOverlayManager;

	private ProjectionImplementation mProjection;

	private final TilesOverlay mMapOverlay;

	private final GestureDetector mGestureDetector;

	/** Handles map scrolling */
	private final Scroller mScroller;

	private final AtomicInteger mTargetZoomLevel = new AtomicInteger();

	private final AtomicBoolean mIsAnimating = new AtomicBoolean(false);

	// ===========================================================
	// Constants
	// ===========================================================

	private final ScaleAnimation mZoomInAnimation;

	private final ScaleAnimation mZoomOutAnimation;
	private final MapController mController;

	// ===========================================================
	// Fields
	// ===========================================================

	// XXX we can use android.widget.ZoomButtonsController if we upgrade the
	// dependency to Android 1.6
	private final ZoomButtonsController mZoomController;

	private boolean mEnableZoomController = false;

	private final ResourceProxy mResourceProxy;

	private MultiTouchController<Object> mMultiTouchController;

	private float mMultiTouchScale = 1.0f;

	protected MapListener mListener;
	// for speed (avoiding allocations)
	private final Matrix mMatrix = new Matrix();
	private final MapTileProviderBase mTileProvider;

	private final Handler mTileRequestCompleteHandler;
	/* a point that will be reused to design added views */
	private final Point mPoint = new Point();

	/**
	 * Constructor used by XML layout resource (uses default tile source).
	 */
	public MapView(final Context context, final AttributeSet attrs) {
		this(context, 256, new DefaultResourceProxyImpl(context), null, null,
				attrs);
	}

	public MapView(final Context context, final AttributeSet attrs, int i) {
		this(context, 256, new DefaultResourceProxyImpl(context), null, null,
				attrs);
	}

	/**
	 * Standard Constructor.
	 */
	public MapView(final Context context, final int tileSizePixels) {
		this(context, tileSizePixels, new DefaultResourceProxyImpl(context));
	}

	public MapView(final Context context, final int tileSizePixels,
			final ResourceProxy resourceProxy) {
		this(context, tileSizePixels, resourceProxy, null);
	}

	public MapView(final Context context, final int tileSizePixels,
			final ResourceProxy resourceProxy,
			final MapTileProviderBase aTileProvider) {
		this(context, tileSizePixels, resourceProxy, aTileProvider, null);
	}

	public MapView(final Context context, final int tileSizePixels,
			final ResourceProxy resourceProxy,
			final MapTileProviderBase aTileProvider,
			final Handler tileRequestCompleteHandler) {
		this(context, tileSizePixels, resourceProxy, aTileProvider,
				tileRequestCompleteHandler, null);
	}

	protected MapView(final Context context, final int tileSizePixels,
			final ResourceProxy resourceProxy,
			MapTileProviderBase tileProvider,
			final Handler tileRequestCompleteHandler, final AttributeSet attrs) {
		super(context, attrs);
		mResourceProxy = resourceProxy;
		mController = new MapController(this);
		mScroller = new Scroller(context);
		TileSystem.setTileSize(tileSizePixels);

		if (tileProvider == null) {
			final ITileSource tileSource = getTileSourceFromAttributes(attrs);
			tileProvider = new MapTileProviderBasic(context, tileSource);
		}

		mTileRequestCompleteHandler = tileRequestCompleteHandler == null ? new SimpleInvalidationHandler(
				this) : tileRequestCompleteHandler;
		mTileProvider = tileProvider;
		mTileProvider
				.setTileRequestCompleteHandler(mTileRequestCompleteHandler);

		mMapOverlay = new TilesOverlay(mTileProvider, mResourceProxy);
		mOverlayManager = new OverlayManager(mMapOverlay);

		mZoomController = new ZoomButtonsController(this);
		mZoomController.setOnZoomListener(new MapViewZoomListener());

		mZoomInAnimation = new ScaleAnimation(1, 2, 1, 2,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mZoomOutAnimation = new ScaleAnimation(1, 0.5f, 1, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mZoomInAnimation.setDuration(ANIMATION_DURATION_SHORT);
		mZoomOutAnimation.setDuration(ANIMATION_DURATION_SHORT);

		mGestureDetector = new GestureDetector(context,
				new MapViewGestureDetectorListener());
		mGestureDetector
				.setOnDoubleTapListener(new MapViewDoubleClickListener());
		someInit(); // OpenMaps
	}

	public MapView(Context context, String s) {
		this(context, 256, new DefaultResourceProxyImpl(context));
	}

	public boolean canZoomIn() {
		try {
			final int maxZoomLevel = getMaxZoomLevel();
			if (mZoomLevel >= maxZoomLevel) {
				return false;
			}
			if (mIsAnimating.get() & mTargetZoomLevel.get() >= maxZoomLevel) {
				return false;
			}
			return true;
		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "canZoomIn", ex);
			return false;
		}
	}

	public boolean canZoomOut() {
		try {
			final int minZoomLevel = getMinZoomLevel();
			if (mZoomLevel <= minZoomLevel) {
				return false;
			}
			if (mIsAnimating.get() && mTargetZoomLevel.get() <= minZoomLevel) {
				return false;
			}
			return true;
		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "canZoomOut", ex);
			return false;
		}
	}

	// Override to allow type-checking of LayoutParams.
	@Override
	protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
		return p instanceof MapView.LayoutParams;
	}

	// ===========================================================
	// Constructors
	// ===========================================================

	private void checkZoomButtons() {
		mZoomController.setZoomInEnabled(canZoomIn());
		mZoomController.setZoomOutEnabled(canZoomOut());
	}

	@Override
	public void computeScroll() {
		try {
			if (mScroller.computeScrollOffset()) {
				if (mScroller.isFinished()) {
					// This will facilitate snapping-to any Snappable points.
					setZoomLevel(mZoomLevel);
				} else {
					scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
				}
				postInvalidate(); // Keep on drawing until the animation has
				// finished.
			}

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "computeScroll", ex);
		}
	}

	@Override
	protected void dispatchDraw(final Canvas c) {
		final long startMs = System.currentTimeMillis();

		mProjection = new ProjectionImplementation(this);

		// Save the current canvas matrix
		c.save();

		if (mMultiTouchScale == 1.0f) {
			c.translate(getWidth() / 2, getHeight() / 2);
		} else {
			c.getMatrix(mMatrix);
			mMatrix.postTranslate(getWidth() / 2, getHeight() / 2);
			mMatrix.preScale(mMultiTouchScale, mMultiTouchScale, getScrollX(),
					getScrollY());
			c.setMatrix(mMatrix);
		}

		/* Draw background */
		// c.drawColor(mBackgroundColor);

		/* Draw all Overlays. */
		getOverlayManager().onDraw(c, this);

		// Restore the canvas matrix
		c.restore();

		super.dispatchDraw(c);

		if (DEBUGMODE) {
			final long endMs = System.currentTimeMillis();
			Log.d("MapaAPI", "MapView: Rendering overall: " + (endMs - startMs) + "ms");
		}
	}

	@Override
	public boolean dispatchTouchEvent(final MotionEvent event) {

		if (DEBUGMODE) {
			Log.d("MapaAPI", "MapView: dispatchTouchEvent(" + event + ")");
		}

		if (mZoomController.isVisible() && mZoomController.onTouch(this, event)) {
			return true;
		}

		if (getOverlayManager().onTouchEvent(event, this)) {
			return true;
		}

		if (mMultiTouchController != null
				&& mMultiTouchController.onTouchEvent(event)) {
			if (DEBUGMODE) {
				Log.d("MapaAPI", "MapView: mMultiTouchController handled onTouchEvent");
			}
			return true;
		}

		final boolean r = super.dispatchTouchEvent(event);

		if (mGestureDetector.onTouchEvent(event)) {
			if (DEBUGMODE) {
				Log.d("MapaAPI", "MapView: mGestureDetector handled onTouchEvent");
			}
			return true;
		}

		if (r) {
			if (DEBUGMODE) {
				Log.d("MapaAPI", "MapView: super handled onTouchEvent");
			}
		} else {
			if (DEBUGMODE) {
				Log.d("MapaAPI", "MapView: no-one handled onTouchEvent");
			}
		}
		return r;
	}

	public void displayZoomControls(boolean bool) {
		if (zoomControls == null) {
			getZoomControls();
		}
		zoomControls.setVisibility(VISIBLE);
	}

	/**
	 * Returns a set of layout parameters with a width of
	 * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}, a height of
	 * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} at the
	 * {@link GeoPoint} (0, 0) align with
	 * {@link MapView.LayoutParams#BOTTOM_CENTER}.
	 */
	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, null,
				MapView.LayoutParams.BOTTOM_CENTER, 0, 0);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
		return new MapView.LayoutParams(getContext(), attrs);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(
			final ViewGroup.LayoutParams p) {
		return new MapView.LayoutParams(p);
	}

	public BoundingBoxE6 getBoundingBox() {
		return getBoundingBox(getWidth(), getHeight());
	}

	public BoundingBoxE6 getBoundingBox(final int pViewWidth,
			final int pViewHeight) {

		final int world_2 = TileSystem.MapSize(mZoomLevel) / 2;
		final Rect screenRect = getScreenRect(null);
		screenRect.offset(world_2, world_2);

		final IGeoPoint neGeoPoint = TileSystem.PixelXYToLatLong(
				screenRect.right, screenRect.top, mZoomLevel, null);
		final IGeoPoint swGeoPoint = TileSystem.PixelXYToLatLong(
				screenRect.left, screenRect.bottom, mZoomLevel, null);

		return new BoundingBoxE6(neGeoPoint.getLatitudeE6(),
				neGeoPoint.getLongitudeE6(), swGeoPoint.getLatitudeE6(),
				swGeoPoint.getLongitudeE6());
	}

	@Override
	public MapController getController() {
		return mController;
	}

	@Override
	public Object getDraggableObjectAtPoint(final PointInfo pt) {
		return this;
	}

	@Override
	public int getLatitudeSpan() {
		return this.getBoundingBox().getLatitudeSpanE6();
	}

	@Override
	public int getLongitudeSpan() {
		return this.getBoundingBox().getLongitudeSpanE6();
	}

	/**
	 * Returns the current center-point position of the map, as a GeoPoint
	 * (latitude and longitude).
	 * 
	 * @return A GeoPoint of the map's center-point.
	 */
	@Override
	public GeoPoint getMapCenter() {
		try {
			final int world_2 = TileSystem.MapSize(mZoomLevel) / 2;
			final Rect screenRect = getScreenRect(null);
			screenRect.offset(world_2, world_2);
			return TileSystem.PixelXYToLatLong(screenRect.centerX(),
					screenRect.centerY(), mZoomLevel, null);

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "getMapCenter", ex);
			return null;
		}
	}

	/**
	 * Returns the maximum zoom level for the point currently at the center.
	 * 
	 * @return The maximum zoom level for the map's current center.
	 */
	@Override
	public int getMaxZoomLevel() {
		return mMapOverlay.getMaximumZoomLevel();
	}

	/**
	 * Returns the minimum zoom level for the point currently at the center.
	 * 
	 * @return The minimum zoom level for the map's current center.
	 */
	public int getMinZoomLevel() {
		return mMapOverlay.getMinimumZoomLevel();
	}

	public OverlayManager getOverlayManager() {
		return mOverlayManager;
	}

	/**
	 * You can add/remove/reorder your Overlays using the List of
	 * {@link Overlay}. The first (index 0) Overlay gets drawn first, the one
	 * with the highest as the last one.
	 */
	public List<Overlay> getOverlays() {
		return getOverlayManager();
	}

	@Override
	public void getPositionAndScale(final Object obj,
			final PositionAndScale objPosAndScaleOut) {
		objPosAndScaleOut.set(0, 0, true, mMultiTouchScale, false, 0, 0, false,
				0);
	}

	/**
	 * Get a projection for converting between screen-pixel coordinates and
	 * latitude/longitude coordinates. You should not hold on to this object for
	 * more than one draw, since the projection of the map could change.
	 * 
	 * @return The Projection of the map in its current state. You should not
	 *         hold on to this object for more than one draw, since the
	 *         projection of the map could change.
	 */
	@Override
	public Projection getProjection() {
		if (mProjection == null) {
			mProjection = new ProjectionImplementation(this);
		}
		return mProjection;
	}

	public ResourceProxy getResourceProxy() {
		return mResourceProxy;
	}

	/**
	 * Gets the current bounds of the screen in <I>screen coordinates</I>.
	 */
	public Rect getScreenRect(final Rect reuse) {
		final Rect out = reuse == null ? new Rect() : reuse;
		out.set(getScrollX() - getWidth() / 2, getScrollY() - getHeight() / 2,
				getScrollX() + getWidth() / 2, getScrollY() + getHeight() / 2);
		return out;
	}

	public Scroller getScroller() {
		return mScroller;
	}

	public MapTileProviderBase getTileProvider() {
		return mTileProvider;
	}

	public Handler getTileRequestCompleteHandler() {
		return mTileRequestCompleteHandler;
	}

	private ITileSource getTileSourceFromAttributes(
			final AttributeSet aAttributeSet) {

		ITileSource tileSource = null;
		switch (mode) {
		case Satellite:
			tileSource = TileSourceFactory.MAPQUESTAERIAL;
			break;
		case StreetView:
			tileSource = TileSourceFactory.PUBLIC_TRANSPORT;
			break;
		case Normal:
		default:
			tileSource = TileSourceFactory.DEFAULT_TILE_SOURCE;
		}

		if (aAttributeSet != null) {
			final String tileSourceAttr = aAttributeSet.getAttributeValue(null,
					"tilesource");
			if (tileSourceAttr != null) {
				try {
					final ITileSource r = TileSourceFactory
							.getTileSource(tileSourceAttr);
					Log.i("MapaAPI", "MapView: Using tile source specified in layout attributes: "
							+ r);
					tileSource = r;
				} catch (final IllegalArgumentException e) {
					Log.w("MapaAPI", "MapView: Invalid tile souce specified in layout attributes: "
							+ tileSource);
				}
			}
		}

		if (aAttributeSet != null && tileSource instanceof IStyledTileSource) {
			final String style = aAttributeSet.getAttributeValue(null, "style");
			if (style == null) {
				Log.i("MapaAPI", "MapView: Using default style: 1");
			} else {
				Log.i("MapaAPI", "MapView: Using style specified in layout attributes: "
						+ style);
				((IStyledTileSource<?>) tileSource).setStyle(style);
			}
		}

		Log.i("MapaAPI", "MapView: Using tile source: " + tileSource);
		return tileSource;
	}

	public ZoomButtonsController getZoomButtonsController() {
		return mZoomController;
	}

	public ZoomControls getZoomControls() {
		if (zoomControls == null) {
			zoomControls = new ZoomControls(getContext());
			zoomControls.setZoomSpeed(2000);
			zoomControls.setVisibility(GONE);
			zoomControls.setOnZoomInClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					getController().zoomIn();
				}
			});
			zoomControls.setOnZoomOutClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					getController().zoomOut();
				}
			});
		}
		return zoomControls;
	}

	/**
	 * Get the current ZoomLevel for the map tiles.
	 * 
	 * @return the current ZoomLevel between 0 (equator) and 18/19(closest),
	 *         depending on the tile source chosen.
	 */
	@Override
	public int getZoomLevel() {
		return getZoomLevel(true);
	}

	/**
	 * Get the current ZoomLevel for the map tiles.
	 * 
	 * @param aPending
	 *            if true and we're animating then return the zoom level that
	 *            we're animating towards, otherwise return the current zoom
	 *            level
	 * @return the zoom level
	 */
	public int getZoomLevel(final boolean aPending) {
		if (aPending && isAnimating()) {
			return mTargetZoomLevel.get();
		} else {
			return mZoomLevel;
		}
	}

	/**
	 * Check mAnimationListener.isAnimating() to determine if view is animating.
	 * Useful for overlays to avoid recalculating during an animation sequence.
	 * 
	 * @return boolean indicating whether view is animating.
	 */
	public boolean isAnimating() {
		return mIsAnimating.get();
	}

	public boolean isSatellite() {
		return mode == Mode.Satellite;
	}

	public boolean isStreetView() {
		return mode == Mode.StreetView;
	}

	public boolean isTraffic() {
		return mode == Mode.Traffic;
	}

	@Override
	protected void onAnimationEnd() {
		mIsAnimating.set(false);
		clearAnimation();
		setZoomLevel(mTargetZoomLevel.get());
		super.onAnimationEnd();
	}

	@Override
	protected void onAnimationStart() {
		mIsAnimating.set(true);
		super.onAnimationStart();
	}

	public void onDetach() {
		getOverlayManager().onDetach(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		mZoomController.setVisible(false);
		onDetach();
		super.onDetachedFromWindow();
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		final boolean result = getOverlayManager().onKeyDown(keyCode, event,
				this);

		return result || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(final int keyCode, final KeyEvent event) {
		final boolean result = getOverlayManager()
				.onKeyUp(keyCode, event, this);

		return result || super.onKeyUp(keyCode, event);
	}

	@Override
	protected void onLayout(final boolean changed, final int l, final int t,
			final int r, final int b) {
		try {
			final int count = getChildCount();

			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != GONE) {

					final MapView.LayoutParams lp = (MapView.LayoutParams) child
							.getLayoutParams();
					final int childHeight = child.getMeasuredHeight();
					final int childWidth = child.getMeasuredWidth();
					getProjection().toMapPixels(lp.geoPoint, mPoint);
					final int x = mPoint.x + getWidth() / 2;
					final int y = mPoint.y + getHeight() / 2;
					int childLeft = x;
					int childTop = y;
					switch (lp.alignment) {
					case MapView.LayoutParams.TOP_LEFT:
						childLeft = getPaddingLeft() + x;
						childTop = getPaddingTop() + y;
						break;
					case MapView.LayoutParams.TOP_CENTER:
						childLeft = getPaddingLeft() + x - childWidth / 2;
						childTop = getPaddingTop() + y;
						break;
					case MapView.LayoutParams.TOP_RIGHT:
						childLeft = getPaddingLeft() + x - childWidth;
						childTop = getPaddingTop() + y;
						break;
					case MapView.LayoutParams.CENTER_LEFT:
						childLeft = getPaddingLeft() + x;
						childTop = getPaddingTop() + y - childHeight / 2;
						break;
					case MapView.LayoutParams.CENTER:
						childLeft = getPaddingLeft() + x - childWidth / 2;
						childTop = getPaddingTop() + y - childHeight / 2;
						break;
					case MapView.LayoutParams.CENTER_RIGHT:
						childLeft = getPaddingLeft() + x - childWidth;
						childTop = getPaddingTop() + y - childHeight / 2;
						break;
					case MapView.LayoutParams.BOTTOM_LEFT:
						childLeft = getPaddingLeft() + x;
						childTop = getPaddingTop() + y - childHeight;
						break;
					case MapView.LayoutParams.BOTTOM_CENTER:
						childLeft = getPaddingLeft() + x - childWidth / 2;
						childTop = getPaddingTop() + y - childHeight;
						break;
					case MapView.LayoutParams.BOTTOM_RIGHT:
						childLeft = getPaddingLeft() + x - childWidth;
						childTop = getPaddingTop() + y - childHeight;
						break;
					}
					childLeft += lp.offsetX;
					childTop += lp.offsetY;
					child.layout(childLeft, childTop, childLeft + childWidth,
							childTop + childHeight);
				}
			}

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "onLayout", ex);
		}
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec,
			final int heightMeasureSpec) {
		try {
			final int count = getChildCount();

			int maxHeight = 0;
			int maxWidth = 0;

			// Find out how big everyone wants to be
			measureChildren(widthMeasureSpec, heightMeasureSpec);

			// Find rightmost and bottom-most child
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				if (child.getVisibility() != GONE) {

					final MapView.LayoutParams lp = (MapView.LayoutParams) child
							.getLayoutParams();
					final int childHeight = child.getMeasuredHeight();
					final int childWidth = child.getMeasuredWidth();
					getProjection().toMapPixels(lp.geoPoint, mPoint);
					final int x = mPoint.x + getWidth() / 2;
					final int y = mPoint.y + getHeight() / 2;
					int childRight = x;
					int childBottom = y;
					switch (lp.alignment) {
					case MapView.LayoutParams.TOP_LEFT:
						childRight = x + childWidth;
						childBottom = y;
						break;
					case MapView.LayoutParams.TOP_CENTER:
						childRight = x + childWidth / 2;
						childBottom = y;
						break;
					case MapView.LayoutParams.TOP_RIGHT:
						childRight = x;
						childBottom = y;
						break;
					case MapView.LayoutParams.CENTER_LEFT:
						childRight = x + childWidth;
						childBottom = y + childHeight / 2;
						break;
					case MapView.LayoutParams.CENTER:
						childRight = x + childWidth / 2;
						childBottom = y + childHeight / 2;
						break;
					case MapView.LayoutParams.CENTER_RIGHT:
						childRight = x;
						childBottom = y + childHeight / 2;
						break;
					case MapView.LayoutParams.BOTTOM_LEFT:
						childRight = x + childWidth;
						childBottom = y + childHeight;
						break;
					case MapView.LayoutParams.BOTTOM_CENTER:
						childRight = x + childWidth / 2;
						childBottom = y + childHeight;
						break;
					case MapView.LayoutParams.BOTTOM_RIGHT:
						childRight = x;
						childBottom = y + childHeight;
						break;
					}
					childRight += lp.offsetX;
					childBottom += lp.offsetY;

					maxWidth = Math.max(maxWidth, childRight);
					maxHeight = Math.max(maxHeight, childBottom);
				}
			}

			// Account for padding too
			maxWidth += getPaddingLeft() + getPaddingRight();
			maxHeight += getPaddingTop() + getPaddingBottom();

			// Check against minimum height and width
			maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
			maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

			setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
					resolveSize(maxHeight, heightMeasureSpec));

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "onMeasure", ex);
		}
	}

	public void onRestoreInstanceState(Bundle bundle) {
		if (bundle != null) {
			if (mController != null) {
				final int lat = bundle.getInt(KEY_CENTER_LATITUDE,
						Integer.MAX_VALUE);
				final int lon = bundle.getInt(KEY_CENTER_LONGITUDE,
						Integer.MAX_VALUE);
				if (lat != Integer.MAX_VALUE && lon != Integer.MAX_VALUE) {
					mController.setCenter(new GeoPoint(lat, lon));
				}
				final int zoomLevel = bundle.getInt(KEY_ZOOM_LEVEL,
						Integer.MAX_VALUE);
				if (zoomLevel != Integer.MAX_VALUE) {
					mController.setZoom(zoomLevel);
				}
			}
			if (bundle.getInt(KEY_ZOOM_DISPLAYED, 0) != 0) {
				displayZoomControls(false);
			}
		}
	}

	public void onSaveInstanceState(Bundle bundle) {
		bundle.putInt(KEY_ZOOM_LEVEL, getZoomLevel());
		bundle.putInt(KEY_CENTER_LATITUDE, getMapCenter().getLatitudeE6());
		bundle.putInt(KEY_CENTER_LONGITUDE, getMapCenter().getLongitudeE6());
		if (mZoomController != null && mZoomController.isVisible()) {
			bundle.putInt(KEY_ZOOM_DISPLAYED, 1);
		} else {
			bundle.putInt(KEY_ZOOM_DISPLAYED, 0);
		}
	}

	@Override
	public boolean onTrackballEvent(final MotionEvent event) {

		if (getOverlayManager().onTrackballEvent(event, this)) {
			return true;
		}

		scrollBy((int) (event.getX() * 25), (int) (event.getY() * 25));

		return super.onTrackballEvent(event);
	}

	public void preLoad() {
		// NOT SUPPORTED
	}

	@Override
	public void scrollTo(int x, int y) {
		try {
			final int worldSize_2 = TileSystem.MapSize(mZoomLevel) / 2;
			while (x < -worldSize_2) {
				x += worldSize_2 * 2;
			}
			while (x > worldSize_2) {
				x -= worldSize_2 * 2;
			}
			while (y < -worldSize_2) {
				y += worldSize_2 * 2;
			}
			while (y > worldSize_2) {
				y -= worldSize_2 * 2;
			}
			super.scrollTo(x, y);

			// do callback on listener
			if (mListener != null) {
				final ScrollEvent event = new ScrollEvent(this, x, y);
				mListener.onScroll(event);
			}

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "scrollTo", ex);
		}
	}

	@Override
	public void selectObject(final Object obj, final PointInfo pt) {
		try {
			// if obj is null it means we released the pointers
			// if scale is not 1 it means we pinched
			if (obj == null && mMultiTouchScale != 1.0f) {
				final float scaleDiffFloat = (float) (Math
						.log(mMultiTouchScale) * ZOOM_LOG_BASE_INV);
				final int scaleDiffInt = Math.round(scaleDiffFloat);
				setZoomLevel(mZoomLevel + scaleDiffInt);
				// XXX maybe zoom in/out instead of zooming direct to zoom level
				// - probably not a good idea because you'll repeat the
				// animation
			}

			// reset scale
			mMultiTouchScale = 1.0f;

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "selectObject", ex);
		}
	}

	@Override
	public void setBackgroundColor(final int pColor) {
		mMapOverlay.setLoadingBackgroundColor(pColor);
		invalidate();
	}

	public void setBuiltInZoomControls(final boolean on) {
		mEnableZoomController = on;
		checkZoomButtons();
	}

	void setMapCenter(final IGeoPoint aCenter) {
		this.setMapCenter(aCenter.getLatitudeE6(), aCenter.getLongitudeE6());
	}

	void setMapCenter(final int aLatitudeE6, final int aLongitudeE6) {
		final Point coords = TileSystem.LatLongToPixelXY(aLatitudeE6 / 1E6,
				aLongitudeE6 / 1E6, getZoomLevel(), null);
		final int worldSize_2 = TileSystem.MapSize(mZoomLevel) / 2;
		if (getAnimation() == null || getAnimation().hasEnded()) {
			Log.d("MapaAPI", "MapView: StartScroll");
			mScroller.startScroll(getScrollX(), getScrollY(), coords.x
					- worldSize_2 - getScrollX(), coords.y - worldSize_2
					- getScrollY(), 500);
			postInvalidate();
		}
	}

	/*
	 * Set the MapListener for this view
	 */
	public void setMapListener(final MapListener ml) {
		mListener = ml;
	}

	public void setMultiTouchControls(final boolean on) {
		mMultiTouchController = on ? new MultiTouchController<Object>(this,
				false) : null;
	}

	// ===========================================================
	// Animation
	// ===========================================================

	@Override
	public boolean setPositionAndScale(final Object obj,
			final PositionAndScale aNewObjPosAndScale,
			final PointInfo aTouchPoint) {
		try {
			float multiTouchScale = aNewObjPosAndScale.getScale();
			// If we are at the first or last zoom level, prevent
			// pinching/expanding
			if (multiTouchScale > 1 && !canZoomIn()) {
				multiTouchScale = 1;
			}
			if (multiTouchScale < 1 && !canZoomOut()) {
				multiTouchScale = 1;
			}
			mMultiTouchScale = multiTouchScale;
			invalidate(); // redraw
			return true;

		} catch (final Exception ex) {
			Log.w(VIEW_LOG_TAG, "setPositionAndScale", ex);
			return false;
		}
	}

	public void setSatellite(boolean on) {
		mode = on ? Mode.Satellite : Mode.Normal;
		updateTileProvider();
	}

	public void setStreetView(boolean on) {
		mode = on ? Mode.StreetView : Mode.Normal;
		updateTileProvider();
	}

	// ===========================================================
	// Implementation of MultiTouchObjectCanvas
	// ===========================================================

	public void setTileSource(final ITileSource aTileSource) {
		mTileProvider.setTileSource(aTileSource);
		TileSystem.setTileSize(aTileSource.getTileSizePixels());
		checkZoomButtons();
		setZoomLevel(mZoomLevel); // revalidate zoom level
		postInvalidate();
	}

	public void setTraffic(boolean on) {
		mode = on ? Mode.Traffic : Mode.Normal;
		updateTileProvider();
	}

	/**
	 * Set whether to use the network connection if it's available.
	 * 
	 * @param aMode
	 *            if true use the network connection if it's available. if false
	 *            don't use the network connection even if it's available.
	 */
	public void setUseDataConnection(final boolean aMode) {
		mMapOverlay.setUseDataConnection(aMode);
	}

	/**
	 * @param aZoomLevel
	 *            the zoom level bound by the tile source
	 */
	int setZoomLevel(int aZoomLevel) {
		final int minZoomLevel = getMinZoomLevel();
		final int maxZoomLevel = getMaxZoomLevel();

		final int newZoomLevel = Math.max(minZoomLevel,
				Math.min(maxZoomLevel, aZoomLevel));
		final int curZoomLevel = mZoomLevel;

		mZoomLevel = newZoomLevel;
		checkZoomButtons();

		if (newZoomLevel > curZoomLevel) {
			// We are going from a lower-resolution plane to a higher-resolution
			// plane, so we have
			// to do it the hard way.
			final int worldSize_current_2 = TileSystem.MapSize(curZoomLevel) / 2;
			final int worldSize_new_2 = TileSystem.MapSize(newZoomLevel) / 2;
			final IGeoPoint centerGeoPoint = TileSystem.PixelXYToLatLong(
					getScrollX() + worldSize_current_2, getScrollY()
							+ worldSize_current_2, curZoomLevel, null);
			final Point centerPoint = TileSystem.LatLongToPixelXY(
					centerGeoPoint.getLatitudeE6() / 1E6,
					centerGeoPoint.getLongitudeE6() / 1E6, newZoomLevel, null);
			scrollTo(centerPoint.x - worldSize_new_2, centerPoint.y
					- worldSize_new_2);
		} else if (newZoomLevel < curZoomLevel) {
			// We are going from a higher-resolution plane to a lower-resolution
			// plane, so we can do
			// it the easy way.
			scrollTo(getScrollX() >> curZoomLevel - newZoomLevel,
					getScrollY() >> curZoomLevel - newZoomLevel);
		}

		// snap for all snappables
		final Point snapPoint = new Point();
		mProjection = new ProjectionImplementation(this);
		if (getOverlayManager().onSnapToItem(getScrollX(), getScrollY(),
				snapPoint, this)) {
			scrollTo(snapPoint.x, snapPoint.y);
		}

		mTileProvider.rescaleCache(newZoomLevel, curZoomLevel,
				getScreenRect(null));

		// do callback on listener
		if (newZoomLevel != curZoomLevel && mListener != null) {
			final ZoomEvent event = new ZoomEvent(this, newZoomLevel);
			mListener.onZoom(event);
		}
		// Allows any views fixed to a Location in the MapView to adjust
		requestLayout();
		return mZoomLevel;
	}

	private void someInit() {
		setMultiTouchControls(true);

	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void updateTileProvider() {
		getTileProvider().setTileSource(getTileSourceFromAttributes(null));
	}

	/**
	 * Whether to use the network connection if it's available.
	 */
	public boolean useDataConnection() {
		return mMapOverlay.useDataConnection();
	}

	/**
	 * Zoom in by one zoom level.
	 */
	boolean zoomIn() {
		if (canZoomIn()) {
			if (mIsAnimating.get()) {
				// TODO extend zoom (and return true)
				return false;
			} else {
				mTargetZoomLevel.set(mZoomLevel + 1);
				mIsAnimating.set(true);
				startAnimation(mZoomInAnimation);
				return true;
			}
		} else {
			return false;
		}
	}

	boolean zoomInFixing(final IGeoPoint point) {
		setMapCenter(point); // TODO should fix on point, not center on it
		return zoomIn();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	boolean zoomInFixing(final int xPixel, final int yPixel) {
		setMapCenter(xPixel, yPixel); // TODO should fix on point, not center on
										// it
		return zoomIn();
	}

	/**
	 * Zoom out by one zoom level.
	 */
	boolean zoomOut() {

		if (canZoomOut()) {
			if (mIsAnimating.get()) {
				// TODO extend zoom (and return true)
				return false;
			} else {
				mTargetZoomLevel.set(mZoomLevel - 1);
				mIsAnimating.set(true);
				startAnimation(mZoomOutAnimation);
				return true;
			}
		} else {
			return false;
		}
	}

	boolean zoomOutFixing(final IGeoPoint point) {
		setMapCenter(point); // TODO should fix on point, not center on it
		return zoomOut();
	}

	// ===========================================================
	// Public Classes
	// ===========================================================

	boolean zoomOutFixing(final int xPixel, final int yPixel) {
		setMapCenter(xPixel, yPixel); // TODO should fix on point, not center on
										// it
		return zoomOut();
	}

	/**
	 * Zoom the map to enclose the specified bounding box, as closely as
	 * possible. Must be called after display layout is complete, or screen
	 * dimensions are not known, and will always zoom to center of zoom level 0.
	 * Suggestion: Check getScreenRect(null).getHeight() > 0
	 */
	public void zoomToBoundingBox(final BoundingBoxE6 boundingBox) {
		final BoundingBoxE6 currentBox = getBoundingBox();

		// Calculated required zoom based on latitude span
		final double maxZoomLatitudeSpan = mZoomLevel == getMaxZoomLevel() ? currentBox
				.getLatitudeSpanE6() : currentBox.getLatitudeSpanE6()
				/ Math.pow(2, getMaxZoomLevel() - mZoomLevel);

		final double requiredLatitudeZoom = getMaxZoomLevel()
				- Math.ceil(Math.log(boundingBox.getLatitudeSpanE6()
						/ maxZoomLatitudeSpan)
						/ Math.log(2));

		// Calculated required zoom based on longitude span
		final double maxZoomLongitudeSpan = mZoomLevel == getMaxZoomLevel() ? currentBox
				.getLongitudeSpanE6() : currentBox.getLongitudeSpanE6()
				/ Math.pow(2, getMaxZoomLevel() - mZoomLevel);

		final double requiredLongitudeZoom = getMaxZoomLevel()
				- Math.ceil(Math.log(boundingBox.getLongitudeSpanE6()
						/ maxZoomLongitudeSpan)
						/ Math.log(2));

		// Zoom to boundingBox center, at calculated maximum allowed zoom level
		getController()
				.setZoom(
						(int) (requiredLatitudeZoom < requiredLongitudeZoom ? requiredLatitudeZoom
								: requiredLongitudeZoom));

		getController().setCenter(
				new GeoPoint(
						boundingBox.getCenter().getLatitudeE6() / 1000000.0,
						boundingBox.getCenter().getLongitudeE6() / 1000000.0));
	}
}
