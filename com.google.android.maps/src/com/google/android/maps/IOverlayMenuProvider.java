package com.google.android.maps;

import android.view.Menu;
import android.view.MenuItem;

public interface IOverlayMenuProvider {
	/**
	 * Can be used to signal to external callers that this Overlay should not be
	 * used for providing option menu items.
	 * 
	 */
	public boolean isOptionsMenuEnabled();

	public boolean onCreateOptionsMenu(final Menu pMenu,
			final int pMenuIdOffset, final MapView pMapView);

	public boolean onOptionsItemSelected(final MenuItem pItem,
			final int pMenuIdOffset, final MapView pMapView);

	public boolean onPrepareOptionsMenu(final Menu pMenu,
			final int pMenuIdOffset, final MapView pMapView);

	public void setOptionsMenuEnabled(final boolean pOptionsMenuEnabled);
}
