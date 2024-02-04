package com.tp77.StrobeLib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class DiagnosticFragment extends MyFragment {
	
	
	public static final String P_REFRESH_INTERVAL = "refreshInterval";
	public static final String P_SHOW_LED_STATE = "showLedState";
	
	public static final int R_MATCH = -1;
	public static final int R_ON_TOUCH = -2;

	private SharedPreferences mPrefs;
	
	private View mRoot = null;

	private long mHandlerNumber = System.currentTimeMillis();
	private Handler mUpdateHandler = null;
	
	private boolean mUseLedState = false;
	
	

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
		
		final View v = inflater.inflate(R.layout.frag_diagnostic_fragment, null);
		mRoot = v;
		
		final DiagnosticView dv = (DiagnosticView)mRoot.findViewById(R.id.diag_view);
		
		
		mRoot.findViewById(R.id.refresh_word).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EveryDialog.showDialog(EveryDialog.N_PICK_FREQ, mActivity);
			}
		});
		
		mRoot.findViewById(R.id.legend).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EveryDialog.showDialog(EveryDialog.N_LEGEND, mActivity);
			}
		});
		
		((CheckBox)v.findViewById(R.id.led_state)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SettingsFragment.togglePref(P_SHOW_LED_STATE, false, mActivity);
				
				updateSliders(false);
				if (mUpdateHandler != null) {
					mUpdateHandler.sendEmptyMessage(1);
				}
				
			}
		});
		
		
