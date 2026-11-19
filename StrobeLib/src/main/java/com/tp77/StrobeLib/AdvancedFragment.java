package com.tp77.StrobeLib;

import java.lang.reflect.Field;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ScrollView;
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
    public static final String P_SHOW_AUX = "showAux";
    public static final String P_AUX_HIGH = "auxHigh";
    public static final String P_AUX_LOW = "auxLow";
    public static final String P_AUX_STEREO = "auxStereo";
    public static final String P_AUX_MAX_VOLUME = "auxMaxVolume";
    public static final String P_AUX_VOLUME_KEYS = "auxVolumeKeys";
    public static final String P_AUX_HEADPHONES = "auxHeadphones";
	
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
		
        v.findViewById(R.id.aux_mode_check).setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                SettingsFragment.togglePref(P_SHOW_AUX, false, mActivity);
                mActivity.updateAuxMode();
                updateSliders(false);
            }
        });

        View auxHelpButton = v.findViewById(R.id.aux_help_button);
        auxHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                (new AuxHelpDialogFragment()).show(mActivity.getSupportFragmentManager(), "");
            }
        });
        auxHelpButton.setVisibility(SettingsFragment.helpVisibility(mActivity));

        setupAuxCheckbox(R.id.aux_max_volume_check, P_AUX_MAX_VOLUME, true);
        setupAuxCheckbox(R.id.aux_volume_keys_check, P_AUX_VOLUME_KEYS, false);
        setupAuxCheckbox(R.id.aux_headphones_check, P_AUX_HEADPHONES, true);
        setupAuxSlider(R.id.aux_high_seek_bar, P_AUX_HIGH, 200, 200);
        setupAuxSlider(R.id.aux_low_seek_bar, P_AUX_LOW, 100, 200);
        setupAuxSlider(R.id.aux_stereo_seek_bar, P_AUX_STEREO, 1, 2);

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

	
	
	
	
	
	
	
	
	
	
	
	
	
    private void setupAuxCheckbox(int id, final String key, final boolean defaultValue) {
        mRoot.findViewById(id).setOnClickListener(view -> {
            SettingsFragment.togglePref(key, defaultValue, mActivity);
            updateSliders(false);
        });
    }

    // Voltage preferences use hundredths in [-100, 100]; stereo is left/center/right (0/1/2).
    private void setupAuxSlider(int id, final String key, int defaultProgress, int maximum) {
        SeekBar slider = (SeekBar)mRoot.findViewById(id);
        slider.setMax(maximum);
        slider.setProgress(mPrefs.getInt(key, defaultProgress - (maximum == 200 ? 100 : 0))
                + (maximum == 200 ? 100 : 0));
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                MainActivity.apply(mPrefs.edit().putInt(key,
                        progress - (bar.getMax() == 200 ? 100 : 0)));
                updateSliders(false);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
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
        int[] brightnessesSave = mActivity.mService.mBrightnesses;
        int whoSave = mActivity.mService.mWhoStartedIt;
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
			mActivity.startFlashing(flashesSave, brightnessesSave, loopSave, whoSave);
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
		
        boolean showAux = mPrefs.getBoolean(P_SHOW_AUX, false);
        ((CheckBox)mRoot.findViewById(R.id.aux_mode_check)).setChecked(showAux);
        mRoot.findViewById(R.id.aux_section).setVisibility(showAux ? View.VISIBLE : View.GONE);
        ((CheckBox)mRoot.findViewById(R.id.aux_max_volume_check)).setChecked(mPrefs.getBoolean(P_AUX_MAX_VOLUME, true));
        ((CheckBox)mRoot.findViewById(R.id.aux_volume_keys_check)).setChecked(mPrefs.getBoolean(P_AUX_VOLUME_KEYS, false));
        ((CheckBox)mRoot.findViewById(R.id.aux_headphones_check)).setChecked(mPrefs.getBoolean(P_AUX_HEADPHONES, true));
        int high = mPrefs.getInt(P_AUX_HIGH, 100);
        int low = mPrefs.getInt(P_AUX_LOW, 0);
        ((SeekBar)mRoot.findViewById(R.id.aux_high_seek_bar)).setProgress(high + 100);
        ((SeekBar)mRoot.findViewById(R.id.aux_low_seek_bar)).setProgress(low + 100);
        ((SeekBar)mRoot.findViewById(R.id.aux_stereo_seek_bar)).setProgress(mPrefs.getInt(P_AUX_STEREO, 1));
        ((TextView)mRoot.findViewById(R.id.aux_high_value)).setText(getString(R.string.aux_voltage_value, high / 100f));
        ((TextView)mRoot.findViewById(R.id.aux_low_value)).setText(getString(R.string.aux_voltage_value, low / 100f));

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

	public static class AuxHelpDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			ScrollView sv = (ScrollView)getActivity().getLayoutInflater().inflate(R.layout.dia_aux_help, null);
			builder.setTitle("AUX Mode")
				.setView(sv)
				.setPositiveButton("Ok", null);
			return builder.create();
		}
	}


}
