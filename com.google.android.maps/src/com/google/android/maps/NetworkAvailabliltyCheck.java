package com.google.android.maps;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * A straightforward network check implementation. NOTE: Requires
 * android.permission.ACCESS_NETWORK_STATE and
 * android.permission.ACCESS_WIFI_STATE (?) and android.permission.INTERNET (?)
 * 
 * @author Marc Kurtz
 * 
 */

public class NetworkAvailabliltyCheck implements INetworkAvailablityCheck {

	private final ConnectivityManager mConnectionManager;

	public NetworkAvailabliltyCheck(final Context aContext) {
		mConnectionManager = (ConnectivityManager) aContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	@Override
	public boolean getCellularDataNetworkAvailable() {
		try {
			final NetworkInfo mobile = mConnectionManager
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			return mobile != null && mobile.isAvailable();
		} catch (final Exception e) {
		}
		return false;
	}

	@Override
	public boolean getNetworkAvailable() {
		try {
			final NetworkInfo networkInfo = mConnectionManager
					.getActiveNetworkInfo();
			return networkInfo != null && networkInfo.isAvailable();
		} catch (final Exception e) {
		}
		return true;
	}

	@Override
	public boolean getRouteToPathExists(final int hostAddress) {
		return (mConnectionManager.requestRouteToHost(
				ConnectivityManager.TYPE_WIFI, hostAddress) || mConnectionManager
				.requestRouteToHost(ConnectivityManager.TYPE_MOBILE,
						hostAddress));
	}

	@Override
	public boolean getWiFiNetworkAvailable() {
		try {
			final NetworkInfo wifi = mConnectionManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			return wifi != null && wifi.isAvailable();
		} catch (final Exception e) {
		}
		return false;
	}

}
