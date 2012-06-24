package com.google.android.maps;

import com.google.android.maps.ResourceProxy.string;

public abstract class OnlineTileSourceBase extends BitmapTileSourceBase {

	private final String mBaseUrls[];

	public OnlineTileSourceBase(final String aName, final string aResourceId,
			final int aZoomMinLevel, final int aZoomMaxLevel,
			final int aTileSizePixels, final String aImageFilenameEnding,
			final String... aBaseUrl) {
		super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel,
				aTileSizePixels, aImageFilenameEnding);
		mBaseUrls = aBaseUrl;
	}

	/**
	 * Get the base url, which will be a random one if there are more than one.
	 */
	protected String getBaseUrl() {
		return mBaseUrls[random.nextInt(mBaseUrls.length)];
	}

	public abstract String getTileURLString(MapTile aTile);
}
