package com.tp77.StrobeLib;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v42.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioGroup;

public class TutorialDialog extends DialogFragment {
	
	public static String P_TUTORIAL_FINISHED = "tutorialFinished";
	
	private static String B_NUMBER = "bNumber";
	private static String B_BACK = "bBack";
	
	public static final int N_INTRO = 0;
	private static final int N_LED_TEST = 1;
	private static final int N_PREVIEW_TEST = 2;
	private static final int N_SUPER_BAD = 3;
	private static final int N_DONE = 4;
	private static final int N_PERMISSION = 5;
	
	private MainActivity mActivity = null;
	private View mRoot = null;
	private SharedPreferences mPrefs;
	
	private int mNumber = 0;
	private int mBack = 0;
	
	private static final int[] LAYOUTS = new int[] {
		R.layout.dia_tut_intro,
		R.layout.dia_tut_led_test,
		R.layout.dia_tut_preview_test,
		R.layout.dia_tut_super_bad,
		R.layout.dia_tut_done,
		R.layout.dia_tut_perm
	};
	
	private static TutorialDialog getInstance(int number, int back) {
		TutorialDialog tut = new TutorialDialog();
		Bundle bundle = new Bundle();
		bundle.putInt(B_NUMBER, number);
		bundle.putInt(B_BACK, back);
		tut.setArguments(bundle);
		return tut;
	}
	
	public static TutorialDialog showDialog(int number, int back, MainActivity activity) {
		TutorialDialog tut = getInstance(number, back);
		tut.setCancelable(false);
		try {
			tut.show(activity.getSupportFragmentManager(), "");
		} catch (IllegalStateException e) {
			e.printStackTrace(); // this catches a bug in android
		}
		
		return tut;
	}
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
		
	}
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.tut_dialog);
	
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		Dialog d = getDialog();
		if (d != null) {
			d.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
			mRoot.requestLayout();
		}


		if (mNumber == N_SUPER_BAD) {
			mActivity.handleLedScreenPress(true, true);
		} else {
			mActivity.handleLedScreenPress(false, true);
		}

		
	}

	@Override
	public void onResume() {
		super.onResume();

		if ( mNumber == N_PERMISSION && mActivity.selfPermissionGranted(Manifest.permission.CAMERA) ) {
			showDialog(N_LED_TEST, mNumber, mActivity);
			dismiss();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();

		mActivity.torchOff();
		mActivity.stopFlashing();
	}
	

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		Bundle args = getArguments();
		if (args != null) {
			mNumber = args.getInt(B_NUMBER);
			mBack = args.getInt(B_BACK);
		}
		

		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		
		mRoot = mActivity.getLayoutInflater().inflate(LAYOUTS[mNumber], null);
		
		builder.setView(mRoot);
		
		switch (mNumber) {
		case N_INTRO:
			builder.setPositiveButton("Next", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if ( mActivity.selfPermissionGranted(Manifest.permission.CAMERA ) ) {
						showDialog(N_LED_TEST, mNumber, mActivity);
					} else {
						showDialog(N_PERMISSION, mNumber, mActivity);
					}
				}
			});
			break;
		case N_PERMISSION:
			mRoot.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					back();
				}
			});
			mRoot.findViewById(R.id.try_again).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mActivity.mRequestedCamera = false;
					if ( mActivity.selfPermissionGranted(Manifest.permission.CAMERA) ) {
						showDialog(N_LED_TEST, mNumber, mActivity);
						dismiss();
					}
					mActivity.setupCameraStuff();
				}
			});
			builder.setPositiveButton("Next", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(N_LED_TEST, mNumber, mActivity);
				}
			});
			builder.setNegativeButton("Don\'t use LED", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(N_DONE, mNumber, mActivity);
				}
			});
			break;
			
		case N_LED_TEST:
			mRoot.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					back();
				}
			});
