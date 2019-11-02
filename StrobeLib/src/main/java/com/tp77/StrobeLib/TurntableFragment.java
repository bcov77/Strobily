package com.tp77.StrobeLib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TurntableFragment extends MyFragment {
	
	private static final int S_START = 0;
	private static final int S_50 = 1;
	private static final int S_60 = 2;
	
	
	private SharedPreferences mPrefs;
	private View mRoot = null;
	

	private int mState = S_START;
	private int[] mResults = null;
	
	private boolean mKill = false;
	private boolean mCalibrating = false;

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
		
		final View v = inflater.inflate(R.layout.frag_turntable_fragment, null);
		mRoot = v;
		
		
		mRoot.findViewById(R.id._50_hz_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mActivity.serviceReady(true))
					return;
				if (mCalibrating)
					return;
				mCalibrating = true;
				calibrate(S_50);
			}
		});

		mRoot.findViewById(R.id._60_hz_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mActivity.serviceReady(true))
					return;
				if (mCalibrating)
					return;
				mCalibrating = true;
				calibrate(S_60);
			}
		});
		
		mRoot.findViewById(R.id.stop_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mKill = true;
			}
		});

		mRoot.findViewById(R.id.up).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doState(S_START);
			}
		});
		

		v.findViewById(R.id.running_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mActivity.serviceReady(true))
					return;
				if (mActivity.mService.mFlashing)
					mActivity.stopFlashing();
				else {
					float freq = mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
					setFrequency(freq);
				}
			}
		});

		
		return mRoot;
		
	}
	
	

	private static final int G_GOOD = 0;
	private static final int G_OK = 1;
	private static final int G_BAD = 2;
	
	private void calibrate(final int mode) {
		if (mActivity.mService == null)
			return;
		if (!mActivity.mService.isFlashReady()) {
			Toast.makeText(mActivity, "Sorry, you\'ll need the LED for this", Toast.LENGTH_LONG).show();
		}
		
		mRoot.findViewById(R.id.calibrating).setVisibility(View.VISIBLE);
		mRoot.findViewById(R.id.stop_button).setVisibility(View.VISIBLE);
		
		mActivity.handleLedScreenPress(false, true);
		
		Editor e = mPrefs.edit();
		e.putInt(AdvancedFragment.P_LATE_THRESHOLD, -1);
		MainActivity.apply(e);
		
		final float startFreq = mode == S_50 ? 50f : 60f;
		
		(new Thread() {
			@Override
			public void run() {
				setPriority(MAX_PRIORITY -1);
				
				int[] results = new int[] {G_GOOD, G_GOOD, G_GOOD, G_GOOD, G_GOOD};
				
				mActivity.startFlashing(new int[] {(int) (1000000f/startFreq/2), (int) (1000000f/startFreq/2)},
						true, MainActivity.S_OTHER);
				
				int divider = 1;
				mKill = false;
				

				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				while (mActivity.mService != null && divider < 5 && !mKill) {
					float useFreq = startFreq / divider;
					setFrequency(useFreq);
					try {
						sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (mActivity.mService == null)
						break;
				
					int lags = mActivity.mService.doCheckLag2();
					
					int result = G_GOOD;
					if (lags == 0) {
						break;
					}
					if (lags < useFreq / 5)
						result = G_OK;
					else 
						result = G_BAD;
					
					results[divider-1] = result;
					divider++;
				}
				
				
				mActivity.stopFlashing();
				

				Editor e = mPrefs.edit();
				e.putInt(AdvancedFragment.P_LATE_THRESHOLD, -1);
				MainActivity.apply(e);

				mCalibrating = false;
				
				if (mKill) {
					mKill = false;
					if (mActivity != null) {
						mActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								doState(S_START);
							}
						});
					}
					return;
				}
				
				mResults = results;
				if (mActivity != null) {
					mActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							doState(mode);
						}
						
					});
				}
				
			}
		}).start();
		
	}
	
	private static final int[] BUTTONS = {
		R.id.button0,
		R.id.button1,
		R.id.button2,
		R.id.button3,
		R.id.button4
	};
	private static final int[] COLORS = {
		0x4C, 0xAF, 0x50,
		0xFB, 0xC0, 0x2D,
		0xF4, 0x43, 0x36,
		
	};
	
	private void doState(int state) {
		mState = state;
		mKill = false;
		
		mRoot.findViewById(R.id.state0).setVisibility(state == S_START ? View.VISIBLE : View.GONE);
		mRoot.findViewById(R.id.state1).setVisibility(state != S_START ? View.VISIBLE : View.GONE);
		mRoot.findViewById(R.id.calibrating).setVisibility(View.GONE);
		mRoot.findViewById(R.id.stop_button).setVisibility(View.GONE);
		
		float start = state == S_50 ? 50f : 60f;
		
		
		for (int iii = 0; iii < 5; iii++) {
			final float freq =  start / (iii + 1);
			String str;
			if(Math.round(freq) == Math.round(freq*10)/10f) {
				str = Integer.toString((int) Math.round(freq)) + " Hz";
			} else {
				str = String.format("%.1f Hz", Math.round(freq*10)/10f);
			}
			
			Button b = (Button)mRoot.findViewById(BUTTONS[iii]);
			b.setText(str);
			if (mResults != null) {
				b.setBackgroundColor(Color.argb(255, COLORS[mResults[iii]*3+0], 
						COLORS[mResults[iii]*3+1], COLORS[mResults[iii]*3+2]));
			}
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Editor e = mPrefs.edit();
					e.putFloat(StrobeFragment.P_FREQUENCY, freq);
					MainActivity.apply(e);
					setFrequency(freq);
					updateFreqText();
				}
			});
			
		}
		
		updateFreqText();
		
		switch (mState) {
		case S_START:
			break;
		case S_50:
			break;
		case S_60:
			break;
		}
		
		
	}
	
	private void updateFreqText() {
		float freq = mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
		((TextView)mRoot.findViewById(R.id.frequency_text)).setText(String.format("%.3f Hz", freq));
	}

	@Override
	void updateSliders(boolean fromMain) {
		
		doState(mState);
	}
	

	private void setFrequency(float frequency) {

		int[] flashes = new int[] {0, (int) (1000000/frequency)};
		
		mActivity.startFlashing(flashes, true, MainActivity.S_OTHER);
	}
	

}
