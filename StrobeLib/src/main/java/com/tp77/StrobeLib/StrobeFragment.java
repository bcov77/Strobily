package com.tp77.StrobeLib;

import com.tp77.StrobeLib.VerticalSeekbar.VerticalSeekbarListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class StrobeFragment extends MyFragment {

	
	
	
	private int mDuty = 50;
	private float mFrequency = 10.0f;
	
	private boolean mUseRpm;	// this won't change without refreshing the fragment
	
	private View mRoot = null;
	
	public static final String P_ON_LENGTH = "onLength";
	public static final String P_USE_SAVED = "useSaved";
	public static final String P_FREQUENCY = "frequency";
	public static final String P_DUTY = "duty";
	private static final String P_BURST = "burst";
	
	private SharedPreferences mPrefs = null;

	private long mHandlerNumber = System.currentTimeMillis();
	
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
		
		
		View v2 = null;
		final View v = inflater.inflate(R.layout.frag_strobe_fragment, null);
		mRoot = v;

		final View focusCatch = v.findViewById(R.id.focus_catch);
		final VerticalSeekbar vsb = (VerticalSeekbar)v.findViewById(R.id.vertical_seekbar);
		
		v.findViewById(R.id.running_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mRoot.findViewById(R.id.focus_catch).requestFocus();
				if (!mActivity.serviceReady(true))
					return;
				if (mActivity.mService.mFlashing)
					mActivity.stopFlashing();
				else {
					controlUpdate(true);
				}
			}
		});
		
		vsb.setListener(new VerticalSeekbarListener() {
			@Override
			public void onSeekbarChange(float progress) {
				mFrequency = progress;
				saveFreqDuty();
				
				controlUpdate(false);
				updateSliders(false);
			}
		});
		
		
		SeekBar dutySeek = (SeekBar)v.findViewById(R.id.duty_seek_bar);
		dutySeek.setMax(100);
		dutySeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mDuty = progress;
				saveFreqDuty();
				if (fromUser)
					controlUpdate(false);
				updateSliders(false);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		
		v2 = v.findViewById(R.id.save_button);
		v2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Editor e = mPrefs.edit();
				e.putInt(P_ON_LENGTH, calculateTime(true));
				MainActivity.apply(e);
				updateSliders(false);
			}
		});
		
		v2.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Editor e = mPrefs.edit();
				e.putInt(P_ON_LENGTH, 0);
				MainActivity.apply(e);
				updateSliders(false);
				return true;
			}
		});
		
		v.findViewById(R.id.steady_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Editor e = mPrefs.edit();
				e.putBoolean(P_USE_SAVED, !useSaved());
				MainActivity.apply(e);
				updateSliders(false);
			}
		});
		
		v.findViewById(R.id.back_layer).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				focusCatch.requestFocus();
			}
		});
		
		((EditText)v.findViewById(R.id.frequency_text)).setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					String text = ((EditText)v).getText().toString();
					try {
						mFrequency = Float.parseFloat(text);
						if (mUseRpm)
							mFrequency /= 60;

						if (mFrequency < 0.001)
							mFrequency = 0.001f;
						saveFreqDuty();
						((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mFrequency);
//						updateSliders(false);
//						controlUpdate(false);
					} catch (Exception e) {
						e.printStackTrace();
					}
					updateSliders(false);
					controlUpdate(false);

				} 
			}
		});

		((EditText)v.findViewById(R.id.saved_num)).setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					String text = ((EditText)v).getText().toString();
					try {
						int onLength = (int) (Float.parseFloat(text) * 1000);

						if (onLength < 0)
							onLength = 0;
						
						Editor e = mPrefs.edit();
						e.putInt(P_ON_LENGTH, onLength);
						MainActivity.apply(e);
						
						updateSliders(false);
						controlUpdate(false);
					} catch (Exception e) {
						e.printStackTrace();
					}

				} 
			}
		});
		
		
		v.findViewById(R.id.cur_on_length).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mRoot.findViewById(R.id.saved_num).requestFocus();
			}
		});
		
		v.findViewById(R.id.x2).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mFrequency *= 2;
				saveFreqDuty();
				((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mFrequency);
				updateSliders(false);
				controlUpdate(false);
			}
		});
		
		v.findViewById(R.id.x12).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mFrequency /= 2;
				if ( mFrequency < 0.001 ) mFrequency = 0.001f;
				saveFreqDuty();
				((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mFrequency);
				updateSliders(false);
				controlUpdate(false);
			}
		});

		v2 = v.findViewById(R.id.burst_button);
		v2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int burst = getBurst();
				if ( burst > 5000 ) burst = 5001; // trigger the warning but don't go oom
				int[] burstCommand = new int[burst*2];
				for (int iii = 0; iii < burst; iii++) {
					burstCommand[iii*2+0] = calculateTime(true);
					burstCommand[iii*2+1] = calculateTime(false);
				}
				mActivity.startFlashing(burstCommand, false, MainActivity.S_STROBE);
			}
		});
		
		v2.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				DialogFragment df = new BurstDialogFragment();
				df.setTargetFragment(StrobeFragment.this, 0);
				df.show(mActivity.getSupportFragmentManager(), "");
				return true;
			}
		});
		
		
		return v;
	}

	
	
	@Override
	public void onPause() {
		super.onPause();
		
		mHandlerNumber++;
	}
	
	@Override
	public void startHandlers() {
		

		final TextView on = (TextView)mRoot.findViewById(R.id.real_cur_on_length);
		final TextView off = (TextView)mRoot.findViewById(R.id.real_cur_off_length);
		final TextView duty = (TextView)mRoot.findViewById(R.id.real_duty);
		
		boolean realtime = mPrefs.getBoolean(AdvancedFragment.P_REALTIME, false);
		on.setVisibility(realtime ? View.VISIBLE : View.GONE);
		off.setVisibility(realtime ? View.VISIBLE : View.GONE);
		duty.setVisibility(realtime ? View.VISIBLE : View.GONE);
		
		if (!realtime)
			return;
		
		final long handlerNumber = mHandlerNumber;
		(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (mHandlerNumber == handlerNumber){
					sendEmptyMessageDelayed(0, 250);
					if (mActivity.mService != null) {
						int[] onOff = mActivity.mService.doGetDutyData();
						int sum = onOff[0] + onOff[1];
						if (sum < 0) {
							off.setText("-- ms");
							on.setText("-- ms");
							duty.setText("--%");
							return;
						}
						off.setText(displayTime(onOff[1], sum < 100000, true));
						on.setText(displayTime(onOff[0], sum < 100000, true));
						
						int toDisp = (int)Math.round((float)onOff[0]*100/sum);
						duty.setText(Integer.toString(toDisp) + "%");
					}
					
				}
			}
		}).sendEmptyMessage(0);
	}
	
	@Override
	public void stopHandlers() {
		mHandlerNumber++;
	}
	
	
	
	
	
	
	
	private void controlUpdate(boolean startIfNotStarted) {

		int[] flashes = new int[] {calculateTime(true), calculateTime(false)};
		
		if (startIfNotStarted)
			mActivity.startFlashing(flashes, true, MainActivity.S_STROBE);
		else
			mActivity.updateIfRunning(flashes, true);
	}
	
	public void updateSliders(boolean fromMain) {
		if (mRoot == null)
			return;
		
		
		if (fromMain) {
			mFrequency = mPrefs.getFloat(P_FREQUENCY, 10f);
			mDuty = mPrefs.getInt(P_DUTY, 50);
			mUseRpm = AdvancedFragment.useRPM(mActivity);
			
			((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mFrequency);
			((SeekBar)mRoot.findViewById(R.id.duty_seek_bar)).setProgress(mDuty);
		}
		
		View v = mRoot;
		
		int offTime = calculateTime(false);
		int onTime = calculateTime(true);
		int savedTime = getSavedTime();

		float toUse = mFrequency;
		String unit = " hz";
		if (mUseRpm) {
			toUse *= 60;
			unit = " rpm";
		}
		
		((TextView)v.findViewById(R.id.space_unit)).setText(unit);
		((TextView)v.findViewById(R.id.frequency_text)).setText(String.format("%.03f", toUse));
		
		((TextView)v.findViewById(R.id.duty)).setText(Integer.toString(mDuty) + "%");
    	((TextView)v.findViewById(R.id.cur_off_length)).setText(displayTime(offTime, onTime + offTime < 100000, true));
    	TextView tv = (TextView)v.findViewById(R.id.cur_on_length);
    	tv.setText(displayTime(onTime, onTime + offTime < 100000, true));
    	tv.setBackgroundResource(useSaved() ? R.drawable.blue_border : 0);
    	
    	((TextView)v.findViewById(R.id.saved_num)).setText(displayTime(savedTime, savedTime < 100000, false));
		
    	((CheckBox)v.findViewById(R.id.steady_check)).setChecked(useSaved());
    	
    	((Button)v.findViewById(R.id.burst_button)).setText("Burst (" + Integer.toString(getBurst()) + ")");
    	
    	v.findViewById(R.id.burst_button).setVisibility(mPrefs.getBoolean(SettingsFragment.P_SHOW_BURST, false) 
    			? View.VISIBLE : View.INVISIBLE);
    	
    	v.findViewById(R.id.x2x12).setVisibility(AdvancedFragment.use2X(mActivity) ? View.VISIBLE : View.GONE);
		
	}
	
	private int getSavedTime() {
		return mPrefs.getInt(P_ON_LENGTH, 50000);
	}
	
	private boolean useSaved() {
		return mPrefs.getBoolean(P_USE_SAVED, false);
	}
	
	private int getBurst() {
		return mPrefs.getInt(P_BURST, 5);
	}
	
	
	private int calculateTime(boolean on) {
		double frameWidth = 1000000.0d/mFrequency;
		
		if (useSaved()) {
			int onTime = getSavedTime();
			if (onTime > frameWidth)
				onTime = (int)frameWidth;
			
			if (on) {
				return onTime;
			} else {
				return (int)(frameWidth - onTime);
			}
		}
		
		if (on) {
			return (int)(frameWidth*mDuty/100);
		} else {
			return (int)(frameWidth*(100-mDuty)/100);
		}
	}
	

    public static String displayTime(long micros, boolean decimal, boolean showMs) {
    	String toReturn = "";
    	if (decimal) {
    		toReturn = Float.toString(((float)((int)(micros / 100))) / 10f);
    	}
    	else {
    		toReturn = Long.toString(micros / 1000);
    	}
    	
    	return toReturn + (showMs ? " ms" : "");
    }


	@Override
	public void onVolumeUp() {
		mFrequency += 0.05f;
		saveFreqDuty();
		((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mFrequency);
		updateSliders(false);
		controlUpdate(false);
	}


	@Override
	public void onVolumeDown() {
		mFrequency -= 0.05f;
		if (mFrequency <= 0.001)
			mFrequency = 0.001f;
		saveFreqDuty();
		((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mFrequency);
		updateSliders(false);
		controlUpdate(false);
		
	}
	
    
	private void saveFreqDuty() {
		Editor e = mPrefs.edit();
		e.putFloat(P_FREQUENCY, mFrequency);
		e.putInt(P_DUTY, mDuty);
		MainActivity.apply(e);
	}
	
	public static class BurstDialogFragment extends DialogFragment {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final EditText et = (EditText)getActivity().getLayoutInflater().inflate(R.layout.dia_burst, null);
			et.setText(Integer.toString(prefs.getInt(P_BURST, 5)));
			builder.setTitle("Set number of flashes")
				.setView(et)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						try {
							int burst = Integer.valueOf(et.getText().toString());
							Editor e = prefs.edit();
							e.putInt(P_BURST, burst);
							MainActivity.apply(e);
							((StrobeFragment)getTargetFragment()).updateSliders(false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
					}
				});
			return builder.create();
		}
	}
    
    
	
	
	
}
