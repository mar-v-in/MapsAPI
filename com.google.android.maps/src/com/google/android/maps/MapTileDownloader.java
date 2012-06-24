package com.google.android.maps;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.maps.BitmapTileSourceBase.LowMemoryException;

/**
 * The {@link MapTileDownloader} loads tiles from an HTTP server. It saves
 * downloaded tiles to an IFilesystemCache if available.
 * 
 * @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Manuel Stahl
 * 
 */
public class MapTileDownloader extends MapTileModuleProviderBase {

	// ===========================================================
	// Constants
	// ===========================================================

	private class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState aState)
				throws CantContinueException {
			try {

				if (mTileSource == null) {
					return null;
				}

				InputStream in = null;
				OutputStream out = null;
				final MapTile tile = aState.getMapTile();

				try {

					if (mNetworkAvailablityCheck != null
							&& !mNetworkAvailablityCheck.getNetworkAvailable()) {
						if (DEBUGMODE) {
							Log.d("MapsAPI", "MapTileDownloader: Skipping "
									+ getName()
									+ " due to NetworkAvailabliltyCheck.");
						}
						return null;
					}

					final String tileURLString = mTileSource
							.getTileURLString(tile);

					if (DEBUGMODE) {
						Log.d("MapsAPI",
								"MapTileDownloader: Downloading Maptile from url: "
										+ tileURLString);
					}

					if (TextUtils.isEmpty(tileURLString)) {
						return null;
					}

					final HttpClient client = new DefaultHttpClient();
					final HttpUriRequest head = new HttpGet(tileURLString);
					final HttpResponse response = client.execute(head);

					// Check to see if we got success
					final org.apache.http.StatusLine line = response
							.getStatusLine();
					if (line.getStatusCode() != 200) {
						Log.w("MapsAPI",
								"MapTileDownloader: Problem downloading MapTile: "
										+ tile + " HTTP response: " + line);
						return null;
					}

					final HttpEntity entity = response.getEntity();
					if (entity == null) {
						Log.w("MapsAPI",
								"MapTileDownloader: No content downloading MapTile: "
										+ tile);
						return null;
					}
					in = entity.getContent();

					final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
					out = new BufferedOutputStream(dataStream,
							StreamUtils.IO_BUFFER_SIZE);
					StreamUtils.copy(in, out);
					out.flush();
					final byte[] data = dataStream.toByteArray();
					final ByteArrayInputStream byteStream = new ByteArrayInputStream(
							data);

					final Drawable result = mTileSource.getDrawable(byteStream);

					// Save the data to the filesystem cache
					if (mFilesystemCache != null && result != null) {
						byteStream.reset();
						mFilesystemCache
								.saveFile(mTileSource, tile, byteStream);
					}

					return result;
				} catch (final UnknownHostException e) {
					// no network connection so empty the queue
					Log.w("MapsAPI",
							"MapTileDownloader: UnknownHostException downloading MapTile: "
									+ tile + " : " + e);
					throw new CantContinueException(e);
				} catch (final LowMemoryException e) {
					// low memory so empty the queue
					Log.w("MapsAPI",
							"MapTileDownloader: LowMemoryException downloading MapTile: "
									+ tile + " : " + e);
					throw new CantContinueException(e);
				} catch (final FileNotFoundException e) {
					Log.w("MapsAPI", "MapTileDownloader: Tile not found: "
							+ tile + " : " + e);
				} catch (final IOException e) {
					Log.w("MapsAPI",
							"MapTileDownloader: IOException downloading MapTile: "
									+ tile + " : " + e);
				} catch (final Throwable e) {
					Log.w("MapsAPI",
							"MapTileDownloader: Error downloading MapTile: "
									+ tile, e);
				} finally {
					StreamUtils.closeStream(in);
					StreamUtils.closeStream(out);
				}

				return null;

			} catch (final Exception ex) {
				Log.w("MapsTileDownloader", "loadTile", ex);
				return null;
			}
		}

		@Override
		protected void tileLoaded(final MapTileRequestState pState,
				final Drawable pDrawable) {
			removeTileFromQueues(pState.getMapTile());
			// don't return the tile because we'll wait for the fs provider to
			// ask for it
			// this prevent flickering when a load of delayed downloads complete
			// for tiles
			// that we might not even be interested in any more
			pState.getCallback().mapTileRequestCompleted(pState, null);
		}

	}

	// ===========================================================
	// Fields
	// ===========================================================

	private final IFilesystemCache mFilesystemCache;

	private OnlineTileSourceBase mTileSource;

	// ===========================================================
	// Constructors
	// ===========================================================

	private final INetworkAvailablityCheck mNetworkAvailablityCheck;

	public MapTileDownloader(final ITileSource pTileSource) {
		this(pTileSource, null, null);
	}

	public MapTileDownloader(final ITileSource pTileSource,
			final IFilesystemCache pFilesystemCache) {
		this(pTileSource, pFilesystemCache, null);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public MapTileDownloader(final ITileSource pTileSource,
			final IFilesystemCache pFilesystemCache,
			final INetworkAvailablityCheck pNetworkAvailablityCheck) {
		super(NUMBER_OF_TILE_DOWNLOAD_THREADS, TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE);

		mFilesystemCache = pFilesystemCache;
		mNetworkAvailablityCheck = pNetworkAvailablityCheck;
		setTileSource(pTileSource);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public int getMaximumZoomLevel() {
		return (mTileSource != null ? mTileSource.getMaximumZoomLevel()
				: MAXIMUM_ZOOMLEVEL);
	}

	@Override
	public int getMinimumZoomLevel() {
		return (mTileSource != null ? mTileSource.getMinimumZoomLevel()
				: MINIMUM_ZOOMLEVEL);
	}

	@Override
	protected String getName() {
		return "Online Tile Download Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return "downloader";
	};

	@Override
	protected Runnable getTileLoader() {
		return new TileLoader();
	}

	public ITileSource getTileSource() {
		return mTileSource;
	}

	@Override
	public boolean getUsesDataConnection() {
		return true;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	@Override
	public void setTileSource(final ITileSource tileSource) {
		// We are only interested in OnlineTileSourceBase tile sources
		if (tileSource instanceof OnlineTileSourceBase) {
			mTileSource = (OnlineTileSourceBase) tileSource;
		} else {
			// Otherwise shut down the tile downloader
			mTileSource = null;
		}
	}
}
