// Created by plusminus on 19:06:38 - 25.09.2008
package com.google.android.maps;

import static com.google.android.maps.MyMath.gudermann;
import static com.google.android.maps.MyMath.gudermannInverse;

import java.io.Serializable;
import java.util.ArrayList;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public class BoundingBoxE6 implements Parcelable, Serializable,
		MapViewConstants {

	// ===========================================================
	// Constants
	// ===========================================================

	static final long serialVersionUID = 2L;

	// ===========================================================
	// Fields
	// ===========================================================

	public static BoundingBoxE6 fromGeoPoints(
			final ArrayList<? extends GeoPoint> partialPolyLine) {
		int minLat = Integer.MAX_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLon = Integer.MIN_VALUE;
		for (final GeoPoint gp : partialPolyLine) {
			final int latitudeE6 = gp.getLatitudeE6();
			final int longitudeE6 = gp.getLongitudeE6();

			minLat = Math.min(minLat, latitudeE6);
			minLon = Math.min(minLon, longitudeE6);
			maxLat = Math.max(maxLat, latitudeE6);
			maxLon = Math.max(maxLon, longitudeE6);
		}

		return new BoundingBoxE6(minLat, minLon, maxLat, maxLon);
	}

	private static BoundingBoxE6 readFromParcel(final Parcel in) {
		final int latNorthE6 = in.readInt();
		final int lonEastE6 = in.readInt();
		final int latSouthE6 = in.readInt();
		final int lonWestE6 = in.readInt();
		return new BoundingBoxE6(latNorthE6, lonEastE6, latSouthE6, lonWestE6);
	}

	protected final int mLatNorthE6;
	protected final int mLatSouthE6;

	// ===========================================================
	// Constructors
	// ===========================================================

	protected final int mLonEastE6;

	protected final int mLonWestE6;

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public static final Parcelable.Creator<BoundingBoxE6> CREATOR = new Parcelable.Creator<BoundingBoxE6>() {
		@Override
		public BoundingBoxE6 createFromParcel(final Parcel in) {
			return readFromParcel(in);
		}

		@Override
		public BoundingBoxE6[] newArray(final int size) {
			return new BoundingBoxE6[size];
		}
	};

	public BoundingBoxE6(final double north, final double east,
			final double south, final double west) {
		mLatNorthE6 = (int) (north * 1E6);
		mLonEastE6 = (int) (east * 1E6);
		mLatSouthE6 = (int) (south * 1E6);
		mLonWestE6 = (int) (west * 1E6);
	}

	public BoundingBoxE6(final int northE6, final int eastE6,
			final int southE6, final int westE6) {
		mLatNorthE6 = northE6;
		mLonEastE6 = eastE6;
		mLatSouthE6 = southE6;
		mLonWestE6 = westE6;
	}

	public GeoPoint bringToBoundingBox(final int aLatitudeE6,
			final int aLongitudeE6) {
		return new GeoPoint(Math.max(mLatSouthE6,
				Math.min(mLatNorthE6, aLatitudeE6)), Math.max(mLonWestE6,
				Math.min(mLonEastE6, aLongitudeE6)));
	}

	public boolean contains(final GeoPoint pGeoPoint) {
		return contains(pGeoPoint.getLatitudeE6(), pGeoPoint.getLongitudeE6());
	}

	public boolean contains(final int aLatitudeE6, final int aLongitudeE6) {
		return ((aLatitudeE6 < mLatNorthE6) && (aLatitudeE6 > mLatSouthE6))
				&& ((aLongitudeE6 < mLonEastE6) && (aLongitudeE6 > mLonWestE6));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * @return GeoPoint center of this BoundingBox
	 */
	public GeoPoint getCenter() {
		return new GeoPoint((mLatNorthE6 + mLatSouthE6) / 2,
				(mLonEastE6 + mLonWestE6) / 2);
	}

	public int getDiagonalLengthInMeters() {
		return new GeoPoint(mLatNorthE6, mLonWestE6).distanceTo(new GeoPoint(
				mLatSouthE6, mLonEastE6));
	}

	public GeoPoint getGeoPointOfRelativePositionWithExactGudermannInterpolation(
			final float relX, final float relY) {

		final double gudNorth = gudermannInverse(mLatNorthE6 / 1E6);
		final double gudSouth = gudermannInverse(mLatSouthE6 / 1E6);
		final double latD = gudermann((gudSouth + (1 - relY)
				* (gudNorth - gudSouth)));
		int lat = (int) (latD * 1E6);

		int lon = (int) ((mLonWestE6 + (getLongitudeSpanE6() * relX)));

		/* Bring into bounds. */
		while (lat > 90500000) {
			lat -= 90500000;
		}
		while (lat < -90500000) {
			lat += 90500000;
		}

		/* Bring into bounds. */
		while (lon > 180000000) {
			lon -= 180000000;
		}
		while (lon < -180000000) {
			lon += 180000000;
		}

		return new GeoPoint(lat, lon);
	}

	public GeoPoint getGeoPointOfRelativePositionWithLinearInterpolation(
			final float relX, final float relY) {

		int lat = (int) (mLatNorthE6 - (getLatitudeSpanE6() * relY));

		int lon = (int) (mLonWestE6 + (getLongitudeSpanE6() * relX));

		/* Bring into bounds. */
		while (lat > 90500000) {
			lat -= 90500000;
		}
		while (lat < -90500000) {
			lat += 90500000;
		}

		/* Bring into bounds. */
		while (lon > 180000000) {
			lon -= 180000000;
		}
		while (lon < -180000000) {
			lon += 180000000;
		}

		return new GeoPoint(lat, lon);
	}

	public int getLatitudeSpanE6() {
		return Math.abs(mLatNorthE6 - mLatSouthE6);
	}

	public int getLatNorthE6() {
		return mLatNorthE6;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	public int getLatSouthE6() {
		return mLatSouthE6;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public int getLonEastE6() {
		return mLonEastE6;
	}

	public int getLongitudeSpanE6() {
		return Math.abs(mLonEastE6 - mLonWestE6);
	}

	public int getLonWestE6() {
		return mLonWestE6;
	}

	public PointF getRelativePositionOfGeoPointInBoundingBoxWithExactGudermannInterpolation(
			final int aLatitudeE6, final int aLongitudeE6, final PointF reuse) {
		final PointF out = (reuse != null) ? reuse : new PointF();
		final float y = (float) ((gudermannInverse(mLatNorthE6 / 1E6) - gudermannInverse(aLatitudeE6 / 1E6)) / (gudermannInverse(mLatNorthE6 / 1E6) - gudermannInverse(mLatSouthE6 / 1E6)));
		final float x = 1 - ((float) (mLonEastE6 - aLongitudeE6) / getLongitudeSpanE6());
		out.set(x, y);
		return out;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	// ===========================================================
	// Parcelable
	// ===========================================================

	/**
	 * 
	 * @param aLatitude
	 * @param aLongitude
	 * @param reuse
	 * @return relative position determined from the upper left corner.<br />
	 *         {0,0} would be the upper left corner. {1,1} would be the lower
	 *         right corner. {1,0} would be the lower left corner. {0,1} would
	 *         be the upper right corner.
	 */
	public PointF getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
			final int aLatitude, final int aLongitude, final PointF reuse) {
		final PointF out = (reuse != null) ? reuse : new PointF();
		final float y = ((float) (mLatNorthE6 - aLatitude) / getLatitudeSpanE6());
		final float x = 1 - ((float) (mLonEastE6 - aLongitude) / getLongitudeSpanE6());
		out.set(x, y);
		return out;
	}

	public BoundingBoxE6 increaseByScale(
			final float pBoundingboxPaddingRelativeScale) {
		final GeoPoint pCenter = getCenter();
		final int mLatSpanE6Padded_2 = (int) ((getLatitudeSpanE6() * pBoundingboxPaddingRelativeScale) / 2);
		final int mLonSpanE6Padded_2 = (int) ((getLongitudeSpanE6() * pBoundingboxPaddingRelativeScale) / 2);

		return new BoundingBoxE6(pCenter.getLatitudeE6() + mLatSpanE6Padded_2,
				pCenter.getLongitudeE6() + mLonSpanE6Padded_2,
				pCenter.getLatitudeE6() - mLatSpanE6Padded_2,
				pCenter.getLongitudeE6() - mLonSpanE6Padded_2);
	}

	@Override
	public String toString() {
		return new StringBuffer().append("N:").append(mLatNorthE6)
				.append("; E:").append(mLonEastE6).append("; S:")
				.append(mLatSouthE6).append("; W:").append(mLonWestE6)
				.toString();
	}

	@Override
	public void writeToParcel(final Parcel out, final int arg1) {
		out.writeInt(mLatNorthE6);
		out.writeInt(mLonEastE6);
		out.writeInt(mLatSouthE6);
		out.writeInt(mLonWestE6);
	}
}