//			mRoot.findViewById(R.id.on).setOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					mActivity.torchOn();
//				}
//			});
//			mRoot.findViewById(R.id.off).setOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					mActivity.torchOff();
//				}
//			});
            final RadioGroup radioGroup = (RadioGroup)mRoot.findViewById(R.id.radio_group);
            radioGroup.setOnCheckedChangeListener( new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int setting = M_OLD_CAMERA;
                    if ( checkedId == R.id.old_camera ) {
                        setting = M_OLD_CAMERA;
                    }
                    if ( checkedId == R.id.new_camera ) {
                        setting = M_NEW_CAMERA;
                    }
                    if ( checkedId == R.id.old_camera_preview ) {
                        setting = M_OLD_PREVIEW;
                    }
                    setFromRadio(radioGroup, setting, false);
                }
            });
            setFromRadio(radioGroup, M_INIT, false);
			mRoot.findViewById(R.id.strobe).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!mActivity.serviceReady(true))
						return;
					if (mActivity.mService.mFlashing)
						mActivity.stopFlashing();
					else {
						int[] flashes = new int[] {(int) (1000000f/10/2), (int) (1000000f/10/2)};

						mActivity.startFlashing(flashes, true, MainActivity.S_STROBE);
					}
				}
			});
            mRoot.findViewById(R.id.torch).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mActivity.serviceReady(true))
                        return;
                    if (mActivity.mService.mTorch)
                        mActivity.torchOff();
                    else {
                        mActivity.torchOn();
                    }
                }
            });
			builder.setPositiveButton("It works!", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(N_DONE, mNumber, mActivity);
				}
			});
			builder.setNegativeButton("Doesn\'t work", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(N_SUPER_BAD, mNumber, mActivity);
				}
			});

			break;
		case N_SUPER_BAD:
			mRoot.findViewById(R.id.email_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AboutFragment.sendMeEmail(mActivity, "Strobily: LED doesn\'t work");
				}
			});
			mRoot.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					back();
				}
			});
			builder.setPositiveButton("Done", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mActivity.refreshFragments();
					tutorialFinished();
				}
			});
			break;
		case N_DONE:
			mRoot.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					back();
				}
			});
			builder.setPositiveButton("Let\'s do this!", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mActivity.refreshFragments();
					tutorialFinished();
				}
			});
			
			mRoot.findViewById(R.id.turntable_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mActivity.refreshFragments();
					int goTo = MainActivity.F_TURNTABLE;
					if (PreferenceManager.getDefaultSharedPreferences(mActivity).getBoolean(AdvancedFragment.P_DIAGNOSTIC, false))
						goTo++;
					mActivity.setPage(goTo);
					tutorialFinished();
					dismiss();
				}
			});
			break;
		}
		
		
		Dialog d = builder.create();

		
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.setCancelable(false);
		
		return d;
		
	
	}

	private static final int M_OLD_CAMERA = 0;
	private static final int M_NEW_CAMERA = 1;
	private static final int M_OLD_PREVIEW = 2;
	private static final int M_INIT = 3;

	private static final int[] radio_ids = {R.id.old_camera, R.id.new_camera, R.id.old_camera_preview};

	private void setFromRadio(RadioGroup group, int setting, boolean doSet ) {
        switch (setting) {
            case M_OLD_CAMERA: {
                mActivity.clickNewCamera(false, false);
                SettingsFragment.setPref(SettingsFragment.P_PREVIEW_HACK, false, mActivity);
                if ( mPrefs.getBoolean(SettingsFragment.P_PREVIEW_HACK, false ) ) {
                    SettingsFragment.setPref(SettingsFragment.P_PERSIST, false, mActivity);
                }
				mActivity.applyPreview();
//                mActivity.setupScroller(true);
                break;
            }
            case M_NEW_CAMERA: {
                mActivity.clickNewCamera(true, false);
//                mActivity.setupScroller(true);
                break;
            }
            case M_OLD_PREVIEW: {
                mActivity.clickNewCamera(false, false);
                SettingsFragment.setPref(SettingsFragment.P_PREVIEW_HACK, true, mActivity);
                SettingsFragment.setPref(SettingsFragment.P_PERSIST, false, mActivity);
				mActivity.applyPreview();
//                mActivity.setupScroller(true);
                break;
            }
        }
        if (doSet) {
			group.check(radio_ids[setting]);
		} else {
			if ( mPrefs.getBoolean(MainActivity.P_NEW_CAMERA, false) ) {
				setFromRadio(group, M_NEW_CAMERA, true);
			} else {
				if ( mPrefs.getBoolean(SettingsFragment.P_PREVIEW_HACK, false ) ) {
					setFromRadio(group, M_OLD_PREVIEW, true);
				} else {
					setFromRadio(group, M_OLD_CAMERA, true);
				}
			}
		}
    }
	

	private void tutorialFinished() {
		Editor e = PreferenceManager.getDefaultSharedPreferences(mActivity).edit();
		e.putBoolean(P_TUTORIAL_FINISHED, true);
		MainActivity.apply(e);
	}
	
	public static boolean isTutorialFinished(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(P_TUTORIAL_FINISHED, false);
	}
	
	private static final int[] DEFAULT_BACK = new int[] {0, N_INTRO, N_LED_TEST, N_PREVIEW_TEST, N_LED_TEST, N_INTRO};
	
	private void back() {
		if (mNumber == N_INTRO)
			return;
		
		int back = mBack;
		if (back < 0)
			back = DEFAULT_BACK[mNumber];
		
		if ( mNumber == N_LED_TEST ) {
			if ( ! mActivity.selfPermissionGranted(Manifest.permission.CAMERA) ) {
				back = N_PERMISSION;
			}
		}
		
		showDialog(back, -1, mActivity);
		dismiss();
		
		
	}
	

}
