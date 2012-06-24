package com.google.android.maps;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * A {@link BitmapDrawable} for a {@link MapTile} that has a state to indicate
 * that it's expired.
 */
public class ExpirableBitmapDrawable extends BitmapDrawable {

	public static final int EXPIRED = -1;

	public static boolean isDrawableExpired(final Drawable pTile) {
		if (!pTile.isStateful()) {
			return false;
		}
		final int[] state = pTile.getState();
		for (final int element : state) {
			if (element == EXPIRED) {
				return true;
			}
		}
		return false;
	}

	private int[] mState;

	public ExpirableBitmapDrawable(final Bitmap pBitmap) {
		super(pBitmap);
		mState = new int[0];
	}

	@Override
	public int[] getState() {
		return mState;
	}

	@Override
	public boolean isStateful() {
		return mState.length > 0;
	}

	@Override
	public boolean setState(final int[] pStateSet) {
		mState = pStateSet;
		return true;
	}

}
