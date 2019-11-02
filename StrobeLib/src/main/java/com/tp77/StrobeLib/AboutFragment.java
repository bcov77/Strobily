package com.tp77.StrobeLib;

import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends MyFragment {

	private SharedPreferences mPrefs;
	
	private View mRoot = null;
	
	

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
		
		final View v = inflater.inflate(R.layout.frag_about_fragment, null);
		mRoot = v;
		
		((TextView)v.findViewById(R.id.app_name)).setText("Strobily");// + (mActivity.mAds ? " Free" : ""));
		try {
			((TextView)v.findViewById(R.id.version_name)).setText("v" + 
					mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		v.findViewById(R.id.email_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMeEmail(mActivity, "Strobily problem");
			}
		});
		
		v.findViewById(R.id.go_back_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity((new Intent(mActivity, StrobeLibActivity.class)).putExtra(getString(com.tp77.StrobeLib.R.string.S_AD), mActivity.mAds));
			}
		});

		v.findViewById(R.id.rate).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor ed = mPrefs.edit();
				ed.putBoolean(MainActivity.P_RATED, true);
				MainActivity.apply(ed);

				String url = "https://play.google.com/store/apps/details?id=com.tp77.Strobe";
				if ( mActivity.mAds ) {
					url = "https://play.google.com/store/apps/details?id=com.tp77.StrobeAd";
				}
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
			}
		});
		
		
//		v.findViewById(R.id.beta).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				String url = "https://play.google.com/apps/testing/com.tp77.Strobe";
//				if ( mActivity.mAds ) {
//					url = "https://play.google.com/apps/testing/com.tp77.StrobeAd";
//				}
//				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//				startActivity(intent);
//			}
//		});
		
		TextView tv = (TextView)v.findViewById(R.id.privacy_policy);
		tv.setPaintFlags(tv.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
		tv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.bcov77.com/strobily/privacy_policy.html"));
				startActivity(browserIntent);
			}
			
		});
		
		
		return v;
	}


	@Override
	public void updateSliders(boolean fromMain) {
		if (mRoot == null)
			return;
		
	}
	
	public static void sendMeEmail(final MainActivity activity, final String subject) {
		
		final ProgressDialogFragment pdf = new ProgressDialogFragment();
		pdf.show(activity.getSupportFragmentManager(), "");
		
		(new Thread() {
			@TargetApi(Build.VERSION_CODES.FROYO)
			@Override 
			public void run() {
				
				
				Uri uri = null;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) { 
					try {
						String filename = activity.getExternalCacheDir() + "/log.txt";
						File theFile = new File(filename);
						theFile.delete();
						Process p = Runtime.getRuntime().exec("logcat -d -f " + filename);
						long stopTime = System.currentTimeMillis() + 4000;
						while (!theFile.exists() && System.currentTimeMillis() < stopTime) {
							try {
								sleep(100);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						if (Build.VERSION.SDK_INT >= 24 ) {

							uri = android.support.v4.content.FileProvider.getUriForFile(
									activity,
									activity.getApplicationContext()
											.getPackageName() + ".provider", //(use your app signature + ".provider" )
									theFile);
						} else {
							uri = Uri.fromFile(theFile);
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
					
					
				pdf.dismiss();

				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("plain/text");
				i.putExtra(Intent.EXTRA_EMAIL, new String[] {"bcoventry77@gmail.com"});
				try {
					i.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.email_body2, 
							activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName,
							activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode,
							Build.MODEL,
							Build.VERSION.RELEASE
							));
					i.putExtra(Intent.EXTRA_SUBJECT, subject);
					
					if (uri != null) {
						i.putExtra(Intent.EXTRA_STREAM, uri);
						i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					}
					
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				
				activity.startActivity(i);
		
			}
		}).start();
		
	}

	public static class ProgressDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			final ProgressDialog dialog = new ProgressDialog(getActivity());
			dialog.setTitle("Creating log...");
			dialog.setMessage("(It helps with debugging)");
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);

			// Disable the back button
			OnKeyListener keyListener = new OnKeyListener() {

				@Override
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					
					if( keyCode == KeyEvent.KEYCODE_BACK){					
						return true;
					}
					return false;
				}

			
			};
			dialog.setOnKeyListener(keyListener);
			return dialog;
		}

	}
	


}
