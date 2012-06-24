package com.google.android.maps;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.ResourceProxy.string;

public abstract class BitmapTileSourceBase implements ITileSource,
		OpenStreetMapTileProviderConstants {

	public final class LowMemoryException extends Exception {
		private static final long serialVersionUID = 146526524087765134L;

		public LowMemoryException(final String pDetailMessage) {
			super(pDetailMessage);
		}

		public LowMemoryException(final Throwable pThrowable) {
			super(pThrowable);
		}
	}


	private static int globalOrdinal = 0;
	private final int mMinimumZoomLevel;

	private final int mMaximumZoomLevel;
	private final int mOrdinal;
	protected final String mName;
	protected final String mImageFilenameEnding;

	protected final Random random = new Random();

	private final int mTileSizePixels;

	private final string mResourceId;

	public BitmapTileSourceBase(final String aName, final string aResourceId,
			final int aZoomMinLevel, final int aZoomMaxLevel,
			final int aTileSizePixels, final String aImageFilenameEnding) {
		mResourceId = aResourceId;
		mOrdinal = globalOrdinal++;
		mName = aName;
		mMinimumZoomLevel = aZoomMinLevel;
		mMaximumZoomLevel = aZoomMaxLevel;
		mTileSizePixels = aTileSizePixels;
		mImageFilenameEnding = aImageFilenameEnding;
	}

	@Override
	public Drawable getDrawable(final InputStream aFileInputStream)
			throws LowMemoryException {
		try {
			// default implementation will load the file as a bitmap and create
			// a BitmapDrawable from it
			final Bitmap bitmap = BitmapFactory.decodeStream(aFileInputStream);
			if (bitmap != null) {
				return new ExpirableBitmapDrawable(bitmap);
			}
		} catch (final OutOfMemoryError e) {
			Log.e("MapsAPI", "BitmapsTileSourceBase: OutOfMemoryError loading bitmap");
			System.gc();
			throw new LowMemoryException(e);
		}
		return null;
	}

	@Override
	public Drawable getDrawable(final String aFilePath) {
		try {
			// default implementation will load the file as a bitmap and create
			// a BitmapDrawable from it
			final Bitmap bitmap = BitmapFactory.decodeFile(aFilePath);
			if (bitmap != null) {
				return new ExpirableBitmapDrawable(bitmap);
			} else {
				// if we couldn't load it then it's invalid - delete it
				try {
					new File(aFilePath).delete();
				} catch (final Throwable e) {
					Log.e("MapsAPI", "BitmapsTileSourceBase: Error deleting invalid file: " + aFilePath, e);
				}
			}
		} catch (final OutOfMemoryError e) {
			Log.e("MapsAPI", "BitmapsTileSourceBase: OutOfMemoryError loading bitmap: " + aFilePath);
			System.gc();
		}
		return null;
	}

	@Override
	public int getMaximumZoomLevel() {
		return mMaximumZoomLevel;
	}

	@Override
	public int getMinimumZoomLevel() {
		return mMinimumZoomLevel;
	}

	@Override
	public String getTileRelativeFilenameString(final MapTile tile) {
		final StringBuilder sb = new StringBuilder();
		sb.append(pathBase());
		sb.append('/');
		sb.append(tile.getZoomLevel());
		sb.append('/');
		sb.append(tile.getX());
		sb.append('/');
		sb.append(tile.getY());
		sb.append(imageFilenameEnding());
		return sb.toString();
	}

	@Override
	public int getTileSizePixels() {
		return mTileSizePixels;
	}

	public String imageFilenameEnding() {
		return mImageFilenameEnding;
	}

	@Override
	public String localizedName(final ResourceProxy proxy) {
		return proxy.getString(mResourceId);
	}

	@Override
	public String name() {
		return mName;
	}

	@Override
	public int ordinal() {
		return mOrdinal;
	}

	public String pathBase() {
		return mName;
	}
}
