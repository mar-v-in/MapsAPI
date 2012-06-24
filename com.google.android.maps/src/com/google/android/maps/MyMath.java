// Created by plusminus on 20:36:01 - 26.09.2008
package com.google.android.maps;

import android.util.Log;

/**
 * 
 * @author Nicolas Gramlich
 * 
 */
public class MyMath implements MathConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * Calculates i.e. the increase of zoomlevel needed when the visible
	 * latitude needs to be bigger by <code>factor</code>.
	 * 
	 * Assert.assertEquals(1, getNextSquareNumberAbove(1.1f));
	 * Assert.assertEquals(2, getNextSquareNumberAbove(2.1f));
	 * Assert.assertEquals(2, getNextSquareNumberAbove(3.9f));
	 * Assert.assertEquals(3, getNextSquareNumberAbove(4.1f));
	 * Assert.assertEquals(3, getNextSquareNumberAbove(7.9f));
	 * Assert.assertEquals(4, getNextSquareNumberAbove(8.1f));
	 * Assert.assertEquals(5, getNextSquareNumberAbove(16.1f));
	 * 
	 * Assert.assertEquals(-1, - getNextSquareNumberAbove(1 / 0.4f) + 1);
	 * Assert.assertEquals(-2, - getNextSquareNumberAbove(1 / 0.24f) + 1);
	 * 
	 * @param factor
	 * @return
	 */
	public static int getNextSquareNumberAbove(final float factor) {
		try {
			Log.d("MapsMath", "getNextSquareNumberAbove(" + factor + ")");
			int out = 0;
			int cur = 1;
			int i = 1;
			while (true) {
				if (cur > factor) {
					return out;
				}

				if (cur > Short.MAX_VALUE) {
					throw new Exception("mmh?!");
				}

				out = i;
				cur *= 2;
				i++;
			}
		} catch (final Exception ex) {
			Log.v("MapsMath", "getNextSquareNumberAbove(" + factor + ")", ex);
			return 0;
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static double gudermann(final double y) {
		return RAD2DEG * Math.atan(Math.sinh(y));
	}

	public static double gudermannInverse(final double aLatitude) {
		return Math.log(Math.tan(PI_4 + (DEG2RAD * aLatitude / 2)));
	}

	public static int mod(int number, final int modulus) {
		if (number > 0) {
			return number % modulus;
		}

		while (number < 0) {
			number += modulus;
		}

		return number;
	}

	/**
	 * This is a utility class with only static members.
	 */
	private MyMath() {
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
