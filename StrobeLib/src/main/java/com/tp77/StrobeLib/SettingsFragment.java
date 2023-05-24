package com.tp77.StrobeLib;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
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

public class SettingsFragment extends MyFragment {


	private SharedPreferences mPrefs;
	
	private View mRoot = null;
	
	public static final String P_MAX_FREQUENCY = "maxFrequency";
	public static final String P_FLASH_COLOR = "flashColor";
	public static final String P_PREVIEW_HACK = "previewHack";
	public static final String P_PERSIST = "persist";
	public static final String P_SHOW_BURST = "showBurst";
	public static final String P_DIM = "dim";
	public static final String P_HELP = "help";
	
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
		
		final View v = inflater.inflate(R.layout.frag_settings_fragment, null);
		mRoot = v;
		
		SeekBar sb = (SeekBar)v.findViewById(R.id.max_seek_bar);
		sb.setMax(99);
		sb.setProgress(getMaxFrequency(mActivity)-1);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				Editor e = mPrefs.edit();
				e.putInt(P_MAX_FREQUENCY, progress+1);
				MainActivity.apply(e);
				
				updateSliders(false);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mActivity.refreshFragments();
			}
		});
		
		sb = (SeekBar)v.findViewById(R.id.color_seek_bar);
		sb.setMax(362);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
  			@Override
  			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
  				Editor e = mPrefs.edit();
  				e.putInt(P_FLASH_COLOR, progress);
  				MainActivity.apply(e);
  				mActivity.updateScreenColor();

  				// If not fromUser, then the colorbar was already set
  				if ( fromUser ) {
					updateSliders(false);
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		mRoot.findViewById(R.id.new_camera_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.clickNewCamera(!mPrefs.getBoolean(MainActivity.P_NEW_CAMERA, false), true);
				updateSliders(false);
			}
		});

		mRoot.findViewById(R.id.preview_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePref(P_PREVIEW_HACK, false, mActivity);
				if ( mPrefs.getBoolean(P_PREVIEW_HACK, false ) ) {
					setPref(P_PERSIST, false, mActivity);
				}
				updateSliders(false);
				mActivity.applyPreview();
				
			}
		});
		mRoot.findViewById(R.id.persist_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePref(P_PERSIST, true, mActivity);
				if ( mPrefs.getBoolean(P_PREVIEW_HACK, false) ) {
					setPref(P_PREVIEW_HACK, false, mActivity);
				}
				updateSliders(false);
			}
		});
		mRoot.findViewById(R.id.dim_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePref(P_DIM, false, mActivity);
				mActivity.setProperBrightness();
				updateSliders(false);
			}
		});
		mRoot.findViewById(R.id.burst_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePref(P_SHOW_BURST, false, mActivity);
				updateSliders(false);
				mActivity.refreshFragments();
			}
		});
		mRoot.findViewById(R.id.help_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePref(P_HELP, true, mActivity);
				updateSliders(false);
				mActivity.refreshFragments();
			}
		});

		mRoot.findViewById(R.id.tutorial).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TutorialDialog.showDialog(TutorialDialog.N_INTRO, -1, mActivity);
			}
		});
		
		
		return v;
		
	}

	
	public void updateSliders(boolean fromMain) {
		if (mRoot == null)
			return;
		
		String str;
		if (AdvancedFragment.useRPM(mActivity)) {
			str = Integer.toString(getMaxFrequency(mActivity)*60) + " rpm";
		} else {
			str = Integer.toString(getMaxFrequency(mActivity)) + " hz";
		}
		((TextView)mRoot.findViewById(R.id.max_text)).setText(str);
		
		mRoot.findViewById(R.id.color_sample).setBackgroundColor(getColor(getFlashColor(mActivity)));
		((SeekBar)mRoot.findViewById(R.id.color_seek_bar)).setProgress(getFlashColor(mActivity));

		((CheckBox)mRoot.findViewById(R.id.new_camera_check)).setChecked(mPrefs.getBoolean(MainActivity.P_NEW_CAMERA, false));
		((CheckBox)mRoot.findViewById(R.id.preview_check)).setChecked(mPrefs.getBoolean(P_PREVIEW_HACK, false));
		((CheckBox)mRoot.findViewById(R.id.persist_check)).setChecked(mPrefs.getBoolean(P_PERSIST, true));
		((CheckBox)mRoot.findViewById(R.id.dim_check)).setChecked(mPrefs.getBoolean(P_DIM, false));
		((CheckBox)mRoot.findViewById(R.id.burst_check)).setChecked(mPrefs.getBoolean(P_SHOW_BURST, false));
		((CheckBox)mRoot.findViewById(R.id.help_check)).setChecked(mPrefs.getBoolean(P_HELP, true));
		
		mRoot.findViewById(R.id.preview_hack_stuff).setVisibility(
				mPrefs.getBoolean(MainActivity.P_NEW_CAMERA, false) ? View.GONE : View.VISIBLE );
	
	}
	
	public static int helpVisibility(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(P_HELP, true) ? View.VISIBLE : View.GONE;
	}
	
	public static int getMaxFrequency(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(P_MAX_FREQUENCY, 30);
	}
	
	public static int getFlashColor(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(P_FLASH_COLOR, 0);
	}
	
	public static int getColor(int mFlashColor) {
		if (mFlashColor == 0 || mFlashColor == 362)
			return Color.WHITE;
		return Color.HSVToColor(new float[] {mFlashColor-1, 1, 1});
	}
	
	public static void togglePref(String pref, boolean defValue, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor e = prefs.edit();
		e.putBoolean(pref, !prefs.getBoolean(pref, defValue));
		MainActivity.apply(e);
	}
	
	public static void setPref(String pref, boolean value, Context context) {
		Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
		e.putBoolean(pref,  value);
		MainActivity.apply(e);
	}


	
}
