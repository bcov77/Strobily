package com.tp77.StrobeLib;

import java.lang.reflect.Field;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class AdvancedFragment extends MyFragment {
	

	private SharedPreferences mPrefs;
	
	private View mRoot = null;
	
	private static final String P_USE_RPM = "useRpm";
	private static final String P_USE_2X = "show2X";
	public static final String P_LATE_THRESHOLD = "lateThreshold";
	public static final String P_FLASH_PREEMPT = "flashPreempt";
	public static final String P_GENTLE = "gentle";
	public static final String P_PERFORM_A = "performA";
	public static final String P_DIAGNOSTIC = "diagnostic";
	public static final String P_REALTIME = "realtime";
	
	public static final String P_FULL_CPU = "fullCpu";

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
		
		final View v = inflater.inflate(R.layout.frag_advanced_fragment, null);
		mRoot = v;
		

		((CheckBox)v.findViewById(R.id.rpm_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsFragment.togglePref(P_USE_RPM, false, mActivity);
				
				updateSliders(false);
				mActivity.refreshFragments();
			}
		});
		
		((CheckBox)v.findViewById(R.id.x2_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsFragment.togglePref(P_USE_2X, false, mActivity);
				
				updateSliders(false);
				mActivity.refreshFragments();
			}
		});

		((CheckBox)v.findViewById(R.id.realtime_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsFragment.togglePref(P_REALTIME, false, mActivity);
				
				updateSliders(false);
			}
		});

		((CheckBox)v.findViewById(R.id.multi_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsFragment.togglePref(P_GENTLE, true, mActivity);
				
				updateSliders(false);
				if (mActivity.mService != null)
					mActivity.mService.settingsUpdate();
			}
		});
		
		((CheckBox)v.findViewById(R.id.performance_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				testPerformA(!usePerformA(mActivity));
				updateSliders(false);
			}
		});
		

		((CheckBox)v.findViewById(R.id.full_cpu_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				SettingsFragment.togglePref(P_FULL_CPU, false, mActivity);
				updateSliders(false);
				

				if (mActivity.mService != null)
					mActivity.mService.settingsUpdate();
				
			}
		});
		
		((CheckBox)v.findViewById(R.id.diagnostic_check)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsFragment.togglePref(P_DIAGNOSTIC, false, mActivity);
				updateSliders(false);
				mActivity.setupScroller(true);
			}
		});
		
		int progress;
		SeekBar sb;
		sb = (SeekBar)v.findViewById(R.id.late_flash_seek_bar);
		sb.setMax(51);
		
		progress = lateThreshold(mActivity);
		if (progress == -1)
			progress = 51;
		else
			progress = progress/5;
		
		sb.setProgress(progress);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				int micros = progress*5;
				if (progress == 51)
					micros = -1;
				
				Editor e = mPrefs.edit();
				e.putInt(P_LATE_THRESHOLD, micros);
				MainActivity.apply(e);
				
				updateSliders(false);
				if (mActivity.mService != null)
					mActivity.mService.settingsUpdate();
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		sb = (SeekBar)v.findViewById(R.id.flash_preempt_seek_bar);
		sb.setMax(100);
		sb.setProgress(flashPreempt(mActivity)/5);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				int micros = progress*5;
				
				Editor e = mPrefs.edit();
				e.putInt(P_FLASH_PREEMPT, micros);
				MainActivity.apply(e);
				
				updateSliders(false);
				if (mActivity.mService != null)
					mActivity.mService.settingsUpdate();
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		
		
		return v;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	private void testPerformA(boolean enabling) {
		if (!mActivity.serviceReady(true))
			return;
		
		Camera c = mActivity.mService.mCamera;
		if (c == null) {
			Toast.makeText(mActivity, "You need an LED for this", Toast.LENGTH_SHORT).show();
			return;
		}
		
		boolean actuallyEnabled = false;
		
		boolean torchSave = mActivity.mService.mTorch;;
		boolean flashingSave = mActivity.mService.mFlashing;
		int[] flashesSave = mActivity.mService.mFlashes;
		boolean loopSave = mActivity.mService.mLoop;
		
		mActivity.stopFlashing();
		mActivity.mService.stopTheStrober(false);
		
		if (enabling) {
			
			try {
				Camera.Parameters testParam = c.getParameters();
				testParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

				Field mMapField = null;
	        	mMapField = Camera.Parameters.class.getDeclaredField("mMap");
	        	mMapField.setAccessible(true);
	        	((HashMap)mMapField.get(testParam)).clear();
	        	testParam.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
	        	c.setParameters(testParam);							//this crashes on certain phones
	        	
	        	testParam = c.getParameters();
	    		if (testParam.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)) {
		
					((HashMap)mMapField.get(mActivity.mService.mParamOn)).clear();
					((HashMap)mMapField.get(mActivity.mService.mParamOff)).clear();
					((HashMap)mMapField.get(mActivity.mService.mParamOff2)).clear();
					
					Toast.makeText(mActivity, "Hack works", Toast.LENGTH_SHORT).show();
					actuallyEnabled = true;
	        	}
			} catch (Exception e) {
				Toast.makeText(mActivity, "This apparently doesn\'t work on your phone", Toast.LENGTH_SHORT).show();
			}
			
			mActivity.mService.mParamOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			mActivity.mService.mParamOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mActivity.mService.mParamOff2.setFlashMode(Camera.Parameters.FLASH_MODE_ON);	//ON is still off, just a little variation
	        
			try {
				c.setParameters(mActivity.mService.mParamOff);
			} catch (Exception e) {
				e.printStackTrace();
				c.release();
				mActivity.mService.mCamera = Camera.open();
				c = mActivity.mService.mCamera;
				try {
					c.setParameters(mActivity.mService.mParamOff2);
				} catch (Exception e1) {
					Toast.makeText(mActivity, "Fatal camera error. Restart your phone", Toast.LENGTH_LONG).show();
					try {
						c.stopPreview();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					c.release();
					mActivity.mService.mParamOn = null;
					mActivity.mService.mParamOff = null;
					mActivity.mService.mParamOff2 = null;
					mActivity.mService.mCamera = null;
					mActivity.handleLedScreenPress(true, true);
					return;
				}
			}
			
			
			
		} else {
			mActivity.mService.mParamOn = c.getParameters();
			mActivity.mService.mParamOff = c.getParameters();
			mActivity.mService.mParamOff2 = c.getParameters();
			
			mActivity.mService.mParamOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			mActivity.mService.mParamOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			mActivity.mService.mParamOff2.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			
		}
		
		mActivity.mService.startTheStrober();
		
		if (flashingSave) {
			mActivity.startFlashing(flashesSave, loopSave, mActivity.mService.mWhoStartedIt);
		}
		
		if (torchSave) {
			mActivity.torchOn();
		}
		
		Editor e = mPrefs.edit();
		e.putBoolean(P_PERFORM_A, actuallyEnabled);
		MainActivity.apply(e);
		
	}
	
	
	
	public void updateSliders(boolean fromMain) {
		if (mRoot == null)
			return;
		
		((CheckBox)mRoot.findViewById(R.id.rpm_check)).setChecked(useRPM(mActivity));
		((CheckBox)mRoot.findViewById(R.id.x2_check)).setChecked(use2X(mActivity));
		((CheckBox)mRoot.findViewById(R.id.diagnostic_check)).setChecked(mPrefs.getBoolean(P_DIAGNOSTIC, false));
		((CheckBox)mRoot.findViewById(R.id.realtime_check)).setChecked(mPrefs.getBoolean(P_REALTIME, false));
		
		((CheckBox)mRoot.findViewById(R.id.multi_check)).setChecked(!useGentle(mActivity));
		((CheckBox)mRoot.findViewById(R.id.performance_check)).setChecked(usePerformA(mActivity));

		mRoot.findViewById(R.id.performance_hack_stuff).setVisibility(
				mPrefs.getBoolean(MainActivity.P_NEW_CAMERA, false) ? View.GONE : View.VISIBLE );

		
		int lateFlash = lateThreshold(mActivity);
		String str = "0." + String.format("%03d", lateFlash) + " ms";
		if (lateFlash == -1)
			str = "None";
		
		((TextView)mRoot.findViewById(R.id.late_flash_text)).setText(str);
		((TextView)mRoot.findViewById(R.id.flash_preempt_text)).setText("0." + String.format("%03d", flashPreempt(mActivity)) + " ms");
		

		((CheckBox)mRoot.findViewById(R.id.full_cpu_check)).setChecked(mPrefs.getBoolean(AdvancedFragment.P_FULL_CPU, false));
		
	}
	
	
	public static boolean useRPM(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(P_USE_RPM, false);
	}

	public static boolean use2X(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(P_USE_2X, false);
	}

	public static boolean useGentle(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(P_GENTLE, true);
	}

	public static boolean usePerformA(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(P_PERFORM_A, false);
	}

	public static int lateThreshold(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(P_LATE_THRESHOLD, -1);
	}
	
	public static int flashPreempt(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(P_FLASH_PREEMPT, 150);
	}


}
