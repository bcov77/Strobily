package com.tp77.StrobeLib;

import java.util.Arrays;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class MusicFragment extends MyFragment {

	public static final String P_HAS_EXPIRED = "expired";

	private SharedPreferences mPrefs;
	
	private View mRoot = null;
	
	private long mHandlerNumber = System.currentTimeMillis();
	
	private Handler mBeater = null;
	private Handler mBpmUpdater = null;
	
	private int mNumberPressed = 0;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
	}
	
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
	}
	
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle bundle) {
		
		final View v = inflater.inflate(R.layout.frag_music_fragment, null);
		mRoot = v;
		
		
		v.findViewById(R.id.start).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ( ! mActivity.haveMicrophone() ) {
					return;
				}
				if (mActivity.serviceReady(true)) {
					if (!mActivity.mService.mMusicing) {
						mActivity.startMusicing();
					}
				}
			}
			
		});
		

		v.findViewById(R.id.stop).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.stopMusicing();
				if (mBpmUpdater != null) {
					mBpmUpdater.sendEmptyMessage(-1);
				}
					
			}
			
		});
		

		v.findViewById(R.id.buy_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paid_link)));
				mActivity.startActivity(i);
			}
		});
		
	
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		
		
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		mBeater = null;
		mHandlerNumber++;
	}
	
	@Override
	public void stopHandlers() {
		mBeater = null;
		mHandlerNumber++;
	}
	
	@Override
	public void startHandlers() {
		final View beat = mRoot.findViewById(R.id.beat);
		final long handlerNumber = mHandlerNumber;
		
		mBeater = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (handlerNumber == mHandlerNumber) {
					if (msg.what == 0) {
						beat.setVisibility(View.VISIBLE);
						this.sendEmptyMessage(1);
					} else {
						beat.setVisibility(View.INVISIBLE);
					}
				}
			}
		};
		
		

		final TextView bpm = (TextView)mRoot.findViewById(R.id.bpm);
		mBpmUpdater = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (handlerNumber == mHandlerNumber) {
					if (msg.what == -1 || msg.what == -2) {
						bpm.setText("--- bpm");
						
						if (msg.what == -2) {
							updateSliders(false);
						}
						
					} else if (msg.what != 0) {
						bpm.setText(Integer.toString(msg.what) + " bpm");
					}
				}
			}
		};
		
		mActivity.mBeater = mBeater;
		mActivity.mBpmUpdater = mBpmUpdater;
		mActivity.musicHandlerUpdate();
		
	}

	@Override
	void updateSliders(boolean fromMain) {

		if (fromMain) {
			
			mNumberPressed = 0;
			mActivity.override(false);

		}
		
		mRoot.findViewById(R.id.full_stuff).setVisibility(mPrefs.getBoolean(P_HAS_EXPIRED, false) ? View.VISIBLE : View.GONE);
		
		
		
		
		
	}
	
	
}