//		mRoot.findViewById(R.id.do_it).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				EveryDialog.showDialog(EveryDialog.N_PICK_FREQ, mActivity);
////				long[] diagData = mActivity.mService.doGetDiagnostics();
////				dv.setData(diagData);
////				doDataAnalysis(diagData);
//			}
//			
//		});
		
		
		return mRoot;
	}
	
	
	
	public void startHandlers() {
		final long handlerNumber = mHandlerNumber;
		
		int refresh = mPrefs.getInt(P_REFRESH_INTERVAL, 1000);
		if (refresh == R_MATCH) {
			refresh = (int) (1000 / mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10));
			if (refresh < 1000 / 60)
				refresh = 1000 / 60;
		}
		final DiagnosticView dv = (DiagnosticView)mRoot.findViewById(R.id.diag_view);
		if (refresh == R_ON_TOUCH) {
			dv.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					diagnosticViewUpdate(dv);
				}
			});
			return;
		}
		
		final int frefresh = refresh;
		
		dv.setOnClickListener(null);
		mUpdateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (handlerNumber == mHandlerNumber) {
					if (msg.what == 0)
						sendEmptyMessageDelayed(0, frefresh);
					diagnosticViewUpdate(dv);
				}
			}
		};
		mUpdateHandler.sendEmptyMessage(0);
		
	}
	
	public void stopHandlers() {
		mHandlerNumber++;
	}
	
	
	private void diagnosticViewUpdate(DiagnosticView dv) {
		if (mActivity.mService == null)
			return;
		long[] diagData = mActivity.mService.doGetDiagnostics();
		dv.setData(diagData, mUseLedState);
		doDataAnalysis(diagData, mRoot);
	}
	
	
	
	// pulse width defined as middle of DI_ON to middle of DI_OFF
	
	public static float doDataAnalysis(long[] diagData, View root) {
		
		int pos = (int)diagData[DiagnosticView.DI_SIZE];
		int number = 0;

		while (number < DiagnosticView.DI_LENGTH-1)
		{
			pos--;
			if (pos < 0)
				pos = DiagnosticView.DI_LENGTH-1;
			if (diagData[pos*DiagnosticView.DI_CATEGORIES] <= 0)
				break;
			
			
			number++;
		}
		
		if (number <= 1)
			return 0;
		
		long pulseSum = 0;
		long onoffSum = 0;
		
		int offset = 0;
		
		int numberOfFlashes = 0;
		
		int flashes = 0;
		int lags = 0;
		int rejects = 0;
		boolean nextIsLag = false;

		pos++;
		if (pos == DiagnosticView.DI_LENGTH)
			pos = 0;
		long startTime = diagData[pos*DiagnosticView.DI_CATEGORIES];
		for (int iii = 0; iii < number - 1; iii++) {
			if (pos == DiagnosticView.DI_LENGTH)
				pos = 0;
			
			offset = pos*DiagnosticView.DI_CATEGORIES;
			
			
			if (diagData[offset + DiagnosticView.DI_ON] == -1) {
				if (nextIsLag) {
					lags++;
				} else {
					rejects++;
				}
			} else {
				flashes++;
			}

			if (diagData[offset + DiagnosticView.DI_OFF_TO_ON] == -1) 
				nextIsLag = true;
			
			
			if (diagData[offset + DiagnosticView.DI_ON] != -1) {
				numberOfFlashes++;
				
				long on = diagData[offset + DiagnosticView.DI_ONS] - diagData[offset + DiagnosticView.DI_ON];
				long off = diagData[offset + DiagnosticView.DI_OFFS] - diagData[offset + DiagnosticView.DI_OFF];
				
				long pulse = diagData[offset + DiagnosticView.DI_OFFS] -
						diagData[offset + DiagnosticView.DI_ONS];
				
				pulseSum += pulse;
				onoffSum += on + off;
			}
			pos++;
		}
		
		
		
		long endTime = diagData[pos*DiagnosticView.DI_CATEGORIES];
		
		long totalTime = endTime-startTime;
		
		int correction = rejects - lags;

		if ( flashes == 0 || numberOfFlashes == 0 ) {
			return 0;
		}

		if ( flashes == 0 ) {
			flashes = 1;
		}
		if ( numberOfFlashes == 0 ) {
			numberOfFlashes = 1;
		}
		int useFlashes = flashes;
		if (correction > 0)
			useFlashes += correction;

		
		int period = (int) (totalTime / useFlashes);
		if ( period == 0 ) period = 1;
		

		float pulseWidth = (float)pulseSum/numberOfFlashes;
		float duty = (float)pulseWidth*100/period;
		
		float minTime = (float)onoffSum/numberOfFlashes;
		if ( minTime == 0 ) minTime = 1;
		float maxHz = 1000000/minTime;
		float hz = (float)1000000/period;
		

		if (root != null) {
			((TextView) root.findViewById(R.id.actual_hz)).setText(String.format("%.1f Hz", hz));
			((TextView) root.findViewById(R.id.max_hz)).setText(String.format("%.1f Hz", maxHz));
			((TextView) root.findViewById(R.id.duty)).setText(String.format("%.1f%%", duty));
			((TextView) root.findViewById(R.id.duration)).setText(String.format("%.1f ms", pulseWidth / 1000));
		}
		return hz;
	}
	

	/*
	 *  normal ## ## ## ## ## ## ##  fl
	 *  reject ## -1 -1 -1 -1 -1 ##  sk
     flash lag ## ## ## ## ## ## -1  fl
	 *         ## -1 -1 -1 -1 -1 ##  lg
	reject lag ## -1 -1 -1 -1 -1 -1  sk
	 *         ## -1 -1 -1 -1 -1 ##  lg
 flash lag lag ## ## ## ## ## ## -1
               ## -1 -1 -1 -1 -1 -1
               ## -1 -1 -1 -1 -1 ##
	 * 
	 * 
	 * 
	 */
	


	@Override
	void updateSliders(boolean fromMain) {
		
		mUseLedState = mPrefs.getBoolean(P_SHOW_LED_STATE, false);
		((CheckBox)mRoot.findViewById(R.id.led_state)).setChecked(mUseLedState);
		
		int refresh = mPrefs.getInt(P_REFRESH_INTERVAL, 1000);
		String refreshStr = "";
		if (refresh == 1000)
			refreshStr = "1 Hz";
		if (refresh == DiagnosticFragment.R_MATCH)
			refreshStr = "Match strobe Hz";
		if (refresh == DiagnosticFragment.R_ON_TOUCH)
			refreshStr = "On graph touch";
		if (refreshStr.length() == 0) {
			refreshStr = "Every " + Integer.toString(refresh) + " ms";
		}
		
		((TextView)mRoot.findViewById(R.id.refresh_word)).setText(Html.fromHtml("<u>" + refreshStr + "</u>"));
		
	}
	
	
	
	
}
