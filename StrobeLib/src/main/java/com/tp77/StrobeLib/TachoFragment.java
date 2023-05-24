package com.tp77.StrobeLib;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.LightingColorFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class TachoFragment extends MyFragment{

	
	private static final int FREQUENCY = 44100/8;
	
	private static final int BIN_SIZE = 8;
	
	private static final String P_FREQ_FAMILIES = "freqFamilies";
	
	private static final int FULL = 0;
	private static final int HIGH = 1;
	private static final int LOW = 2;
	private static final int ECHO = 3;
	
	private SharedPreferences mPrefs;
	
	private View mRoot = null;
	
	
	
	private float mFrequency = 0;
	private boolean mUseRpm = true;
	
	private double mMax = 0;
	
	

	short[] buf = new short[32768];
	
	double[] a = new double[32768/BIN_SIZE];
	double[] b = new double[32768/BIN_SIZE];

	FFT fft = new FFT(32768/BIN_SIZE);

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
		
		final View v = inflater.inflate(R.layout.frag_tacho_fragment, null);
		mRoot = v;
		

		final View focusCatch = v.findViewById(R.id.focus_catch);
		

		v.findViewById(R.id.back_layer).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				focusCatch.requestFocus();
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
					frequencyUpdate(true);
				}
			}
		});

		v.findViewById(R.id.full_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if ( ! mActivity.haveMicrophone() ) {
					return;
				}
				
				(new Thread() {
					@Override
					public void run() {
						final FreqFamily[] families = getFamilies(FULL);
						mActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showFamilies(families);
							}
						});
					}
				}).start();
			}
		});
		
		v.findViewById(R.id.low_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ( ! mActivity.haveMicrophone() ) {
					return;
				}
		
				(new Thread() {
					@Override
					public void run() {
						final FreqFamily[] families = getFamilies(LOW);
						mActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showFamilies(families);
							}
						});
					}
				}).start();
			}
		});

		v.findViewById(R.id.high_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ( ! mActivity.haveMicrophone() ) {
					return;
				}
				
				
				(new Thread() {
					@Override
					public void run() {
						final FreqFamily[] families = getFamilies(HIGH);
						mActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showFamilies(families);
							}
						});
					}
				}).start();
			}
		});
		
		v.findViewById(R.id.echo_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(mActivity, "Long click this button.\nWarning: Makes loud noise!", Toast.LENGTH_LONG).show();
			}
		});
		
		v.findViewById(R.id.echo_button).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if ( ! mActivity.haveMicrophone() ) {
					return true;
				}
				
				(new Thread() {
					@Override
					public void run() {
						final FreqFamily[] families = getFamilies(ECHO);
						mActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showFamilies(families);
							}
						});
					}
				}).start();
				return true;
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
						
						Editor e = mPrefs.edit();
						e.putFloat(StrobeFragment.P_FREQUENCY, mFrequency);
						MainActivity.apply(e);
						
						updateSliders(false);
						
						frequencyUpdate(false);
						
						
					} catch (Exception e) {
						e.printStackTrace();
					}

				} 
			}
		});
		
		View v2 = v.findViewById(R.id.help_button);
		v2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {	
				(new TachoHelpDialogFragment()).show(mActivity.getSupportFragmentManager(), "");
			}
		});
		v2.setVisibility(SettingsFragment.helpVisibility(mActivity));
		
		return v;
	}
	
	
	
	
	private void showFamilies(FreqFamily[] families) {
		LinearLayout ll = (LinearLayout)mRoot.findViewById(R.id.freq_holder);
		
		LinearLayout.LayoutParams lp;
		
		ll.removeAllViews();

		
		for (FreqFamily fam : families) {
			
			LinearLayout ll2 = new LinearLayout(mActivity);
			lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			ll2.setLayoutParams(lp);
			
			
			double[] freqs = fam.getFamily();
			double[] mags = fam.getMags();
			
			for (int iii = 0; iii < freqs.length; iii++) {
				final double freq = freqs[iii];
				lp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT);
				Button b = new Button(mActivity);
				
				String str;
				
				if (iii == 0) {
					str = StrobeFragment.displayTime((int) (freq * (mUseRpm ? 60 : 1)*1000), !mUseRpm, false) + (mUseRpm ? " rpm" : "hz");
					lp.weight = 5;
				} else {
					str = "1/" + Integer.toString(iii + 1);
					lp.weight = 3;
				}
				
				b.setText(str);
				b.setLayoutParams(lp);
				b.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mFrequency = (float) freq;
						frequencyUpdate(true);
						Editor e = mPrefs.edit();
						e.putFloat(StrobeFragment.P_FREQUENCY, mFrequency);
						e.putInt(StrobeFragment.P_DUTY, 0);
						MainActivity.apply(e);
						updateSliders(false);
						
					}
				});
				
				double ofMax = mags[iii] / mMax;
				
				int colorNumber = (int) (200*ofMax - 80);
				if (colorNumber <= 0)
					colorNumber = 1;
				
				if (ofMax > 0)
					b.getBackground().setColorFilter(new LightingColorFilter(0, SettingsFragment.getColor(colorNumber)));
				
				ll2.addView(b);
			}
			
			ll.addView(ll2);
			
		}
		
	}
	

	private static final int BITRATE = 44100;

	@SuppressLint("NewApi")
	private FreqFamily[] getFamilies(int mode) {
		
		AudioManager am = null;
		AudioTrack at = null;
		
		if (mode == ECHO) {
			am = (AudioManager) mActivity.getSystemService(Activity.AUDIO_SERVICE);
		
			short[] soundData = new short[BITRATE*2];
			
			int width = BITRATE/FREQUENCY/2;
			
			short[] waveform = new short[width];
			for (int iii = 0; iii < width; iii++) {
				if (iii < width/2) {
					waveform[iii] = 32767;
				} else {
					waveform[iii] = 0;
				}
//				waveform[iii] = (short)(32767*Math.sin(Math.PI * (double) iii / (double)width));
			}
			
			boolean high = false;
			int ofThis = 0;
		
			int inWidth = 0;
			for (int iii = 0; iii < BITRATE*2; iii++) {
				if (iii % 2 == 0) {
					inWidth = iii;
				} else {
					inWidth = iii + width/2;
					if (inWidth > width)
						inWidth -= width;
				}
//				if (iii % 2 == 0) {
//					soundData[iii] = 0;
//					continue;
//				}
//				if (high) {
//					soundData[iii] = 32767;
//				} else {
//					soundData[iii] = 0;
//				}
//				
//				high ^= true;
//				
//				soundData[iii] = (short)(32767*Math.random());
				
				if (high) {
					soundData[iii] = waveform[ofThis];
				} else {
					soundData[iii] = (short) (-1* waveform[ofThis]);
				}
				
				ofThis++;
				if (ofThis == width) {
					ofThis = 0;
					high^= true;
				}
			}
			
			
			at = new AudioTrack(AudioManager.STREAM_MUSIC, 
					BITRATE, AudioFormat.CHANNEL_OUT_STEREO,
					AudioFormat.ENCODING_PCM_16BIT, BITRATE*2*Short.SIZE,
					AudioTrack.MODE_STATIC);
			
			at.write(soundData, 0, BITRATE*2);
			at.play();
			

			try {
				Thread.sleep(200);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		}
		


		AudioRecord ar = new AudioRecord(AudioSource.MIC, BITRATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 32768);
		ar.startRecording();
		
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Arrays.fill(a, 0);
		Arrays.fill(b, 0);
		
		int read = ar.read(buf, 0, buf.length);
		

		ar.stop();
		ar.release();
		ar = null;
		
		if (mode == ECHO) {
			at.stop();
			at.release();
			at = null;
		}
		
		for (int iii = 0; iii < read/BIN_SIZE; iii++) {
			for (int jjj = 0; jjj < BIN_SIZE; jjj++) {
				a[iii] += buf[iii*BIN_SIZE+jjj];
			}
			
		}
		
		
		fft.fft(a, b);
		

//
//		byte[] endl = "\r\n".getBytes();
//
//		try {
//			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/sdcard/_fft.csv"));
//			
//			for (int iii = 0; iii < a.length; iii++) {
//				bos.write(Double.toString(a[iii]).getBytes());
//				bos.write(endl);
//			}
//			
//			bos.flush();
//			bos.close();
//			
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		

		
		
		boolean partial = mode != FULL && mode != ECHO;
		boolean high = mode != LOW;
//		
//		if (highFreq != null) {
//			partial = true;
//			high = highFreq.booleanValue();
//		}
		
		for (int iii = 0; iii < a.length; iii++) {
			a[iii] = a[iii]*a[iii] + b[iii]*b[iii];
			if (iii > 10000/BIN_SIZE && iii < (32768/BIN_SIZE-10000/BIN_SIZE))
				a[iii] = 0;
			if (partial && ((iii < 1600/BIN_SIZE || iii > (32768/BIN_SIZE-1600/BIN_SIZE)) ^ (!high)))
				a[iii] = 0;
//			if (mode == ECHO) {
//				if ((iii > 4096-30 && iii < 4096+30) || (iii < (32768 - (4096-30)) && iii > (32768 - (4096+30))))
//					a[iii] = 0;
//			}
		}
		
		Arrays.fill(b, 0);
		
		fft.ifft(a, b);
		
//
//		try {
//			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/sdcard/_auto.csv"));
//			
//			for (int iii = 0; iii < a.length; iii++) {
//				bos.write(Double.toString(a[iii]).getBytes());
//				bos.write(endl);
//			}
//			
//			bos.flush();
//			bos.close();
//			
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
		
		int len = a.length/2;
		
		double avg = 0;
		
		for (int iii = 0; iii < len; iii++) {
			avg += a[iii];
		}
		
		avg /= len;
		
		double std = 0;
		double tmp = 0;
		
		for (int iii = 0; iii < len; iii++) {
			tmp = a[iii] - avg;
			std += tmp*tmp;
		}
		
		std = Math.sqrt(std)/(len);
		
		double cut = std*6 + avg;
		
		
		int[] peaks = new int[1024];	// this would be a lol case
		int peaksFound = 0;
		
		boolean inPeak = false;
		double currentPeak = 0;
		int currentPeakPos = 0;
		boolean isAboveCut = false;
		
		
		for (int iii = 500/BIN_SIZE; iii < len; iii++) {
			isAboveCut = a[iii] > cut;
			
			if (inPeak) {
				if (isAboveCut) {
					if (a[iii] > currentPeak) {
						currentPeak = a[iii];
						currentPeakPos = iii;
					}
					
				} else {
					peaks[peaksFound++] = currentPeakPos;
					inPeak = false;
				}
			} else {
				if (isAboveCut) {
					inPeak = true;
					currentPeak = a[iii];
					currentPeakPos = iii;
				}
			}
			
		}
		
		double magnitudes[] = new double[peaksFound];
		for (int iii = 0; iii < peaksFound; iii++) {
			magnitudes[iii] = a[peaks[iii]];
		}
		
		Arrays.sort(magnitudes);
		
		cut = 0;
		if (peaksFound > 5) {
			cut = magnitudes[peaksFound-5];
		}
		
		if (peaksFound > 0)
			mMax = magnitudes[peaksFound-1];
		
		ArrayList<FreqFamily> list = new ArrayList<FreqFamily>();
		
		for (int iii = 0; iii < peaksFound; iii++) {
			if (a[peaks[iii]] < cut)
				continue;
			double freq = 44100/BIN_SIZE/(double)peaks[iii];
			boolean matched = false;
			for (int jjj = 0; jjj < list.size(); jjj++) {
				if (list.get(jjj).isHarmonic(freq, a[peaks[iii]], true)) {
					matched = true;
					break;
				}
			}
			if (!matched) {
				list.add(new FreqFamily(freq, a[peaks[iii]]));
			}
		}
		
		FreqFamily[] toRet = list.toArray(new FreqFamily[list.size()]);
		

		if (Build.VERSION.SDK_INT >= 8) {
			ByteArrayOutputStream baos = null;
			ObjectOutputStream oos = null;
			
			
			try {
				baos = new ByteArrayOutputStream();
				oos = new ObjectOutputStream(new BufferedOutputStream(baos));
			
				oos.writeInt(toRet.length);
				oos.writeDouble(mMax);
				
				for (FreqFamily fam : toRet)
					fam.writeExternal(oos);
				oos.flush();
			
				String str = Base64.encodeToString(baos.toByteArray(), 0);
				
				Editor e = mPrefs.edit();
				e.putString(P_FREQ_FAMILIES, str);
				MainActivity.apply(e);
			
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if(oos != null)
						oos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return toRet;
		
		
	}
	

	private void frequencyUpdate(boolean startIfNotStarted) {

		int[] flashes = new int[] {0, (int) (1000000/mFrequency)};
		
		if (startIfNotStarted)
			mActivity.startFlashing(flashes, true, MainActivity.S_TACHO);
		else
			mActivity.updateIfRunning(flashes, true);
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public void updateSliders(boolean fromMain) {
		
		if (mRoot == null)
			return;
		
		if (fromMain) {
			
			mFrequency = mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
			mUseRpm = AdvancedFragment.useRPM(mActivity);
			
			if (Build.VERSION.SDK_INT >= 8) {
				
				String str = mPrefs.getString(P_FREQ_FAMILIES, "");
				
				if (str.length() >= 0) {
				
					ObjectInputStream ois = null;
					Externalizable res = null;
					try {
						ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decode(str, 0)));

						FreqFamily[] families = new FreqFamily[ois.readInt()];
						
						mMax = ois.readDouble();
						
						for (int iii = 0; iii < families.length; iii++) {
							families[iii] = new FreqFamily(ois);
						}
						
						
						showFamilies(families);
			  
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if(ois != null)
								ois.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
			 
					}
			
					
				}
				
				
				
			}
			
			
			
		}
		
		

		float toUse = mFrequency;
		String unit = " hz";
		if (mUseRpm) {
			toUse *= 60;
			unit = " rpm";
		}
		
		TextView tv = null;
		
		tv = (TextView)mRoot.findViewById(R.id.space_unit);
		tv.setText(unit);
		tv.setTextSize(mUseRpm ? 26 : 30);
		tv = (TextView)mRoot.findViewById(R.id.frequency_text);
		tv.setText(String.format("%.03f", toUse));
		tv.setTextSize(mUseRpm ? 26 : 30);
		
		
	}
	
	

	public static class TachoHelpDialogFragment extends DialogFragment {
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			ScrollView sv = (ScrollView)getActivity().getLayoutInflater().inflate(R.layout.dia_tacho_help, null);
			builder.setTitle("Tachometer")
				.setView(sv)
				.setPositiveButton("Ok", null);
			return builder.create();
		}
	}
    

	@Override
	public void onVolumeUp() {
		mFrequency += 0.05f;
		Editor e = mPrefs.edit();
		e.putFloat(StrobeFragment.P_FREQUENCY, mFrequency);
		MainActivity.apply(e);
		
		updateSliders(false);
		frequencyUpdate(false);
	}


	@Override
	public void onVolumeDown() {
		mFrequency -= 0.05f;
		if (mFrequency <= 0)
			mFrequency = 0;
		Editor e = mPrefs.edit();
		e.putFloat(StrobeFragment.P_FREQUENCY, mFrequency);
		MainActivity.apply(e);
		
		updateSliders(false);
		frequencyUpdate(false);
	}
	
    
	
	
}
