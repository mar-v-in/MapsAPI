package com.google.android.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class GEMFFileArchive implements IArchiveFile {

	public static GEMFFileArchive getGEMFFileArchive(final File pFile)
			throws FileNotFoundException, IOException {
		return new GEMFFileArchive(pFile);
	}

	private final GEMFFile mFile;

	private GEMFFileArchive(final File pFile) throws FileNotFoundException,
			IOException {
		mFile = new GEMFFile(pFile);
	}

	@Override
	public InputStream getInputStream(final ITileSource pTileSource,
			final MapTile pTile) {
		return mFile.getInputStream(pTile.getX(), pTile.getY(),
				pTile.getZoomLevel());
	}

	@Override
	public String toString() {
		return "GEMFFileArchive [mGEMFFile=" + mFile.getName() + "]";
	}

}
