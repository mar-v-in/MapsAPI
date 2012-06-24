package com.google.android.maps;

import java.io.File;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.BitmapTileSourceBase.LowMemoryException;

/**
 * Implements a file system cache and provides cached tiles. This functions as a
 * tile provider by serving cached tiles for the supplied tile source.
 * 
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * 
 */
public class MapTileFilesystemProvider extends MapTileFileStorageProviderBase {

	// ===========================================================
	// Constants
	// ===========================================================

	private class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState pState)
				throws CantContinueException {

			if (mTileSource == null) {
				return null;
			}

			final MapTile tile = pState.getMapTile();

			// if there's no sdcard then don't do anything
			if (!getSdCardAvailable()) {
				if (DEBUGMODE) {
					Log.d("MapsAPI", "No sdcard - do nothing for tile: " + tile);
				}
				return null;
			}

			// Check the tile source to see if its file is available and if so,
			// then render the
			// drawable and return the tile
			final File file = new File(TILE_PATH_BASE,
					mTileSource.getTileRelativeFilenameString(tile)
							+ TILE_PATH_EXTENSION);
			if (file.exists()) {

				try {
					final Drawable drawable = mTileSource.getDrawable(file
							.getPath());

					// Check to see if file has expired
					final long now = System.currentTimeMillis();
					final long lastModified = file.lastModified();
					final boolean fileExpired = lastModified < now
							- mMaximumCachedFileAge;

					if (fileExpired) {
						if (DEBUGMODE) {
							Log.d("MapsAPI", "MapTileFilesystemProvider: Tile expired: " + tile);
						}
						drawable.setState(new int[] { ExpirableBitmapDrawable.EXPIRED });
					}

					return drawable;
				} catch (final LowMemoryException e) {
					// low memory so empty the queue
					Log.w("MapsAPI", "MapTileFilesystemProvider: LowMemoryException downloading MapTile: "
							+ tile + " : " + e);
					throw new CantContinueException(e);
				}
			}

			// If we get here then there is no file in the file cache
			return null;
		}
	}

	// ===========================================================
	// Fields
	// ===========================================================

	private final long mMaximumCachedFileAge;

	// ===========================================================
	// Constructors
	// ===========================================================

	private ITileSource mTileSource;

	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver) {
		this(pRegisterReceiver, TileSourceFactory.DEFAULT_TILE_SOURCE);
	}

	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver,
			final ITileSource aTileSource) {
		this(pRegisterReceiver, aTileSource, DEFAULT_MAXIMUM_CACHED_FILE_AGE);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	/**
	 * Provides a file system based cache tile provider. Other providers can
	 * register and store data in the cache.
	 * 
	 * @param pRegisterReceiver
	 */
	public MapTileFilesystemProvider(final IRegisterReceiver pRegisterReceiver,
			final ITileSource pTileSource, final long pMaximumCachedFileAge) {
		super(pRegisterReceiver, NUMBER_OF_TILE_FILESYSTEM_THREADS,
				TILE_FILESYSTEM_MAXIMUM_QUEUE_SIZE);
		mTileSource = pTileSource;

		mMaximumCachedFileAge = pMaximumCachedFileAge;
	}

	@Override
	public int getMaximumZoomLevel() {
		return mTileSource != null ? mTileSource.getMaximumZoomLevel()
				: MAXIMUM_ZOOMLEVEL;
	}

	@Override
	public int getMinimumZoomLevel() {
		return mTileSource != null ? mTileSource.getMinimumZoomLevel()
				: MINIMUM_ZOOMLEVEL;
	}

	@Override
	protected String getName() {
		return "File System Cache Provider";
	};

	@Override
	protected String getThreadGroupName() {
		return "filesystem";
	}

	@Override
	protected Runnable getTileLoader() {
		return new TileLoader();
	}

	@Override
	public boolean getUsesDataConnection() {
		return false;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	@Override
	public void setTileSource(final ITileSource pTileSource) {
		mTileSource = pTileSource;
	}
}
