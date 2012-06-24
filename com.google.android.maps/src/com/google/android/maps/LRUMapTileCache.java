package com.google.android.maps;

import java.util.LinkedHashMap;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class LRUMapTileCache extends LinkedHashMap<MapTile, Drawable> implements
		OpenStreetMapTileProviderConstants {

	private static final long serialVersionUID = -541142277575493335L;

	private int mCapacity;

	public LRUMapTileCache(final int aCapacity) {
		super(aCapacity + 2, 0.1f, true);
		mCapacity = aCapacity;
	}

	@Override
	public void clear() {
		// remove them all individually so that they get recycled
		while (!isEmpty()) {
			remove(keySet().iterator().next());
		}

		// and then clear
		super.clear();
	}

	public void ensureCapacity(final int aCapacity) {
		if (aCapacity > mCapacity) {
			Log.i("MapsAPI", "LRUMapTileCache: Tile cache increased from " + mCapacity + " to "
					+ aCapacity);
			mCapacity = aCapacity;
		}
	}

	@Override
	public Drawable remove(final Object aKey) {
		final Drawable drawable = super.remove(aKey);
		if (drawable instanceof BitmapDrawable) {
			final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
		return drawable;
	}

	@Override
	protected boolean removeEldestEntry(
			final java.util.Map.Entry<MapTile, Drawable> aEldest) {
		if (size() > mCapacity) {
			final MapTile eldest = aEldest.getKey();
			if (DEBUGMODE) {
				Log.d("MapsAPI","LRUMapTileCache: Remove old tile: " + eldest);
			}
			remove(eldest);
			// don't return true because we've already removed it
		}
		return false;
	}
}
