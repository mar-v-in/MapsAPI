package com.google.android.maps;

/**
 * An interface that resembles the Google Maps API MapView class and is
 * implemented by the osmdroid {@link MapView} class.
 * 
 * @author Neil Boyd
 * 
 */
public interface IMapView {

	MapController getController();

	int getLatitudeSpan();

	int getLongitudeSpan();

	GeoPoint getMapCenter();

	int getMaxZoomLevel();

	Projection getProjection();

	int getZoomLevel();

	// some methods from View
	// (well, just one for now)
	void setBackgroundColor(int color);

}
