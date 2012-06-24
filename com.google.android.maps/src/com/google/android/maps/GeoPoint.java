// Created by plusminus on 21:28:12 - 25.09.2008
package com.google.android.maps;

import java.io.Serializable;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * 
 */
public class GeoPoint implements IGeoPoint, MathConstants, GeoConstants,
		Parcelable, Serializable, Cloneable {

	// ===========================================================
	// Constants
	// ===========================================================

	static final long serialVersionUID = 1L;

	// ===========================================================
	// Fields
	// ===========================================================

	public static final Parcelable.Creator<GeoPoint> CREATOR = new Parcelable.Creator<GeoPoint>() {
		@Override
		public GeoPoint createFromParcel(final Parcel in) {
			return new GeoPoint(in);
		}

		@Override
		public GeoPoint[] newArray(final int size) {
			return new GeoPoint[size];
		}
	};

	public static GeoPoint fromCenterBetween(final GeoPoint geoPointA,
			final GeoPoint geoPointB) {
		return new GeoPoint(
				(geoPointA.getLatitudeE6() + geoPointB.getLatitudeE6()) / 2,
				(geoPointA.getLongitudeE6() + geoPointB.getLongitudeE6()) / 2);
	}

	public static GeoPoint fromDoubleString(final String s, final char spacer) {
		final int spacerPos1 = s.indexOf(spacer);
		final int spacerPos2 = s.indexOf(spacer, spacerPos1 + 1);

		if (spacerPos2 == -1) {
			return new GeoPoint((int) (Double.parseDouble(s.substring(0,
					spacerPos1)) * 1E6), (int) (Double.parseDouble(s.substring(
					spacerPos1 + 1, s.length())) * 1E6));
		} else {
			return new GeoPoint((int) (Double.parseDouble(s.substring(0,
					spacerPos1)) * 1E6), (int) (Double.parseDouble(s.substring(
					spacerPos1 + 1, spacerPos2)) * 1E6),
					(int) Double.parseDouble(s.substring(spacerPos2 + 1,
							s.length())));
		}
	}

	// ===========================================================
	// Constructors
	// ===========================================================

	public static GeoPoint fromIntString(final String s) {
		final int commaPos1 = s.indexOf(',');
		final int commaPos2 = s.indexOf(',', commaPos1 + 1);

		if (commaPos2 == -1) {
			return new GeoPoint(Integer.parseInt(s.substring(0, commaPos1)),
					Integer.parseInt(s.substring(commaPos1 + 1, s.length())));
		} else {
			return new GeoPoint(Integer.parseInt(s.substring(0, commaPos1)),
					Integer.parseInt(s.substring(commaPos1 + 1, commaPos2)),
					Integer.parseInt(s.substring(commaPos2 + 1, s.length())));
		}
	}

	public static GeoPoint fromInvertedDoubleString(final String s,
			final char spacer) {
		final int spacerPos1 = s.indexOf(spacer);
		final int spacerPos2 = s.indexOf(spacer, spacerPos1 + 1);

		if (spacerPos2 == -1) {
			return new GeoPoint(
					(int) (Double.parseDouble(s.substring(spacerPos1 + 1,
							s.length())) * 1E6),
					(int) (Double.parseDouble(s.substring(0, spacerPos1)) * 1E6));
		} else {
			return new GeoPoint(
					(int) (Double.parseDouble(s.substring(spacerPos1 + 1,
							spacerPos2)) * 1E6),
					(int) (Double.parseDouble(s.substring(0, spacerPos1)) * 1E6),
					(int) Double.parseDouble(s.substring(spacerPos2 + 1,
							s.length())));

		}
	}

	private int mLongitudeE6;

	private int mLatitudeE6;

	private int mAltitude;

	public GeoPoint(final double aLatitude, final double aLongitude) {
		mLatitudeE6 = (int) (aLatitude * 1E6);
		mLongitudeE6 = (int) (aLongitude * 1E6);
	}

	public GeoPoint(final double aLatitude, final double aLongitude,
			final double aAltitude) {
		mLatitudeE6 = (int) (aLatitude * 1E6);
		mLongitudeE6 = (int) (aLongitude * 1E6);
		mAltitude = (int) aAltitude;
	}

	public GeoPoint(final GeoPoint aGeopoint) {
		mLatitudeE6 = aGeopoint.mLatitudeE6;
		mLongitudeE6 = aGeopoint.mLongitudeE6;
		mAltitude = aGeopoint.mAltitude;
	}

	public GeoPoint(final int aLatitudeE6, final int aLongitudeE6) {
		mLatitudeE6 = aLatitudeE6;
		mLongitudeE6 = aLongitudeE6;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public GeoPoint(final int aLatitudeE6, final int aLongitudeE6,
			final int aAltitude) {
		mLatitudeE6 = aLatitudeE6;
		mLongitudeE6 = aLongitudeE6;
		mAltitude = aAltitude;
	}

	public GeoPoint(final Location aLocation) {
		this(aLocation.getLatitude(), aLocation.getLongitude(), aLocation
				.getAltitude());
	}

	// ===========================================================
	// Parcelable
	// ===========================================================
	private GeoPoint(final Parcel in) {
		mLatitudeE6 = in.readInt();
		mLongitudeE6 = in.readInt();
		mAltitude = in.readInt();
	}

	/**
	 * @see Source@
	 *      http://groups.google.com/group/osmdroid/browse_thread/thread/
	 *      d22c4efeb9188fe9/ bc7f9b3111158dd
	 * @return bearing in degrees
	 */
	public double bearingTo(final IGeoPoint other) {
		final double lat1 = Math.toRadians(mLatitudeE6 / 1E6);
		final double long1 = Math.toRadians(mLongitudeE6 / 1E6);
		final double lat2 = Math.toRadians(other.getLatitudeE6() / 1E6);
		final double long2 = Math.toRadians(other.getLongitudeE6() / 1E6);
		final double delta_long = long2 - long1;
		final double a = Math.sin(delta_long) * Math.cos(lat2);
		final double b = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
				* Math.cos(lat2) * Math.cos(delta_long);
		final double bearing = Math.toDegrees(Math.atan2(a, b));
		final double bearing_normalized = (bearing + 360) % 360;
		return bearing_normalized;
	}

	@Override
	public Object clone() {
		return new GeoPoint(mLatitudeE6, mLongitudeE6);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Calculate a point that is the specified distance and bearing away from
	 * this point.
	 * 
	 * @see Source@ http://www.movable-type.co.uk/scripts/latlong.html
	 * @see Source@ http://www.movable-type.co.uk/scripts/latlon.js
	 */
	public GeoPoint destinationPoint(final double aDistanceInMeters,
			final float aBearingInDegrees) {

		// convert distance to angular distance
		final double dist = aDistanceInMeters / RADIUS_EARTH_METERS;

		// convert bearing to radians
		final float brng = DEG2RAD * aBearingInDegrees;

		// get current location in radians
		final double lat1 = DEG2RAD * getLatitudeE6() / 1E6;
		final double lon1 = DEG2RAD * getLongitudeE6() / 1E6;

		final double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist)
				+ Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));
		final double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat1),
						Math.cos(dist) - Math.sin(lat1) * Math.sin(lat2));

		final double lat2deg = lat2 / DEG2RAD;
		final double lon2deg = lon2 / DEG2RAD;

		return new GeoPoint(lat2deg, lon2deg);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	/**
	 * @see Source@ http://www.geocities.com/DrChengalva/GPSDistance.html
	 * @return distance in meters
	 */
	public int distanceTo(final IGeoPoint other) {

		final double a1 = DEG2RAD * mLatitudeE6 / 1E6;
		final double a2 = DEG2RAD * mLongitudeE6 / 1E6;
		final double b1 = DEG2RAD * other.getLatitudeE6() / 1E6;
		final double b2 = DEG2RAD * other.getLongitudeE6() / 1E6;

		final double cosa1 = Math.cos(a1);
		final double cosb1 = Math.cos(b1);

		final double t1 = cosa1 * Math.cos(a2) * cosb1 * Math.cos(b2);

		final double t2 = cosa1 * Math.sin(a2) * cosb1 * Math.sin(b2);

		final double t3 = Math.sin(a1) * Math.sin(b1);

		final double tt = Math.acos(t1 + t2 + t3);

		return (int) (RADIUS_EARTH_METERS * tt);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		final GeoPoint rhs = (GeoPoint) obj;
		return rhs.mLatitudeE6 == mLatitudeE6
				&& rhs.mLongitudeE6 == mLongitudeE6
				&& rhs.mAltitude == mAltitude;
	}

	public int getAltitude() {
		return mAltitude;
	}

	@Override
	public int getLatitudeE6() {
		return mLatitudeE6;
	}

	@Override
	public int getLongitudeE6() {
		return mLongitudeE6;
	}

	@Override
	public int hashCode() {
		return 37 * (17 * mLatitudeE6 + mLongitudeE6) + mAltitude;
	}

	public void setAltitude(final int aAltitude) {
		mAltitude = aAltitude;
	}

	public void setCoordsE6(final int aLatitudeE6, final int aLongitudeE6) {
		mLatitudeE6 = aLatitudeE6;
		mLongitudeE6 = aLongitudeE6;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void setLatitudeE6(final int aLatitudeE6) {
		mLatitudeE6 = aLatitudeE6;
	}

	public void setLongitudeE6(final int aLongitudeE6) {
		mLongitudeE6 = aLongitudeE6;
	}

	public String toDoubleString() {
		return new StringBuilder().append(mLatitudeE6 / 1E6).append(",")
				.append(mLongitudeE6 / 1E6).append(",").append(mAltitude)
				.toString();
	}

	public String toInvertedDoubleString() {
		return new StringBuilder().append(mLongitudeE6 / 1E6).append(",")
				.append(mLatitudeE6 / 1E6).append(",").append(mAltitude)
				.toString();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(mLatitudeE6).append(",")
				.append(mLongitudeE6).append(",").append(mAltitude).toString();
	}

	@Override
	public void writeToParcel(final Parcel out, final int flags) {
		out.writeInt(mLatitudeE6);
		out.writeInt(mLongitudeE6);
		out.writeInt(mAltitude);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
