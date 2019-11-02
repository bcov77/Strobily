package com.tp77.StrobeLib;

import com.tp77.StrobeLib.MyNumberPicker.OnNumberChangedListener;
import com.tp77.StrobeLib.PatternStorage.PatternSaved;
import com.tp77.StrobeLib.VerticalSeekbar.VerticalSeekbarListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class PatternFragment extends MyFragment implements PatternSaved {

	
	private static final String P_PATTERN = "pattern";
	private static final String P_LAST_OPEN = "lastOpen";
	
	private static final String B_PATTERN = "bPattern";
	private static final String B_NAME = "bName";

	private SharedPreferences mPrefs;
	
	private View mRoot = null;
	
	public PatPart[] mPattern = new PatPart[0];
	
	private boolean mRearranging = false;
	private int mSelected = 0;

	private float dp;

	private boolean mUseRpm = false;
	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		

		dp = getResources().getDisplayMetrics().density;
	}
	
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}
	
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle bundle) {
		
		final View v = inflater.inflate(R.layout.frag_pattern_fragment, null);
		mRoot = v;
		
	
		mRoot.findViewById(R.id.stop_rearranging_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mRearranging = false;
				updateSliders(false);
			}
		});
		
		mRoot.findViewById(R.id.save_load_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				
				if (Build.VERSION.SDK_INT <= 7) {
					Toast.makeText(mActivity, "Your phone is too old!", Toast.LENGTH_SHORT).show();
					return;
				}
				
				String pattern = PatternStorage.patternToString(mPattern);
				if (pattern == null)
					pattern = "";
				
				Bundle bundle = new Bundle();
				bundle.putString(B_PATTERN, pattern);
				
				DialogFragment df = new SaveLoadDialogFragment();
				df.setTargetFragment(PatternFragment.this, 0);
				df.setArguments(bundle);
				df.show(mActivity.getSupportFragmentManager(), "");
			}
		});
		
		mRoot.findViewById(R.id.once_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mActivity.serviceReady(true))
					return;
				if (mActivity.mService.mFlashing)
					mActivity.stopFlashing();
				else
					mActivity.startFlashing(buildFlashes(), false, MainActivity.S_OTHER);
			}
		});

		mRoot.findViewById(R.id.looped_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mActivity.serviceReady(true))
					return;
				if (mActivity.mService.mFlashing)
					mActivity.stopFlashing();
				else
					mActivity.startFlashing(buildFlashes(), true, MainActivity.S_OTHER);
			}
		});
		
	
		return v;
	}
	
	
	
	
	
	
	
	
	
	
	
	private int[] buildFlashes() {
		
		int commands = PatPart.getCommandSpots(mPattern, mActivity, 0);

		if ( commands > 10000 ) {
			mActivity.toastOnUiThread(mActivity, "Sorry! Patterns limited to 10,000 events." +
					"Email me if this is an issue. It\'s easy to change", Toast.LENGTH_LONG);
			return new int[] {1000000, 1000000};
		}
		
		int[] flashes = new int[commands];
		
		PatPart.fillInCommands(0, flashes, mPattern, mActivity, 0);
		
		
		return flashes;
	}
	
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	void updateSliders(boolean fromMain) {
		
		

		if (fromMain) {
			
			mUseRpm = AdvancedFragment.useRPM(mActivity);
			
			mPattern = PatternStorage.stringToPattern(mPrefs.getString(P_PATTERN, ""));
			if (mPattern == null)
				mPattern = new PatPart[0];
			
			
			
		}
		
		
		
		
		
		LayoutInflater inflater = mActivity.getLayoutInflater();
		
		LinearLayout.LayoutParams lp = null;
		
		LinearLayout ll = (LinearLayout)mRoot.findViewById(R.id.pattern_list);
		ll.removeAllViews();
		View v = null;
		RelativeLayout rl = null;
		
		LinearLayout ll2 = null;
		
		for (int iii = 0; iii < mPattern.length + 1; iii++) {
			final int fiii = iii;
			
			if (mRearranging) {
				ll2 = (LinearLayout)inflater.inflate(R.layout.list_copy_move, null);
				
				ll2.findViewById(R.id.move).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						
						if (mPattern.length > 0) {
						
							PatPart[] next = new PatPart[mPattern.length];
							
							int insertSpot = fiii;
							if (mSelected < insertSpot)
								insertSpot--;
							
							int added = 0;
							int putPos = 0;
							int getPos = 0;
							for (int iii = 0; iii < mPattern.length - 1; iii++) {
								putPos = iii;
								if (iii >= insertSpot) {
									putPos++;
								}
								getPos = iii;
								if (getPos >= mSelected)
									getPos++;
								next[putPos] = mPattern[getPos];
							}
							
							next[insertSpot] = mPattern[mSelected];
							
							mPattern = next;
							mSelected = insertSpot;
							
						}
						
						savePattern();
						updateSliders(false);
						
						
					}
				});
				
				ll2.findViewById(R.id.copy).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						
						PatPart[] next = new PatPart[mPattern.length + 1];
						
						int toPut = 0;
						
						for (int iii = 0; iii < mPattern.length; iii++) {
							toPut = iii;
							if (toPut >= fiii)
								toPut++;
							next[toPut] = mPattern[iii];
						}
						next[fiii] = new PatPart(mPattern[mSelected]);
						mPattern = next;
						savePattern();
						updateSliders(false);
					}
				});
				
				ll.addView(ll2);
				
				
				v = new View(mActivity);
				lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int)(dp + 0.5f));
				v.setLayoutParams(lp);
				v.setBackgroundColor(0xFF008080);
				ll.addView(v);
				
			}
			if (iii >= mPattern.length)
				continue;
			
			rl = (RelativeLayout)inflater.inflate(R.layout.list_pat_part, null);
			
			rl.findViewById(R.id.flip_flop).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mSelected = fiii;
					mRearranging = true;
					updateSliders(false);
				}
			});

			rl.findViewById(R.id.x).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					PatPart[] next = new PatPart[mPattern.length - 1];
					int toGet = 0;
					for (int iii = 0; iii < next.length; iii++) {
						toGet = iii;
						if (toGet >= fiii)
							toGet++;
						next[iii] = mPattern[toGet];
					}
					
					mPattern = next;
					
					if (fiii == mSelected)
						mRearranging = false;
					
					savePattern();
					updateSliders(false);
				}
			});
			
			
			if (mRearranging && mSelected == iii) {
				rl.setBackgroundColor(0xFF003030);
			}
			
			PatPart part = mPattern[iii];
			
			TextView tv = (TextView)rl.findViewById(R.id.multiplier);
			tv.setText(Integer.toString(part.mFlashes) + "×");
			tv.setVisibility(part.mType == PatPart.T_DELAY || part.mType == PatPart.T_PATTERN ? View.GONE : View.VISIBLE);
			((TextView)rl.findViewById(R.id.type)).setText(part.getTypeTitle(mActivity));
			tv = (TextView)rl.findViewById(R.id.frequency);
			tv.setText(part.getFrequencyText(mUseRpm));
			tv.setVisibility(part.mType == PatPart.T_DELAY || part.mType == PatPart.T_PATTERN ? View.GONE : View.VISIBLE);
			tv = (TextView)rl.findViewById(R.id.time_text);
			tv.setText(part.getTimeText());
			tv.setTextSize(part.mType == PatPart.T_DELAY ? 16 : 12);
			tv.setVisibility(part.mType == PatPart.T_PATTERN ? View.GONE : View.VISIBLE);
			
			
			
			if (!mRearranging) {
				rl.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						editPatPart(fiii);
					}
				});
			}
			
			
			ll.addView(rl);

			v = new View(mActivity);
			lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int)(dp + 0.5f));
			v.setLayoutParams(lp);
			v.setBackgroundColor(0xFF008080);
			ll.addView(v);
			
			
			
			
		}
		

		rl = (RelativeLayout)inflater.inflate(R.layout.list_last_pat, null);
		rl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PatPart[] next = new PatPart[mPattern.length + 1];
				for (int iii = 0; iii < mPattern.length; iii++) {
					next[iii] = mPattern[iii];
				}
				next[mPattern.length] = new PatPart(mActivity);
				mPattern = next;
				savePattern();
				updateSliders(false);
			}
		});
		
		ll.addView(rl);
		

		v = new View(mActivity);
		lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int)(dp + 0.5f));
		v.setLayoutParams(lp);
		v.setBackgroundColor(0xFF008080);
		ll.addView(v);
		
		
		mRoot.findViewById(R.id.rearrange_buttons).setVisibility(mRearranging ? View.VISIBLE : View.GONE);
		mRoot.findViewById(R.id.normal_buttons).setVisibility(!mRearranging ? View.VISIBLE : View.GONE);
		
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		savePattern();
	}
	@Override
	public void stopHandlers() {
		savePattern();
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	private void savePattern() {
		String str = PatternStorage.patternToString(mPattern);
		if (str != null) {
			Editor e = mPrefs.edit();
			e.putString(P_PATTERN, str);
			MainActivity.apply(e);
		}
		
	}

	private void editPatPart(int index) {
		
		DialogFragment df = new EditDialogFragment();
		df.setTargetFragment(this, index);
		df.show(mActivity.getSupportFragmentManager(), "");
		
	}
	
	

	public static class EditDialogFragment extends DialogFragment {
		
		private PatPart mPp = null;
		private LinearLayout mRoot = null;
		private boolean mUseRpm = false;
		private MainActivity mActivity = null;
		
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			mActivity = (MainActivity)getActivity();
			mUseRpm = AdvancedFragment.useRPM(getActivity());

			final int index = getTargetRequestCode();

			try {
				mPp = new PatPart(((PatternFragment) getTargetFragment()).mPattern[index]);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(mActivity, "Error? Email me if you get this a lot.", Toast.LENGTH_LONG).show();
				mPp = new PatPart(mActivity);
			}
			
			
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
			mRoot = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.dia_edit_part, null);
			
			final View focusCatch = mRoot.findViewById(R.id.focus_catch);
			
			mRoot.findViewById(R.id.flashes).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mRoot.findViewById(R.id.focus_catch).requestFocus();
					mPp.mType = PatPart.T_FLASH;
					refreshLayout(false);
				}
			});

			mRoot.findViewById(R.id.delay).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mRoot.findViewById(R.id.focus_catch).requestFocus();
					mPp.mType = PatPart.T_FLASH_DELAY;
					refreshLayout(false);
				}
			});
			
			mRoot.findViewById(R.id.pattern).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mRoot.findViewById(R.id.focus_catch).requestFocus();
					mPp.mType = PatPart.T_PATTERN;
					refreshLayout(false);
				}
			});

			mRoot.findViewById(R.id.skipped).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPp.mType = PatPart.T_FLASH_DELAY;
					refreshLayout(false);
				}
			});

			mRoot.findViewById(R.id.time).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPp.mType = PatPart.T_DELAY;
					refreshLayout(false);
				}
			});
			
			VerticalSeekbar vsb = (VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar);
			vsb.setListener(new VerticalSeekbarListener() {
				@Override
				public void onSeekbarChange(float progress) {
					mPp.mFrequency = progress;
					
					refreshLayout(true);
				}
			});
			vsb.setProgress(mPp.mFrequency);
			
			SeekBar dutySeek = (SeekBar)mRoot.findViewById(R.id.duty_seek_bar);
			dutySeek.setMax(100);
			dutySeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					mPp.mDuty = progress;
					
					refreshLayout(true);
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
			dutySeek.setProgress(mPp.mDuty);
			
			View v2 = mRoot.findViewById(R.id.save_button);
			v2.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPp.mTime = mPp.calculateTime(true);
					
					refreshLayout(false);
				}
			});
			
			v2.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					mPp.mTime = 0;
					
					refreshLayout(false);
					return true;
				}
			});
			
			mRoot.findViewById(R.id.steady_check).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPp.mUseTime = !mPp.mUseTime;
					
					refreshLayout(false);
				}
			});
			

			mRoot.findViewById(R.id.back_layer).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					focusCatch.requestFocus();
				}
			});
			

			((EditText)mRoot.findViewById(R.id.frequency_text)).setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String text = ((EditText)v).getText().toString();
						try {
							mPp.mFrequency = Float.parseFloat(text);
							if (mUseRpm)
								mPp.mFrequency /= 60;

							if (mPp.mFrequency < 0.001)
								mPp.mFrequency = 0.001f;
							
							((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mPp.mFrequency);
							
							refreshLayout(false);
						} catch (Exception e) {
							e.printStackTrace();
						}

					} 
				}
			});
			

			((EditText)mRoot.findViewById(R.id.saved_num)).setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String text = ((EditText)v).getText().toString();
						try {
							int onLength = (int) (Float.parseFloat(text) * 1000);

							if (onLength < 0)
								onLength = 0;
							
							mPp.mTime = onLength;
							
							refreshLayout(false);
						} catch (Exception e) {
							e.printStackTrace();
						}

					} 
				}
			});

			mRoot.findViewById(R.id.cur_on_length).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mRoot.findViewById(R.id.saved_num).requestFocus();
				}
			});
			
			
			mRoot.findViewById(R.id.test_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mRoot.findViewById(R.id.focus_catch).requestFocus();
					int[] flashes = new int[mPp.getCommandSpots(mActivity, 0)];
					mPp.fillInCommands(0, flashes, mActivity, 0);
					((MainActivity)getActivity()).startFlashing(flashes, false, MainActivity.S_OTHER);
				}
			});
			
			((EditText)mRoot.findViewById(R.id.time_edit_text)).setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						try {
							mPp.mTime = Integer.valueOf(((EditText)v).getText().toString())*1000;
						} catch (Exception e) {
							e.printStackTrace();
						}
						refreshLayout(false);
					}
				}
			});
			
			
			refreshLayout(false);
			
			
			builder
				.setView(mRoot)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						View focus = mRoot.findViewById(R.id.test_button);
						focus.requestFocus();
						
						if (mPp.mType == PatPart.T_DELAY) {
							try {
								mPp.mTime = Integer.valueOf(((EditText)mRoot.findViewById(R.id.time_edit_text)).getText().toString())*1000;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						if (mPp.mType == PatPart.T_FLASH || mPp.mType == PatPart.T_FLASH_DELAY) {

							String text = ((EditText)mRoot.findViewById(R.id.frequency_text)).getText().toString();
							try {
								mPp.mFrequency = Float.parseFloat(text);
								if (mUseRpm)
									mPp.mFrequency /= 60;

								if (mPp.mFrequency < 0.001)
									mPp.mFrequency = 0.001f;
								
								((VerticalSeekbar)mRoot.findViewById(R.id.vertical_seekbar)).setProgress(mPp.mFrequency);
								
								refreshLayout(false);
							} catch (Exception e) {
								e.printStackTrace();
							}

							text = ((EditText)mRoot.findViewById(R.id.saved_num)).getText().toString();
							try {
								int onLength = (int) (Float.parseFloat(text) * 1000);

								if (onLength < 0)
									onLength = 0;
								
								mPp.mTime = onLength;
								
								refreshLayout(false);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						PatternFragment frag = (PatternFragment)getTargetFragment();
						frag.mPattern[index] = mPp;
						frag.savePattern();
						frag.updateSliders(false);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						
					}
				});
			Dialog d =  builder.create();
			
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);
			
			return d;
		}
		
		
		private View flashes;
		private View delay;
		private View pattern;
		private View flashes_underline;
		private View delay_underline;
		private View pattern_underline;
		private View skipped;
		private View time;
		private View skipped_underline;
		private View time_underline;
		private View flash_controls;
		private View time_controls;
		private View pattern_controls;
		private View delay_controls;
		private MyNumberPicker mnp;
		private TextView space_unit;
		private TextView frequency_text;
		private TextView duty;
		private TextView cur_off_length;
		private TextView cur_on_length;
		private TextView saved_num;
		private CheckBox steady_check;
		private EditText time_edit_text;
		private ListView pattern_list;
		
		private void refreshLayout(boolean sliderUpdate) {
			if (flashes == null) {
				flashes = mRoot.findViewById(R.id.flashes);
				delay = mRoot.findViewById(R.id.delay);
				pattern = mRoot.findViewById(R.id.pattern);
				flashes_underline = mRoot.findViewById(R.id.flashes_underline);
				delay_underline = mRoot.findViewById(R.id.delay_underline);
				pattern_underline = mRoot.findViewById(R.id.pattern_underline);
				skipped = mRoot.findViewById(R.id.skipped);
				time = mRoot.findViewById(R.id.time);
				skipped_underline = mRoot.findViewById(R.id.skipped_underline);
				time_underline = mRoot.findViewById(R.id.time_underline);
				flash_controls = mRoot.findViewById(R.id.flash_controls);
				time_controls = mRoot.findViewById(R.id.time_controls);
				pattern_controls = mRoot.findViewById(R.id.pattern_controls);
				delay_controls = mRoot.findViewById(R.id.delay_controls);
				mnp = (MyNumberPicker) mRoot.findViewById(R.id.flashes_picker);
				space_unit = (TextView) mRoot.findViewById(R.id.space_unit);
				frequency_text = (TextView) mRoot.findViewById(R.id.frequency_text);
				duty = (TextView) mRoot.findViewById(R.id.duty);
				cur_off_length = (TextView) mRoot.findViewById(R.id.cur_off_length);
				cur_on_length = (TextView) mRoot.findViewById(R.id.cur_on_length);
				saved_num = (TextView) mRoot.findViewById(R.id.saved_num);
				steady_check = (CheckBox) mRoot.findViewById(R.id.steady_check);
				time_edit_text = (EditText) mRoot.findViewById(R.id.time_edit_text);
				pattern_list = (ListView) mRoot.findViewById(R.id.pattern_list);
				
			}
			
			
			if (!sliderUpdate) {
				flashes.setBackgroundColor(mPp.mType == PatPart.T_FLASH ? 0xFF004040 : 0x00000000);
				delay.setBackgroundColor(mPp.mType == PatPart.T_DELAY || mPp.mType == PatPart.T_FLASH_DELAY ? 0xFF004040 : 0x00000000);
				pattern.setBackgroundColor(mPp.mType == PatPart.T_PATTERN ? 0xFF004040 : 0x00000000);
				flashes_underline.setBackgroundColor(mPp.mType == PatPart.T_FLASH ? 0xFF004040 : 0xFF008080);
				delay_underline.setBackgroundColor(mPp.mType == PatPart.T_DELAY || mPp.mType == PatPart.T_FLASH_DELAY ? 0xFF004040 : 0xFF008080);
				pattern_underline.setBackgroundColor(mPp.mType == PatPart.T_PATTERN ? 0xFF004040 : 0xFF008080);
				
				delay_controls.setVisibility(mPp.mType == PatPart.T_DELAY || mPp.mType == PatPart.T_FLASH_DELAY ? View.VISIBLE : View.GONE);
				
				if (mPp.mType == PatPart.T_DELAY || mPp.mType == PatPart.T_FLASH_DELAY) {
					skipped.setBackgroundColor(mPp.mType == PatPart.T_FLASH_DELAY ? 0xFF004040 : 0x00000000);
					time.setBackgroundColor(mPp.mType != PatPart.T_FLASH_DELAY ? 0xFF004040 : 0x00000000);
					skipped_underline.setBackgroundColor(mPp.mType == PatPart.T_FLASH_DELAY ? 0xFF004040 : 0xFF008080);
					time_underline.setBackgroundColor(mPp.mType != PatPart.T_FLASH_DELAY ? 0xFF004040 : 0xFF008080);
				}
				
				flash_controls.setVisibility(mPp.mType == PatPart.T_FLASH_DELAY || mPp.mType == PatPart.T_FLASH ? View.VISIBLE : View.GONE);
				time_controls.setVisibility(mPp.mType == PatPart.T_DELAY ? View.VISIBLE : View.GONE);
				pattern_controls.setVisibility(mPp.mType == PatPart.T_PATTERN ? View.VISIBLE : View.GONE);
			}
			if (mPp.mType == PatPart.T_FLASH_DELAY || mPp.mType == PatPart.T_FLASH) {
				if (!sliderUpdate) {
					mnp.setMinMaxStepSuff(1, 100000, 1, "");
					mnp.setValue(mPp.mFlashes);
					mnp.setOnNumberChangedListener(new OnNumberChangedListener() {
						@Override
						public void onNumberChanged(int newVal) {
							mPp.mFlashes = newVal;
						}
					});
				}
				
				
				int offTime = mPp.calculateTime(false);
				int onTime = mPp.calculateTime(true);
	
				float toUse = mPp.mFrequency;
				String unit = " hz";
				if (mUseRpm) {
					toUse *= 60;
					unit = " rpm";
				}
				
				
				space_unit.setText(unit);
				space_unit.setTextSize(mUseRpm ? 28 : 36);
				frequency_text.setText(String.format("%.03f", toUse));
				frequency_text.setTextSize(mUseRpm ? 28 : 36);
				
				duty.setText(Integer.toString(mPp.mDuty) + "%");
		    	cur_off_length.setText(StrobeFragment.displayTime(offTime, onTime + offTime < 100000, true));
		    	cur_on_length.setText(StrobeFragment.displayTime(onTime, onTime + offTime < 100000, true));
		    	cur_on_length.setBackgroundResource(mPp.mUseTime ? R.drawable.blue_border : 0);
		    	
		    	saved_num.setText(StrobeFragment.displayTime(mPp.mTime, mPp.mTime < 100000, false));
				
		    	steady_check.setChecked(mPp.mUseTime);
			}
			if (!sliderUpdate) {
				if (mPp.mType == PatPart.T_DELAY) {
					time_edit_text.setText(Integer.toString(mPp.mTime/1000));
				}
				if (mPp.mType == PatPart.T_PATTERN) {

					PatternStorage storage = null;
					try {
						storage = PatternStorage.getInstance(mActivity);
					}  catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
					}
					
					
					ListView lv = pattern_list;
					
					if (lv.getFooterViewsCount() == 0) {
						View v = new View(mActivity);
						lv.addFooterView(v);
					}

					if ( storage != null ) {
						final String[] names = storage.getPatterns();
						lv.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.list_patterns, names) {
							@Override
							public View getView(final int position, View recycleView, ViewGroup parent) {
								LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.list_patterns_dialog, null);
								((TextView) ll.findViewById(R.id.name)).setText(names[position]);

								if (names[position].equals(mPp.mName)) {
									ll.setBackgroundColor(0xFF004040);
								}

								return ll;
							}
						});
						lv.setOnItemClickListener(new OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> parent, View view,
													int position, long id) {
								mPp.mName = names[position];
								refreshLayout(false);
							}
						});
					}
					lv.setFooterDividersEnabled(true);
					
					
				}
				
			}
			
			
		}


		
		
				
		
		
	}
	
	

	public static class SaveLoadDialogFragment extends DialogFragment implements PatternSaved {
		
		private MainActivity mActivity;
		
		private LinearLayout mRoot;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			
			Bundle args = getArguments();
			
			String curPat = args.getString(B_PATTERN);
			if (curPat == null)
				curPat = "";
			
			final String fcurPat = curPat;
			
			mActivity = (MainActivity)getActivity();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			LinearLayout ll = (LinearLayout)mActivity.getLayoutInflater().inflate(R.layout.dia_save_load, null);
			
			mRoot = ll;
			
			ll.findViewById(R.id.save_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					DialogFragment df = new NameDialogFragment();
					df.setTargetFragment(SaveLoadDialogFragment.this, 0);
					Bundle bundle = new Bundle();
					bundle.putString(B_PATTERN, fcurPat);
					df.setArguments(bundle);
					df.show(mActivity.getSupportFragmentManager(), "");
				}
			});
			
			setupListView();
			
			builder.setTitle("Save / Load")
				.setView(ll)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
			return builder.create();
		}

		private void setupListView() {
			PatternStorage storage = null;
			try {
				storage = PatternStorage.getInstance(mActivity);
			}  catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
			}
			
			
			ListView lv = (ListView) mRoot.findViewById(R.id.pattern_list);
			
			if (lv.getFooterViewsCount() == 0) {
				View v = new View(mActivity);
				lv.addFooterView(v);
			}

			if ( storage != null ) {
				final String[] names = storage.getPatterns();
				lv.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.list_patterns, names) {
					@Override
					public View getView(final int position, View recycleView, ViewGroup parent) {
						LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.list_patterns, null);
						((TextView) ll.findViewById(R.id.name)).setText(names[position]);

						ll.findViewById(R.id.x).setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								DialogFragment df = new DeleteDialogFragment();
								df.setTargetFragment(SaveLoadDialogFragment.this, 0);
								Bundle bundle = new Bundle();
								bundle.putString(B_NAME, names[position]);
								df.setArguments(bundle);
								df.show(mActivity.getSupportFragmentManager(), "");
							}
						});

						return ll;
					}
				});
				lv.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
											int position, long id) {
						Editor e = PreferenceManager.getDefaultSharedPreferences(mActivity).edit();
						e.putString(P_LAST_OPEN, names[position]);
						MainActivity.apply(e);
						PatternFragment frag = (PatternFragment) getTargetFragment();
						PatternStorage storage = null;
						try {
							storage = PatternStorage.getInstance(mActivity);
						}  catch (Exception e2) {
							e2.printStackTrace();
							Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
						}
						if ( storage != null ) {
							frag.mPattern = storage.getPattern(names[position]);
						} else {
							// Are these things valid when created like this?
							frag.mPattern = new PatPart[] {new PatPart(mActivity) };
						}
						frag.updateSliders(false);
						dismiss();
					}
				});
			}
			lv.setFooterDividersEnabled(true);
			
			
		}
		
		@Override
		public void patternSaved() {
			((PatternSaved)getTargetFragment()).patternSaved();
			setupListView();
		}
		
		
	}
    
	

	public static class NameDialogFragment extends DialogFragment implements PatternSaved {
		
		private MainActivity mActivity;
		private String mCurPat;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			Bundle args = getArguments();
			
			mCurPat = args.getString(B_PATTERN);
			if (mCurPat == null)
				mCurPat = "";
			
			
			
			mActivity = (MainActivity)getActivity();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			LinearLayout ll = (LinearLayout)mActivity.getLayoutInflater().inflate(R.layout.dia_enter_name, null);
			
			
			
			builder.setTitle("Enter a name:")
				.setView(ll)
				.setPositiveButton("Ok", null)
				.setNegativeButton("Cancel", null);
			return builder.create();
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			Dialog d = getDialog();
			if (d != null) {
				final EditText et = (EditText)d.findViewById(R.id.name_edit_text);
				et.setText(PreferenceManager.getDefaultSharedPreferences(mActivity).getString(P_LAST_OPEN, ""));
				et.selectAll();
				
				((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						String name = et.getText().toString();

						PatternStorage storage = null;
						try {
							storage = PatternStorage.getInstance(mActivity);
						}  catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
						}
						if ( storage != null ) {
							if (storage.getPattern(name) == null) {
								storage.addPattern(name, mCurPat);
								patternSaved();
							} else {
								DialogFragment df = new OverwriteDialogFragment();
								df.setTargetFragment(NameDialogFragment.this, 0);
								Bundle bundle = new Bundle();
								bundle.putString(B_PATTERN, mCurPat);
								bundle.putString(B_NAME, name);
								df.setArguments(bundle);
								df.show(mActivity.getSupportFragmentManager(), "");
							}
						}
					}
				});
			}
		}
		

		@Override
		public void patternSaved() {
			((PatternSaved)getTargetFragment()).patternSaved();
			dismiss();
		}
		
	}
	

	public static class OverwriteDialogFragment extends DialogFragment implements PatternSaved {
		
		private MainActivity mActivity;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			Bundle args = getArguments();
			
			String curPat = args.getString(B_PATTERN);
			if (curPat == null)
				curPat = "";
			
			String name = args.getString(B_NAME);
			if (name == null)
				name = "";
			
			final String fcurPat = curPat;
			final String fname = name;
			
			
			mActivity = (MainActivity)getActivity();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			LinearLayout ll = (LinearLayout)mActivity.getLayoutInflater().inflate(R.layout.dia_overwrite, null);
			
			((TextView)ll.findViewById(R.id.overwrite_text)).setText("The pattern \"" + fname + "\" already exists.");
			
			
			builder.setTitle("Duplicate name!")
				.setView(ll)
				.setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						PatternStorage storage = null;
						try {
							storage = PatternStorage.getInstance(mActivity);
						}  catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
						}
						if ( storage != null ) {
							storage.addPattern(fname, fcurPat);
						}
						patternSaved();
					}
				})
				.setNegativeButton("Cancel", null);
			return builder.create();
		}

		@Override
		public void patternSaved() {
			((PatternSaved)getTargetFragment()).patternSaved();
		}
		
		
	}
	

	public static class DeleteDialogFragment extends DialogFragment implements PatternSaved {
		
		private MainActivity mActivity;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			Bundle args = getArguments();
			
			String name = args.getString(B_NAME);
			if (name == null)
				name = "";
			
			final String fname = name;
			
			
			mActivity = (MainActivity)getActivity();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			LinearLayout ll = (LinearLayout)mActivity.getLayoutInflater().inflate(R.layout.dia_overwrite, null);
			
			((TextView)ll.findViewById(R.id.overwrite_text)).setText("Are you sure you want to delete \"" + name + "\"?");
			
			
			builder.setTitle("Delete " + name + "?")
				.setView(ll)
				.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						PatternStorage storage = null;
						try {
							storage = PatternStorage.getInstance(mActivity);
						}  catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
						}
						if ( storage != null ) {
							storage.deletePattern(fname);
						}
						patternSaved();
					}
				})
				.setNegativeButton("Cancel", null);
			return builder.create();
		}

		@Override
		public void patternSaved() {
			((PatternSaved)getTargetFragment()).patternSaved();
		}
		
		
	}
	
	
	@Override
	public void patternSaved() {
	}

}
