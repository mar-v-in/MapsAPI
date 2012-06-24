package com.google.android.maps;

public interface INetworkAvailablityCheck {

	boolean getCellularDataNetworkAvailable();

	boolean getNetworkAvailable();

	boolean getRouteToPathExists(int hostAddress);

	boolean getWiFiNetworkAvailable();
}
