package com.google.android.maps;

import android.location.Location;
import android.os.Bundle;

/**
 * An interface that resembles the Google Maps API MyLocationOverlay class and
 * is implemented by the osmdroid {@link MyLocationOverlay} class.
 * 
 * @author Neil Boyd
 * 
 */
public interface IMyLocationOverlay {

	void disableCompass();

	void disableMyLocation();

	boolean enableCompass();

	boolean enableMyLocation();

	Location getLastFix();

	public float getOrientation();

	boolean isCompassEnabled();

	boolean isMyLocationEnabled();

	void onStatusChanged(String provider, int status, Bundle extras);

	boolean runOnFirstFix(Runnable runnable);

}
