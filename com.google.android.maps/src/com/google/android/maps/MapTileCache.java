// Created by plusminus on 17:58:57 - 25.09.2008
package com.google.android.maps;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.graphics.drawable.Drawable;

/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public final class MapTileCache implements OpenStreetMapTileProviderConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected LRUMapTileCache mCachedTiles;

	private final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

	// ===========================================================
	// Constructors
	// ===========================================================

	public MapTileCache() {
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}

	/**
	 * @param aMaximumCacheSize
	 *            Maximum amount of MapTiles to be hold within.
	 */
	public MapTileCache(final int aMaximumCacheSize) {
		mCachedTiles = new LRUMapTileCache(aMaximumCacheSize);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void clear() {
		mReadWriteLock.writeLock().lock();
		try {
			mCachedTiles.clear();
		} finally {
			mReadWriteLock.writeLock().unlock();
		}
	}

	public boolean containsTile(final MapTile aTile) {
		mReadWriteLock.readLock().lock();
		try {
			return mCachedTiles.containsKey(aTile);
		} finally {
			mReadWriteLock.readLock().unlock();
		}
	}

	public void ensureCapacity(final int aCapacity) {
		mReadWriteLock.readLock().lock();
		try {
			mCachedTiles.ensureCapacity(aCapacity);
		} finally {
			mReadWriteLock.readLock().unlock();
		}
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public Drawable getMapTile(final MapTile aTile) {
		mReadWriteLock.readLock().lock();
		try {
			return mCachedTiles.get(aTile);
		} finally {
			mReadWriteLock.readLock().unlock();
		}
	}

	public void putTile(final MapTile aTile, final Drawable aDrawable) {
		if (aDrawable != null) {
			mReadWriteLock.writeLock().lock();
			try {
				mCachedTiles.put(aTile, aDrawable);
			} finally {
				mReadWriteLock.writeLock().unlock();
			}
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
