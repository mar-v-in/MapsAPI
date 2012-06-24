package com.google.android.maps;

import java.io.InputStream;

public interface IArchiveFile {

	/**
	 * Get the input stream for the requested tile.
	 * 
	 * @return the input stream, or null if the archive doesn't contain an entry
	 *         for the requested tile
	 */
	InputStream getInputStream(ITileSource tileSource, MapTile tile);

}
