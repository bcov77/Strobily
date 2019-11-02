package com.tp77.StrobeLib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnShowListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class EveryDialog extends DialogFragment {
	
	
	private static String B_NUMBER = "bNumber";
	
	public static final int N_PICK_FREQ = 0;
	public static final int N_CUSTOM = 1;
	public static final int N_LEGEND = 2;
	public static final int N_UPDATE = 3;
	public static final int N_BETA = 4;
	public static final int N_RATE = 5;
	
	private MainActivity mActivity = null;
	private View mRoot = null;
	
	private int mNumber = 0;
	
	protected SharedPreferences mPrefs;
	
	private static final int[] LAYOUTS = new int[] {
		R.layout.dia_refresh_freq,
		R.layout.dia_refresh_custom,
		R.layout.dia_legend,
		R.layout.dia_update,
		R.layout.dia_beta,
		R.layout.dia_rate
	};
	
	private static EveryDialog getInstance(int number) {
		EveryDialog tut = new EveryDialog();
		Bundle bundle = new Bundle();
		bundle.putInt(B_NUMBER, number);
		tut.setArguments(bundle);
		
		return tut;
	}
	
	public static EveryDialog showDialog(int number, MainActivity activity) {
		EveryDialog tut = getInstance(number);
		tut.show(activity.getSupportFragmentManager(), "");
		
		return tut;
	}
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		
	}
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
//		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppBaseTheme);
	
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		Dialog d = getDialog();
//		if (d != null) {
//			d.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
//			mRoot.requestLayout();
//		}

		
	}
	
	@Override
	public void onPause() {
		super.onPause();

	}
	
	private void doRadio(int selected, CheckBox ch0, CheckBox ch1, CheckBox ch2, CheckBox ch3) {
		ch0.setChecked(selected == 0);
		ch1.setChecked(selected == 1);
		ch2.setChecked(selected == 2);
		ch3.setChecked(selected == 3);
	}

	@SuppressLint("NewApi")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		Bundle args = getArguments();
		if (args != null) {
			mNumber = args.getInt(B_NUMBER);
		}
		

		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		
		mRoot = mActivity.getLayoutInflater().inflate(LAYOUTS[mNumber], null);
		
		builder.setView(mRoot);
		
		switch (mNumber) {
		case N_PICK_FREQ:
			builder.setTitle("Select update frequency:");
			builder.setNegativeButton("Cancel", null);
			
			final CheckBox ch0 = (CheckBox)mRoot.findViewById(R.id.one_check);
			final CheckBox ch1 = (CheckBox)mRoot.findViewById(R.id.match_check);
			final CheckBox ch2 = (CheckBox)mRoot.findViewById(R.id.touch_check);
			final CheckBox ch3 = (CheckBox)mRoot.findViewById(R.id.custom_check);
			
			mRoot.findViewById(R.id.one).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					doRadio(0, ch0, ch1, ch2, ch3);
				}
			});
			mRoot.findViewById(R.id.match).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					doRadio(1, ch0, ch1, ch2, ch3);
				}
			});
			mRoot.findViewById(R.id.touch).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					doRadio(2, ch0, ch1, ch2, ch3);
				}
			});
			mRoot.findViewById(R.id.custom).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					doRadio(3, ch0, ch1, ch2, ch3);
				}
			});
			
			int before = mPrefs.getInt(DiagnosticFragment.P_REFRESH_INTERVAL, 1000);
			int selected = 3;
			if (before == 1000)
				selected = 0;
			if (before == DiagnosticFragment.R_MATCH)
				selected = 1;
			if (before == DiagnosticFragment.R_ON_TOUCH)
				selected = 2;
			doRadio(selected, ch0, ch1, ch2, ch3);
			

			builder.setPositiveButton("Ok", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int toSave = 0;
					if (ch0.isChecked())
						toSave = 1000;
					if (ch1.isChecked())
						toSave = DiagnosticFragment.R_MATCH;
					if (ch2.isChecked())
						toSave = DiagnosticFragment.R_ON_TOUCH;
					if (toSave == 0) {
						EveryDialog.showDialog(N_CUSTOM, mActivity);
						return;
					}
					
					Editor e = mPrefs.edit();
					e.putInt(DiagnosticFragment.P_REFRESH_INTERVAL, toSave);
					MainActivity.apply(e);
					mActivity.updateSliders();
					MyFragment frag = mActivity.getCurrentFragment();
					frag.stopHandlers();
					frag.startHandlers();
				}
			});
			
			
			break;
		case N_CUSTOM:
			builder.setNegativeButton("Cancel", null);
			int start = mPrefs.getInt(DiagnosticFragment.P_REFRESH_INTERVAL, 1000);
			if (start < 0)
				start = 1000;
			final EditText et = (EditText)mRoot.findViewById(R.id.custom_et);
			et.setText(Integer.toString(start));
			et.selectAll();
			builder.setPositiveButton("Ok", null);
			final Dialog dd = builder.create();
			if (Build.VERSION.SDK_INT >= 8) {
				dd.setOnShowListener(new OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						View v = ((AlertDialog)dd).getButton(AlertDialog.BUTTON_POSITIVE);
						v.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								int value = -1;
								try {
									value = Integer.valueOf(et.getText().toString().trim());
								} catch (Exception e) {
									e.printStackTrace();
								}
								if (value < 10) {
									Toast.makeText(mActivity, "Please enter a value greater than 10 ms", Toast.LENGTH_LONG).show();
								} else {
									Editor e = mPrefs.edit();
									e.putInt(DiagnosticFragment.P_REFRESH_INTERVAL, value);
									MainActivity.apply(e);
									mActivity.updateSliders();
									MyFragment frag = mActivity.getCurrentFragment();
									frag.stopHandlers();
									frag.startHandlers();
									dd.dismiss();
								}
							}
						});
					}
					
				});
				
			}
			return dd;
		case N_LEGEND:
			builder.setTitle("Legend");
			builder.setPositiveButton("Back", null);
			break;
		case N_UPDATE:
			try {
				builder.setTitle("Update (" + mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName + ")");
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			builder.setPositiveButton("Ok", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			break;
		case N_BETA:
			builder.setTitle("Join the beta?");

			mRoot.findViewById(R.id.beta).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String url = "https://play.google.com/apps/testing/com.tp77.Strobe";
					if ( mActivity.mAds ) {
						url = "https://play.google.com/apps/testing/com.tp77.StrobeAd";
					}
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
				}
			});
			builder.setPositiveButton("Dismiss", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor ed = mPrefs.edit();
					ed.putBoolean(MainActivity.P_SEEN_BETA, true);
					MainActivity.apply(ed);
				}
			});
			break;

		case N_RATE:
			builder.setTitle("Strobily needs your help!");

			mRoot.findViewById(R.id.beta).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					Editor ed = mPrefs.edit();
					ed.putBoolean(MainActivity.P_RATED, true);
					MainActivity.apply(ed);

					String url = "https://play.google.com/store/apps/details?id=com.tp77.Strobe";
					if ( mActivity.mAds ) {
						url = "https://play.google.com/store/apps/details?id=com.tp77.StrobeAd";
					}
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
					dismiss();

				}
			});
			builder.setPositiveButton("Later", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			builder.setNegativeButton("No thanks", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor ed = mPrefs.edit();
					ed.putBoolean(MainActivity.P_RATED, true);
					MainActivity.apply(ed);
				}
			});
			break;


		}
		
		
		Dialog d = builder.create();

		
//		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		return d;
		
	
	}
	

	
	

}
