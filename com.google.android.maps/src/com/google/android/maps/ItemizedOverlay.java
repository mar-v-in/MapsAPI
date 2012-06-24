// Created by plusminus on 23:18:23 - 02.10.2008
package com.google.android.maps;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.google.android.maps.OverlayItem.HotspotPlace;

/**
 * Draws a list of {@link OverlayItem} as markers to a map. The item with the
 * lowest index is drawn as last and therefore the 'topmost' marker. It also
 * gets checked for onTap first. This class is generic, because you then you get
 * your custom item-class passed back in onTap().
 * 
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * @author Fred Eisele
 * 
 * @param <Item>
 */
public abstract class ItemizedOverlay<Item extends OverlayItem> extends Overlay
		implements Overlay.Snappable {

	// ===========================================================
	// OpenMaps added
	// ===========================================================

	public static interface OnFocusChangeListener {
		public abstract void onFocusChanged(ItemizedOverlay itemizedOverlay,
				OverlayItem overlayItem);
	}

	@SuppressWarnings("deprecation")
	private static final int MINIMUM_TOUCH_DIAMETER = 4 * ViewConfiguration
			.getTouchSlop();

	private final static int MAX_MOVES_UNTIL_UP = 50;

	protected static Drawable boundCenter(Drawable drawable) {
		final int width = drawable.getIntrinsicWidth();
		final int halfwidth = width / 2;
		final int height = drawable.getIntrinsicHeight();
		final int halfheight = height / 2;
		drawable.setBounds(-halfwidth, -halfheight, width - halfwidth, height
				- halfheight);
		return drawable;
	}

	protected static Drawable boundCenterBottom(Drawable drawable) {
		final int width = drawable.getIntrinsicWidth();
		final int halfwidth = width / 2;
		final int height = drawable.getIntrinsicHeight();
		drawable.setBounds(-halfwidth, 1 - height, width - halfwidth, 1);
		return drawable;
	}

	private int inDown = 0;

	private final static int MAX_TAP_RANGE = 10;

	private int downX = -1;

	private int downY = -1;

	private OnFocusChangeListener mOnFocusChangeListener;

	private int[] mItemState;

	protected final Drawable mDefaultMarker;

	private final ArrayList<Item> mInternalItemList;

	private final Rect mRect = new Rect();

	private final Point mCurScreenCoords = new Point();

	protected boolean mDrawFocusedItem = true;

	private Item mFocusedItem;

	private int mLastFocusedIndex = -1;

	public ItemizedOverlay(Drawable drawable) {
		this(drawable, MyResourceProxy.getInstance());
	}

	public ItemizedOverlay(final Drawable pDefaultMarker,
			final ResourceProxy pResourceProxy) {

		super(pResourceProxy);

		if (pDefaultMarker == null) {
			throw new IllegalArgumentException(
					"You must pass a default marker to ItemizedOverlay.");
		}

		this.mDefaultMarker = pDefaultMarker;

		mInternalItemList = new ArrayList<Item>();
	}

	/**
	 * Adjusts a drawable's bounds so that (0,0) is a pixel in the location
	 * described by the hotspot parameter. Useful for "pin"-like graphics. For
	 * convenience, returns the same drawable that was passed in.
	 * 
	 * @param marker
	 *            the drawable to adjust
	 * @param hotspot
	 *            the hotspot for the drawable
	 * @return the same drawable that was passed in.
	 */
	protected synchronized Drawable boundToHotspot(final Drawable marker,
			HotspotPlace hotspot) {
		final int markerWidth = marker.getIntrinsicWidth();
		final int markerHeight = marker.getIntrinsicHeight();

		mRect.set(0, 0, 0 + markerWidth, 0 + markerHeight);
		if (hotspot == null) {
			hotspot = HotspotPlace.BOTTOM_CENTER;
		}

		switch (hotspot) {
		default:
		case NONE:
			break;
		case CENTER:
			mRect.offset(-markerWidth / 2, -markerHeight / 2);
			break;
		case BOTTOM_CENTER:
			mRect.offset(-markerWidth / 2, -markerHeight);
			break;
		case TOP_CENTER:
			mRect.offset(-markerWidth / 2, 0);
			break;
		case RIGHT_CENTER:
			mRect.offset(-markerWidth, -markerHeight / 2);
			break;
		case LEFT_CENTER:
			mRect.offset(0, -markerHeight / 2);
			break;
		case UPPER_RIGHT_CORNER:
			mRect.offset(-markerWidth, 0);
			break;
		case LOWER_RIGHT_CORNER:
			mRect.offset(-markerWidth, -markerHeight);
			break;
		case UPPER_LEFT_CORNER:
			mRect.offset(0, 0);
			break;
		case LOWER_LEFT_CORNER:
			mRect.offset(0, -markerHeight);
			break;
		}
		marker.setBounds(mRect);
		return marker;
	}

	/**
	 * Method by which subclasses create the actual Items. This will only be
	 * called from populate() we'll cache them for later use.
	 */
	protected abstract Item createItem(int i);

	/**
	 * Draw a marker on each of our items. populate() must have been called
	 * first.<br/>
	 * <br/>
	 * The marker will be drawn twice for each Item in the Overlay--once in the
	 * shadow phase, skewed and darkened, then again in the non-shadow phase.
	 * The bottom-center of the marker will be aligned with the geographical
	 * coordinates of the Item.<br/>
	 * <br/>
	 * The order of drawing may be changed by overriding the getIndexToDraw(int)
	 * method. An item may provide an alternate marker via its
	 * OverlayItem.getMarker(int) method. If that method returns null, the
	 * default marker is used.<br/>
	 * <br/>
	 * The focused item is always drawn last, which puts it visually on top of
	 * the other items.<br/>
	 * 
	 * @param canvas
	 *            the Canvas upon which to draw. Note that this may already have
	 *            a transformation applied, so be sure to leave it the way you
	 *            found it
	 * @param mapView
	 *            the MapView that requested the draw. Use
	 *            MapView.getProjection() to convert between on-screen pixels
	 *            and latitude/longitude pairs
	 * @param shadow
	 *            if true, draw the shadow layer. If false, draw the overlay
	 *            contents.
	 */
	@Override
	public void draw(final Canvas canvas, final MapView mapView,
			final boolean shadow) {

		if (shadow) {
			return;
		}

		final Projection pj = mapView.getProjection();
		synchronized (mInternalItemList) {
			final int size = this.mInternalItemList.size() - 1;

			/*
			 * Draw in backward cycle, so the items with the least index are on
			 * the front.
			 */
			for (int i = size; i >= 0; i--) {
				final Item item = getItemInternal(i);
				pj.toMapPixels(item.mGeoPoint, mCurScreenCoords);

				onDrawItem(canvas, item, mCurScreenCoords);
			}
		}
	}

	public GeoPoint getCenter() {
		Item item;
		synchronized (mInternalItemList) {
			item = getItemInternal(0);
		}
		if (item != null) {
			return item.getPoint();
		}
		return null;
	}

	private Drawable getDefaultMarker(final int state) {
		OverlayItem.setState(mDefaultMarker, state);
		return mDefaultMarker;
	}

	/**
	 * 
	 * @return the currently-focused item, or null if no item is currently
	 *         focused.
	 */
	public Item getFocus() {
		return mFocusedItem;
	}

	protected int getIndexToDraw(int i) {
		return i;
	}

	/**
	 * Returns the Item at the given index.
	 * 
	 * @param position
	 *            the position of the item to return
	 * @return the Item of the given index.
	 */
	public final Item getItem(final int position) {
		synchronized (mInternalItemList) {
			return getItemInternal(position);
		}
	}

	private int getItemAtLocation(int x, int y, MapView mapview) {
		final ArrayList<Integer> items = getItemsAtLocation(x, y, mapview);
		int toCenter = Integer.MAX_VALUE;
		int choosenItem = -1;
		for (final Integer i : items) {
			final Item item = getItemInternal(i);
			final Point pt = new Point();
			mapview.getProjection().toPixels(item.getPoint(), pt, true);
			final Rect rect = getTouchableBounds(item.getDrawable().getBounds());
			final int diffx = rect.centerX() - x + pt.x;
			final int diffy = rect.centerY() - y + pt.y;
			final int zsquare = diffx * diffx + diffy * diffy; // Pythagoras \o/
			if (toCenter > zsquare) {
				toCenter = zsquare;
				choosenItem = i;
			}
		}
		return choosenItem;
	}

	private Item getItemInternal(int position) {
		if (mInternalItemList.size() > position && position >= 0) {
			return mInternalItemList.get(position);
		}
		return null;
	}

	private ArrayList<Integer> getItemsAtLocation(int x, int y, MapView mapview) {
		final ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < mInternalItemList.size(); i++) {
			final Item item = getItemInternal(i);
			final Point pt = new Point();
			mapview.getProjection().toPixels(item.getPoint(), pt, true);
			if (hitTest(item, item.getDrawable(), x - pt.x, y - pt.y)) {
				list.add(i);
			}
		}
		return list;
	}

	public final int getLastFocusedIndex() {
		return mLastFocusedIndex;
	}

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	public int getLatSpanE6() {
		int latMin = Integer.MAX_VALUE;
		int latMax = Integer.MIN_VALUE;
		synchronized (mInternalItemList) {
			for (final Item item : mInternalItemList) {
				final GeoPoint geoPoint = item.getPoint();
				latMin = Math.min(latMin, geoPoint.getLatitudeE6());
				latMax = Math.max(latMax, geoPoint.getLatitudeE6());
			}
		}
		return latMax - latMin;
	}

	public int getLonSpanE6() {
		int lonMin = Integer.MAX_VALUE;
		int lonMax = Integer.MIN_VALUE;
		synchronized (mInternalItemList) {
			for (final Item item : mInternalItemList) {
				final GeoPoint geoPoint = item.getPoint();
				lonMin = Math.min(lonMin, geoPoint.getLongitudeE6());
				lonMax = Math.max(lonMax, geoPoint.getLongitudeE6());
			}
		}
		return lonMax - lonMin;
	}

	private Rect getTouchableBounds(Rect rect) {
		final int width = rect.width();
		final int height = rect.height();
		if (width < MINIMUM_TOUCH_DIAMETER || height < MINIMUM_TOUCH_DIAMETER) {
			final int newWidth = Math.max(MINIMUM_TOUCH_DIAMETER, width);
			final int newTop = rect.centerX() - newWidth / 2;
			final int newHeight = Math.max(MINIMUM_TOUCH_DIAMETER, height);
			final int newLeft = rect.centerY() - newHeight / 2;
			rect = new Rect();
			rect.set(newTop, newLeft, newTop + newWidth, newLeft + newHeight);
		}
		return rect;
	}

	/**
	 * See if a given hit point is within the bounds of an item's marker.
	 * Override to modify the way an item is hit tested. The hit point is
	 * relative to the marker's bounds. The default implementation just checks
	 * to see if the hit point is within the touchable bounds of the marker.
	 * 
	 * @param item
	 *            the item to hit test
	 * @param marker
	 *            the item's marker
	 * @param hitX
	 *            x coordinate of point to check
	 * @param hitY
	 *            y coordinate of point to check
	 * @return true if the hit point is within the marker
	 */
	protected boolean hitTest(final Item item,
			final android.graphics.drawable.Drawable marker, final int hitX,
			final int hitY) {
		if (marker == null) {
			return false;
		}
		return getTouchableBounds(marker.getBounds()).contains(hitX, hitY);
	}

	private int maskHelper(int i, int j, int k) {
		if (i != j) {
			if (i != -1) {
				mItemState[i] = mItemState[i] & ~k;
			}
			if (j != -1) {
				mItemState[j] = k | mItemState[j];
			}
		}
		return j;
	}

	public OverlayItem nextFocus(boolean forward) {
		int i;
		if (forward) {
			i = mLastFocusedIndex + 1;
		} else {
			i = mLastFocusedIndex - 1;
		}

		synchronized (mInternalItemList) {
			return getItemInternal(i);
		}
	}

	/**
	 * Draws an item located at the provided screen coordinates to the canvas.
	 * 
	 * @param canvas
	 *            what the item is drawn upon
	 * @param item
	 *            the item to be drawn
	 * @param curScreenCoords
	 *            the screen coordinates of the item
	 */
	protected void onDrawItem(final Canvas canvas, final Item item,
			final Point curScreenCoords) {
		final int state = (mDrawFocusedItem && (mFocusedItem == item) ? OverlayItem.ITEM_STATE_FOCUSED_MASK
				: 0);
		final Drawable marker = (item.getMarker(state) == null) ? getDefaultMarker(state)
				: item.getMarker(state);
		final HotspotPlace hotspot = item.getMarkerHotspot();

		boundToHotspot(marker, hotspot);

		// draw it
		Overlay.drawAt(canvas, marker, curScreenCoords.x, curScreenCoords.y,
				false);
	}

	// ===========================================================
	// Abstract methods
	// ===========================================================

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event, MapView mapView) {

		boolean flag = super.onKeyUp(keyCode, event, mapView);

		if (getFocus() == null) {
			flag = onTap(mLastFocusedIndex);
		}

		return flag;
	}

	@Override
	public boolean onSnapToItem(int x, int y, Point point, MapView mapView) {
		synchronized (mInternalItemList) {
			final int k = getItemAtLocation(x, y, mapView);
			if (k == -1) {
				return false;
			} else {
				final Item overlayitem = getItemInternal(k);
				mapView.getProjection().toPixels(overlayitem.getPoint(), point,
						true);
				return true;
			}
		}
	}

	// ===========================================================
	// Constructors
	// ===========================================================

	public boolean onTap(GeoPoint geopoint, MapView mapView) {
		final Point pt = new Point();
		mapView.getProjection().toPixels(geopoint, pt, true);
		return onTap(pt, mapView);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces (and supporting methods)
	// ===========================================================

	protected boolean onTap(int i) {
		return false;
	}

	public boolean onTap(Point pt, MapView mapView) {

		int i;
		Item item = null;
		boolean flag = false;
		synchronized (mInternalItemList) {
			i = getItemAtLocation(pt.x, pt.y, mapView);
			if (i != -1) {
				item = getItemInternal(i);
			}
		}
		if (item != null) {
			flag = onTap(i);
			setFocus(i, item);
		}
		return flag;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {

		boolean ret = false;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			inDown = MAX_MOVES_UNTIL_UP;
			downX = (int) event.getX();
			downY = (int) event.getY();
			break;
		case MotionEvent.ACTION_UP:
			if (Math.abs(event.getX() - downX) > MAX_TAP_RANGE
					|| Math.abs(event.getY() - downY) > MAX_TAP_RANGE) {
				inDown = 0;
				break;
			}
			if (inDown > 0) {
				int i;
				Item item = null;
				synchronized (mInternalItemList) {
					i = getItemAtLocation((int) event.getX(),
							(int) event.getY(), mapView);
					if (i != -1) {
						item = getItemInternal(i);
					}
				}
				if (item != null) {
					setFocus(i, item);
					ret = onTap(i);
				}
			}
			inDown = 0;
			break;
		case MotionEvent.ACTION_OUTSIDE:
		case MotionEvent.ACTION_CANCEL:
			inDown = 0;
			break;
		default:
			if (Math.abs(event.getX() - downX) > MAX_TAP_RANGE
					|| Math.abs(event.getY() - downY) > MAX_TAP_RANGE) {
				inDown = 0;
				break;
			}
			if (inDown > 0) {
				inDown--;
			}
			break;
		}
		return ret;

	}

	/**
	 * Utility method to perform all processing on a new ItemizedOverlay.
	 * Subclasses provide Items through the createItem(int) method. The subclass
	 * should call this as soon as it has data, before anything else gets
	 * called.
	 */
	protected final void populate() {
		final int size = size();
		synchronized (mInternalItemList) {
			mInternalItemList.clear();
			mInternalItemList.ensureCapacity(size);
			for (int a = 0; a < size; a++) {
				mInternalItemList.add(createItem(a));
			}
			mItemState = new int[mInternalItemList.size()];
		}
	}

	/**
	 * Set whether or not to draw the focused item. The default is to draw it,
	 * but some clients may prefer to draw the focused item themselves.
	 */
	public void setDrawFocusedItem(final boolean drawFocusedItem) {
		mDrawFocusedItem = drawFocusedItem;
	}

	private void setFocus(int i, Item item) {
		maskHelper(mLastFocusedIndex, i, 4);
		if (i != -1) {
			mLastFocusedIndex = i;
		}

		boolean sendChanged = false;
		if (mFocusedItem != item) {
			sendChanged = true;
		}

		mFocusedItem = item;

		if (sendChanged && mOnFocusChangeListener != null) {
			mOnFocusChangeListener.onFocusChanged(this, item);
		}
	}

	/**
	 * If the given Item is found in the overlay, force it to be the current
	 * focus-bearer. Any registered
	 * {@link ItemizedOverlay#OnFocusChangeListener} will be notified. This does
	 * not move the map, so if the Item isn't already centered, the user may get
	 * confused. If the Item is not found, this is a no-op. You can also pass
	 * null to remove focus.
	 */
	public void setFocus(final Item item) {
		if (item == null) {
			setFocus(mLastFocusedIndex, null);
		} else {
			synchronized (mInternalItemList) {
				for (int i = 0; i < mInternalItemList.size(); i++) {
					if (item == getItemInternal(i)) {
						setFocus(i, item);
						return;
					}
				}
			}
		}
	}

	protected void setLastFocusedIndex(int i) {
		mLastFocusedIndex = i;
	}

	public void setOnFocusChangeListener(
			OnFocusChangeListener onfocuschangelistener) {
		mOnFocusChangeListener = onfocuschangelistener;
	}

	/**
	 * The number of items in this overlay.
	 */
	public abstract int size();
}
