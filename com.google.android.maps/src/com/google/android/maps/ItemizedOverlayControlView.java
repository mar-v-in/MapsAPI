// Created by plusminus on 22:59:38 - 12.09.2008
package com.google.android.maps;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class ItemizedOverlayControlView extends LinearLayout {

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	public interface ItemizedOverlayControlViewListener {
		public void onCenter();

		public void onNavTo();

		public void onNext();

		public void onPrevious();
	}

	protected ImageButton mPreviousButton;
	protected ImageButton mNextButton;
	protected ImageButton mCenterToButton;

	protected ImageButton mNavToButton;

	// ===========================================================
	// Constructors
	// ===========================================================

	protected ItemizedOverlayControlViewListener mLis;

	public ItemizedOverlayControlView(final Context context,
			final AttributeSet attrs) {
		this(context, attrs, new DefaultResourceProxyImpl(context));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public ItemizedOverlayControlView(final Context context,
			final AttributeSet attrs, final ResourceProxy pResourceProxy) {
		super(context, attrs);

		mPreviousButton = new ImageButton(context);
		mPreviousButton.setImageBitmap(pResourceProxy
				.getBitmap(ResourceProxy.bitmap.previous));

		mNextButton = new ImageButton(context);
		mNextButton.setImageBitmap(pResourceProxy
				.getBitmap(ResourceProxy.bitmap.next));

		mCenterToButton = new ImageButton(context);
		mCenterToButton.setImageBitmap(pResourceProxy
				.getBitmap(ResourceProxy.bitmap.center));

		mNavToButton = new ImageButton(context);
		mNavToButton.setImageBitmap(pResourceProxy
				.getBitmap(ResourceProxy.bitmap.navto_small));

		this.addView(mPreviousButton, new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		this.addView(mCenterToButton, new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		this.addView(mNavToButton, new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		this.addView(mNextButton, new LayoutParams(
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

		initViewListeners();
	}

	private void initViewListeners() {
		mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mLis != null) {
					mLis.onNext();
				}
			}
		});

		mPreviousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mLis != null) {
					mLis.onPrevious();
				}
			}
		});

		mCenterToButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mLis != null) {
					mLis.onCenter();
				}
			}
		});

		mNavToButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (mLis != null) {
					mLis.onNavTo();
				}
			}
		});
	}

	public void setItemizedOverlayControlViewListener(
			final ItemizedOverlayControlViewListener lis) {
		mLis = lis;
	}

	public void setNavToVisible(final int pVisibility) {
		mNavToButton.setVisibility(pVisibility);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void setNextEnabled(final boolean pEnabled) {
		mNextButton.setEnabled(pEnabled);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public void setPreviousEnabled(final boolean pEnabled) {
		mPreviousButton.setEnabled(pEnabled);
	}
}
