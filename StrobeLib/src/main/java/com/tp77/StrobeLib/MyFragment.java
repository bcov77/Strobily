package com.tp77.StrobeLib;

import android.support.v42.app.Fragment;

public abstract class MyFragment extends Fragment {

	public MainActivity mActivity;
	private boolean mStartHandlerRequested = false;
	
	abstract void updateSliders(boolean fromMain);
	
	public void onVolumeUp() {
		mActivity.volumeUp();
	}
	public void onVolumeDown() {
		mActivity.volumeDown();
	}
	
	public void requestStartHandlers() {
		mStartHandlerRequested = true;
	}
	
	
	public void onServiceConnected() {
		
	}
	
	@Override
	public void setMenuVisibility(boolean visible) {
		super.setMenuVisibility(visible);
		
		if (visible && mActivity != null)
			updateSliders(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateSliders(true);
		if (mStartHandlerRequested) {
			mStartHandlerRequested = false;
			startHandlers();
		}
	}
	
	public void startHandlers() {
		
	}
	
	public void stopHandlers() {
		
	}
	
	
}
