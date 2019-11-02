package com.tp77.StrobeLib;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.tp77.StrobeLib.StrobeLibService.LocalBinder;

public class StrobeLibActivity extends Activity {


	private boolean mAds;

	private static final String S_M_BASE_FREQUENCY = "mBaseFrequency";
	private static final String S_M_FINE_TUNE = "mFineTune";
	private static final String S_M_DUTY = "mDuty";
	private static final String S_M_ON_LENGTH = "mOnLength";
	private static final String S_M_STEADY = "mSteady";
	private static final String S_M_USE_SCREEN = "mUseScreen";
	private static final String S_M_SUPE_DIM = "mSupeDim";
	private static final String S_M_AIRPLANE = "mAirplane";
	private static final String S_M_MAX_FREQUENCY = "mMaxFrequency";
	private static final String S_M_FLASH_COLOR = "mFlashColor";
	private static final String S_M_SHOW_FINE = "mShowFine";
	private static final String S_M_USE_RPM = "mUseRPM";
	private static final String S_M_MULTIPLIER = "mMultiplier";
	private static final String S_M_PERSISTENT = "mPersistent";
	private static final String S_M_PERFORM_A = "mPerformA";
	private static final String S_M_PREVIEW_HACK = "mPreviewHack";
	private static final String S_M_FOCUS_HACK = "mFocusHack";
	private static final String S_M_BURST_OPTION = "mBurstOption";
	private static final String S_M_BURST_NUMBER = "mBurstNumber";
	private static final String S_M_SHOW_X2 = "mShowX2";
	private static final String S_M_LATE_THRESHOLD = "mLateThreshold";
	private static final String S_M_FLASH_PREEMPT = "mFlashPreempt";
	private static final String S_M_PERFORM_B = "mPerformB";
	private static final String S_M_MULTI_KILL = "mMultiKill";
	public static final String S_FRIEND = "friend";
	public static final String S_WARNING = "warning";
	private static final String S_KNOWN_VERSION = "knownVersion";
	
	private static final int D_SETTINGS = 0;
	private static final int D_FAQ = 1;
	private static final int D_KEY = 2;
	private static final int D_WARNING = 3;
	private static final int D_CALCULATIONS = 4;
	private static final int D_TROUBLESHOOT = 5;
	private static final int D_NEW_VERSION = 6;
	private static final int D_MORE_FEATURES = 7;
	private static final int D_HELP_LAYOVER = 8;
	private static final int D_BURST = 9;
	
	public static final int SCREEN_ON = 0;
	public static final int SCREEN_OFF = 1;
	
	private static final String AD_ID = "ca-app-pub-3269351921095092/1067143562";
	

	private Camera mCamera;
	private Camera.Parameters mParamOn;
	private Camera.Parameters mParamOff;
	private Camera.Parameters mParamOff2;
	
	private int mBaseFrequency = 5;
	private float mFineTune = 0;
	private float mDuty = .5f;
	private int mOnLength = 100;
	private boolean mSteady = false;
	private boolean mUseScreen = false;
	private boolean mSupeDim = false;
	private boolean mAirplane = false;
	private int mMaxFrequency = 30;
	private int mFlashColor = 0;
	private boolean mShowFine = true;
	private boolean mUseRPM = false;
	private int mMultiplier = 9;
	private boolean mPersistent = true;
	private boolean mPerformA = false;
	private boolean mPreviewHack = false;
	private boolean mFocusHack = false;
	private boolean mBurstOption = false;
	private int mBurstNumber = 5;
	private boolean mShowX2 = false;
	private int mLateThreshold = -1;
	private int mFlashPreempt = 500;
	private boolean mPerformB = false;
	private boolean mMultiKill = true;
	
	

	
	private SeekBar mBaseSeek;
	private SeekBar mFineSeek;
	private SeekBar mDutySeek;
	private CheckBox mSteadyCheck;
	private CheckBox mUseScreenCheck;
	private CheckBox mSupeDimCheck;
	private CheckBox mHideAllCheck;
	private CheckBox mAirplaneCheck;
	private View mRoot;
	
	private long mScr;
	private long mScr2;
	
	private boolean mAirplaneSave;

	private SharedPreferences mPrefs;
	private Handler mScreenPost;
	private Handler mLagChecker;
	private boolean mHideAll;
	private boolean mLagging = false;
	
	private LayoutInflater mLayoutInflater;
	private PowerManager mPowerManager;
	private WakeLock mWakeLock;
	private SurfaceView mCameraSurface;
	
	
	private StrobeLibService mService;
	
	
	private static final float[] mMultipliers = new float[] {
		1/10f, 1/9f, 1/8f, 1/7f, 1/6f, 1/5f, 1/4f, 1/3f, 1/2f, 1f,
		2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f};
	
	private static final String[] mMultiplierStrings = new String[] {
		"1/10", "1/9", "1/8", "1/7", "1/6", "1/5", "1/4", "1/3", "1/2", "1",
		"2", "3", "4", "5", "6", "7", "8", "9", "10"};
	

	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(android.R.style.Theme_Black);
        mAds = getIntent().getBooleanExtra(getString(R.string.S_AD), true);


		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mRoot = findViewById(R.id.main_linear_layout);
        
		mPrefs =  PreferenceManager.getDefaultSharedPreferences(this);
		
		mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "com.tp77.StrobeLib:Strobe wake lock");
		
		mScreenPost = new Handler() {
			public void handleMessage(Message msg) {
	    		mRoot.setBackgroundColor(msg.arg1 == SCREEN_ON ? getColor() : Color.BLACK);
			}
		};
		
		
		mLagChecker = new Handler() {
			public void handleMessage(Message msg) {
				this.sendEmptyMessageDelayed(0, 500);
				mLagging = false;
				if (mService != null)
					mLagging = (mService.doCheckLag() > 0);
				
				
				if (!mHideAll) {
					View v = findViewById(R.id.lag_warning);
					if (v != null)
						v.setVisibility(mLagging ? View.VISIBLE : View.INVISIBLE);
				}
				
			}
		};
		mLagChecker.sendEmptyMessage(0);
		
        
		try {
	        if (mPrefs.getBoolean(S_WARNING, true))
	        	showDialog(D_WARNING);
	        else {
	        	if (mPrefs.getInt(S_KNOWN_VERSION, 0) != getPackageManager().getPackageInfo(getPackageName(), 0).versionCode)
	        		showDialog(D_NEW_VERSION);
	        }
	        
	        Editor e = mPrefs.edit();
	        e.putInt(S_KNOWN_VERSION, getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
	        e.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	            
		
        
    }
    
    
    public void onResume() {
    	super.onResume();

    	
    	Intent intent = new Intent(this, StrobeLibService.class);
    	startService(intent);
    	bindService(intent, mTheServiceConnection, Context.BIND_AUTO_CREATE);
    	
    	mBaseFrequency = mPrefs.getInt(S_M_BASE_FREQUENCY, 5);
		mFineTune = mPrefs.getFloat(S_M_FINE_TUNE, 0);
		mDuty = mPrefs.getFloat(S_M_DUTY, .5f);
		mOnLength = mPrefs.getInt(S_M_ON_LENGTH, 100);
		mSteady = mPrefs.getBoolean(S_M_STEADY, false);
		mUseScreen = mPrefs.getBoolean(S_M_USE_SCREEN, false);
		mSupeDim = mPrefs.getBoolean(S_M_SUPE_DIM, false);
		mAirplane = mPrefs.getBoolean(S_M_AIRPLANE, false);
		mMaxFrequency = mPrefs.getInt(S_M_MAX_FREQUENCY, 30);
		mFlashColor = mPrefs.getInt(S_M_FLASH_COLOR, 0);
		mShowFine = mPrefs.getBoolean(S_M_SHOW_FINE, true);
		mUseRPM = mPrefs.getBoolean(S_M_USE_RPM, false);
		mMultiplier = mPrefs.getInt(S_M_MULTIPLIER, 9);
		mPersistent = mPrefs.getBoolean(S_M_PERSISTENT, true);
		mPerformA = mPrefs.getBoolean(S_M_PERFORM_A, false);
		mPreviewHack = mPrefs.getBoolean(S_M_PREVIEW_HACK, false);
//		mFocusHack = mPrefs.getBoolean(S_M_FOCUS_HACK, false);
		mBurstOption = mPrefs.getBoolean(S_M_BURST_OPTION, false);
		mBurstNumber = mPrefs.getInt(S_M_BURST_NUMBER, 5);
		mShowX2 = mPrefs.getBoolean(S_M_SHOW_X2, false);
		mLateThreshold = mPrefs.getInt(S_M_LATE_THRESHOLD, -1);
		mFlashPreempt = mPrefs.getInt(S_M_FLASH_PREEMPT, 500);
		mPerformB = mPrefs.getBoolean(S_M_PERFORM_B, false);
		mMultiKill = mPrefs.getBoolean(S_M_MULTI_KILL, true);
		
		mAirplaneSave = Settings.System.getInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    	setAirplaneMode(mAirplane);
		

    	
        mCameraSurface = new SurfaceView(this);
        SurfaceHolder holder = mCameraSurface.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {	
			}
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {	
				
				if (holder.getSurface() == null) {
					Toast.makeText(StrobeLibActivity.this, "Surface is null", Toast.LENGTH_LONG).show();
					return;
				}
				if (mCamera != null) {
					try {
						mCamera.stopPreview();
					} catch (Exception e) {
					}
					try {
						mCamera.setPreviewDisplay(holder);
					} catch (IOException e) {
						e.printStackTrace();
						try {
							mCamera.setPreviewDisplay(null);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					mCamera.startPreview();
					if (mFocusHack) {
						try {
							mCamera.autoFocus(new AutoFocusCallback() {
				                public void onAutoFocus(boolean success, Camera camera) {
				                }
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}
			}
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (mCamera != null) {
					try {
						mCamera.stopPreview();
					} catch (Exception e) {}
					try {
						mCamera.setPreviewDisplay(null);
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						mCamera.startPreview();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
    	});
  
    
		
		mHideAll = false;
        
		setupUI();
		
    	setProperBrightness();
    	
    	
//    	(new Handler() {
//    		@Override
//    		public void handleMessage(Message msg) {
//    			this.sendEmptyMessageDelayed(0, 100);
//    			
//    			if (mService != null) {
//    				((TextView)findViewById(R.id.debug_number)).setText(Integer.toString(mService.getDebugNumber()));
//    			}
//    			
//    		}
//    	}).sendEmptyMessage(0);
    	
    	
    	
    	
    	
    	
    	
    	
    }
    

    
    public void onPause() {
    	super.onPause();
    	

    	if ((mService == null) || !mPersistent || mUseScreen 
    			|| !(mService.mRunning || mService.mTorch)) {
    		if (mService != null) {
    			mService.stopTheStrober(false);
    			mService.mBound = false;
    			mService.stopSelf();
    			unbindService(mTheServiceConnection);
    			mService = null;
    		}
    		
    		flashOff();
    		
    		if (mCamera != null) {
        		try {
        			mCamera.stopPreview();		
        		} catch (Exception e) {}		// this would be if the preview never started...
    	    	mCamera.release();
    	    	mCamera = null;
        	}
    		
    	}
    	else {
    		mService.goForeground();
    		unbindService(mTheServiceConnection);
    	}
    	
    	setWakeLock(false);
    	setAirplaneMode(mAirplaneSave);
    	
    	
    	Editor e = mPrefs.edit();
    	e.putInt(S_M_BASE_FREQUENCY, mBaseFrequency);
		e.putFloat(S_M_FINE_TUNE, mFineTune);
		e.putFloat(S_M_DUTY, mDuty);
		e.putInt(S_M_ON_LENGTH, mOnLength);
		e.putBoolean(S_M_STEADY, mSteady);
		e.putBoolean(S_M_USE_SCREEN, mUseScreen);
		e.putBoolean(S_M_SUPE_DIM, mSupeDim);
		e.putBoolean(S_M_AIRPLANE, mAirplane);
		e.putInt(S_M_MAX_FREQUENCY, mMaxFrequency);
		e.putInt(S_M_FLASH_COLOR, mFlashColor);
		e.putBoolean(S_M_SHOW_FINE, mShowFine);
		e.putBoolean(S_M_USE_RPM, mUseRPM);
		e.putInt(S_M_MULTIPLIER, mMultiplier);
		e.putBoolean(S_M_PERSISTENT, mPersistent);
		e.putBoolean(S_M_PERFORM_A, mPerformA);
		e.putBoolean(S_M_PREVIEW_HACK, mPreviewHack);
		e.putBoolean(S_M_FOCUS_HACK, mFocusHack);
		e.putBoolean(S_M_BURST_OPTION, mBurstOption);
		e.putInt(S_M_BURST_NUMBER,  mBurstNumber);
		e.putBoolean(S_M_SHOW_X2, mShowX2);
		e.putInt(S_M_LATE_THRESHOLD, mLateThreshold);
		e.putInt(S_M_FLASH_PREEMPT, mFlashPreempt);
		e.putBoolean(S_M_PERFORM_B, mPerformB);
		e.putBoolean(S_M_MULTI_KILL, mMultiKill);
		e.commit();
		
		setScreenBrightness(-1);
		
    }
    

	@Override
	public void onDestroy() {
		super.onDestroy();
		
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflator = getMenuInflater();
    	inflator.inflate(R.menu.menu, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_calculations) {
			showDialog(D_CALCULATIONS);
		} else if (item.getItemId() == R.id.menu_settings) {
			showDialog(D_SETTINGS);
			return true;
		} else if (item.getItemId() == R.id.menu_faq) {
			showDialog(D_FAQ);
			return true;
		} else if (item.getItemId() == R.id.menu_troubleshoot) {
			showDialog(D_TROUBLESHOOT);
			return true;
		} else if (item.getItemId() == R.id.menu_more_features) {
			showDialog(D_MORE_FEATURES);
		}
    	return (false);
    }
    
    @Override
	public Dialog onCreateDialog(int dialogId) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final LinearLayout ll;
		final ScrollView sv;
		SeekBar sb;
		Dialog dialog;
		switch (dialogId) {
		case D_SETTINGS:
			builder.setTitle("Settings");
			
			sv = (ScrollView) mLayoutInflater.inflate(R.layout.settings_dialog, null);
			ll = (LinearLayout) sv.findViewById(R.id.settings_linear_layout);
			
			sb = (SeekBar) ll.findViewById(R.id.max_seek_bar);
			sb.setMax(98);
			sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	  			@Override
	  			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
	  				mMaxFrequency = progress + 2;
	  				((TextView)((ViewGroup)sb.getParent()).findViewById(R.id.max_text)).setText(
	  						Integer.toString(mMaxFrequency) + " Hz");
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
			
			((CheckBox)ll.findViewById(R.id.fine_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mShowFine = isChecked;
				}
			});
			
			((CheckBox)ll.findViewById(R.id.show_burst_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mBurstOption == isChecked)
						return;
					mBurstOption = isChecked;
					if (mBurstOption)
						Toast.makeText(StrobeLibActivity.this, "Long-click the burst button to adjust", Toast.LENGTH_LONG).show();
				}
			});
			
			((CheckBox)ll.findViewById(R.id.persist_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mPersistent = isChecked;
				}
			});
			
			((CheckBox)ll.findViewById(R.id.performance_hack_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked == mPerformA)
						return;
					boolean torchSave = false;
					boolean runningSave = false;
					if (mService != null) {
						torchSave = mService.mTorch;
						runningSave = mService.mRunning;
					}
					
					if (isChecked && mService != null && mCamera != null) {
						mService.stopTheStrober(false);
				        Camera.Parameters testParam = mCamera.getParameters();
				        testParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				        
				        try {
				        	Field mMapField = null;
				        	mMapField = Camera.Parameters.class.getDeclaredField("mMap");
				        	mMapField.setAccessible(true);
				        	((HashMap)mMapField.get(testParam)).clear();
				        	testParam.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
				        	mCamera.setParameters(testParam);							//this crashes on certain phones
				        	
				        	testParam = mCamera.getParameters();
				    		if (testParam.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)) {
					
								((HashMap)mMapField.get(mParamOn)).clear();
								((HashMap)mMapField.get(mParamOff)).clear();
								((HashMap)mMapField.get(mParamOff2)).clear();
								
								Toast.makeText(StrobeLibActivity.this, "Hack works", Toast.LENGTH_SHORT).show();
								mPerformA = true;
				        	}
						} catch (Exception e) {
							Toast.makeText(StrobeLibActivity.this, "This apparently doesn\'t work on your phone", Toast.LENGTH_LONG).show();
							buttonView.setChecked(false);
							mPerformA = false;
						}
				        
				        mParamOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				        mParamOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				        mParamOff2.setFlashMode(Camera.Parameters.FLASH_MODE_ON);	//ON is still off, just a little variation
				        
				        try {
				        	mCamera.setParameters(mParamOff);
				        } catch (Exception e) {
				        	mCamera.release();
				        	mCamera = Camera.open();
				        	try {
				        		mCamera.setParameters(mParamOff2);
				        	} catch (Exception e1) {
				                Toast.makeText(StrobeLibActivity.this, "Hey, I\'m sorry, your camera is really unhappy. Email me at bcoventry77@gmail.com for help.", Toast.LENGTH_LONG).show();
				            	mCamera.stopPreview();
				            	mCamera.release();
				            	mParamOn = null;
				            	mParamOff = null;
				            	mCamera = null;
				        	}
				        }
				    	
				        mService.mCamera = mCamera;
				        mService.mParamOn = mParamOn;
				        mService.mParamOff = mParamOff;
				        mService.mParamOff2 = mParamOff2;
				        mService.startTheStrober();
				        
			        }
					
					if (!isChecked && mService != null && mCamera != null) {
						mService.stopTheStrober(false);
						mParamOn = mCamera.getParameters();
						mParamOff = mCamera.getParameters();
						mParamOff2 = mCamera.getParameters();
						
				        mParamOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				        mParamOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				        mParamOff2.setFlashMode(Camera.Parameters.FLASH_MODE_ON);	//ON is still off, just a little variation
				        mPerformA = false;
				       
				        mService.mParamOn = mParamOn;
				        mService.mParamOff = mParamOff;
				        mService.mParamOff2 = mParamOff2;
				        mService.startTheStrober();
				        
					}
					
					if (mCamera == null || mService == null) {
						buttonView.setChecked(false);
						Toast.makeText(StrobeLibActivity.this, "This only works with the LED", Toast.LENGTH_LONG).show();
						mPerformA = false;
					}
					
					if (mService != null) {
						mService.mTorch = torchSave;
						mService.mRunning = runningSave;
						mScreenPost.postDelayed(new Runnable() {
							public void run() {
								updateC(0);
							}
						}, 10);
					}
					
				}
			});
			
			((CheckBox)sv.findViewById(R.id.multi_kill_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mMultiKill = !isChecked;
					updateC(0);
				}
			});
			
			sb = (SeekBar) ll.findViewById(R.id.color_seek_bar);
			sb.setMax(362);
			sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	  			@Override
	  			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
	  				mFlashColor = progress;
					((ViewGroup)sb.getParent()).findViewById(R.id.color_sample).setBackgroundColor(getColor());
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
			
			
			builder.setView(sv);
			
			builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    				setupUI();
    			}
    		});
			

			builder.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					setupUI();
				}
			});
			
			
			break;
		case D_FAQ:
			
			builder.setTitle("FAQ");
			ListView lv = new ListView(this);
			Resources res = getResources();
			String[] questions = res.getStringArray(R.array.questions);
			String[] answers = res.getStringArray(R.array.answers);
			QASet[] qaSets = new QASet[questions.length];
			for (int iii = 0; iii < questions.length; iii++) {
				qaSets[iii] = new QASet(questions[iii], answers[iii]);
			}
			QASetAdapter qaAdapter = new QASetAdapter(this, 0, new ArrayList(Arrays.asList(qaSets)));
			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					((QASetAdapter)parent.getAdapter()).items.get(position).mExpanded^= true;
					((ArrayAdapter)parent.getAdapter()).notifyDataSetChanged();
				}
			});
			lv.setAdapter(qaAdapter);
			
			builder.setView(lv);
			builder.setNeutralButton("Ok", null);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					if (
							mBaseFrequency == 7 &&
							mFineTune == -.5 &&
							mOnLength == 69230 &&
							mMaxFrequency == 57 &&
							mFlashColor == 362
							)
						showDialog(D_KEY);
				}
			});
			
			break;
			
		case D_KEY:
			builder.setTitle("Woah");
			ll = (LinearLayout)mLayoutInflater.inflate(R.layout.key_dialog, null);
			builder.setView(ll);
			builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (((EditText)ll.findViewById(R.id.key_edit_text)).getText().toString().toLowerCase().equals("shoe fish clock dragon")) {
						Editor e = PreferenceManager.getDefaultSharedPreferences(StrobeLibActivity.this).edit();
//						e.putString(S_FRIEND, obfuscateHex(Secure.getString(getContentResolver(), Secure.ANDROID_ID)));
						e.commit();
						finish();
					}
				}
			});
			builder.setNegativeButton("Cancel", null);
			break;
			
		case D_WARNING:
			builder.setTitle("Note");
			ll = (LinearLayout)mLayoutInflater.inflate(R.layout.warning_dialog, null);
			builder.setView(ll);
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor e = mPrefs.edit();
					e.putBoolean(S_WARNING, false);
					e.commit();
					showDialog(D_HELP_LAYOVER);
				}
			});
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					showDialog(D_HELP_LAYOVER);
				}
			});
			
			break;
			
		case D_CALCULATIONS:
			builder.setTitle("Stroboscope settings");
			sv = (ScrollView)mLayoutInflater.inflate(R.layout.calculations_dialog, null);
			
			((CheckBox)sv.findViewById(R.id.use_rpm_check_box)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mUseRPM = isChecked;
					updateSliders(false, false);
				}
			});
			
			sb = (SeekBar)sv.findViewById(R.id.multiplier_seek_bar);
			sb.setMax(18);
			sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					((TextView)sv.findViewById(R.id.multiplier_text)).setText(mMultiplierStrings[progress]);
					mMultiplier = ((SeekBar)sv.findViewById(R.id.multiplier_seek_bar)).getProgress();
					updateSliders(false, false);
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
		
			((CheckBox)sv.findViewById(R.id.show_x2_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked == mShowX2)
						return;
					mShowX2 = isChecked;
					setupUI();
				}
			});
			
			sb = (SeekBar)sv.findViewById(R.id.late_flash_seek_bar);
			sb.setMax(51);
			sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					mLateThreshold = progress == 51 ? -1 : progress * 5;
					((TextView)sv.findViewById(R.id.late_flash_text)).setText(
							mLateThreshold == -1 ? "None" : Float.toString(((float)mLateThreshold)/1000) + " ms");
					updateC(0);
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
			
			sb = (SeekBar)sv.findViewById(R.id.flash_preempt_seek_bar);
			sb.setMax(100);sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					mFlashPreempt = progress * 5;
					((TextView)sv.findViewById(R.id.flash_preempt_text)).setText(Float.toString(((float)mFlashPreempt)/1000) + " ms");
					updateC(0);
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
			
			sv.findViewById(R.id.beta_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://groups.google.com/forum/#!forum/strobily-beta-testers"));
					startActivity(intent);
				}
			});

			builder.setView(sv);
			builder.setPositiveButton("Ok", null);
			break;
			
		case D_TROUBLESHOOT:
			builder.setTitle("Having torch trouble?");
			sv = (ScrollView) mLayoutInflater.inflate(R.layout.dialog_troubleshoot, null);
			
			((CheckBox)sv.findViewById(R.id.multi_kill_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mMultiKill = !isChecked;
					updateC(0);
				}
			});
			
			((CheckBox)sv.findViewById(R.id.preview_check)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mCamera != null) {
						if (mPreviewHack == isChecked)
							return;
						mPreviewHack = isChecked;
						setupUI();
					}
					else {
						Toast.makeText(StrobeLibActivity.this, "LED not found", Toast.LENGTH_SHORT).show();
						buttonView.setChecked(false);
						mPreviewHack = false;
					}
						
				}
			});
			
			builder.setView(sv);
			builder.setPositiveButton("Back", null);
			builder.setNegativeButton("Send email", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(Intent.ACTION_SEND);
					i.setType("plain/text");
					i.putExtra(Intent.EXTRA_EMAIL, new String[] {"bcoventry77@gmail.com"});
					i.putExtra(Intent.EXTRA_SUBJECT, "Auto:Strobily Troubleshoot");
					try {
						i.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body, 
								getPackageManager().getPackageInfo(getPackageName(), 0).versionName,
								getPackageManager().getPackageInfo(getPackageName(), 0).versionCode,
								Build.MODEL,
								Build.VERSION.RELEASE
								));
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
					startActivity(i);
				}
			});
			break;
			
		case D_NEW_VERSION:
			try {
				builder.setTitle("New for v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			ll = (LinearLayout)mLayoutInflater.inflate(R.layout.new_version_dialog, null);
			builder.setView(ll);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					showDialog(D_HELP_LAYOVER);
				}
			});
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(D_HELP_LAYOVER);
				}
			});
//			builder.setNeutralButton("Rate app", new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(mAds ? R.string.free_link : R.string.paid_link)));
//					startActivity(i);
//					showDialog(D_HELP_LAYOVER);
//				}
//			});
			break;
			
		case D_MORE_FEATURES:
			builder.setTitle("More Features");
			sv = (ScrollView)mLayoutInflater.inflate(R.layout.more_features, null);
			sv.findViewById(R.id.rating_layout).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.free_link)));
					startActivity(i);
				}
			});
			
			sv.findViewById(R.id.email_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(Intent.ACTION_SEND);
					i.setType("plain/text");
					i.putExtra(Intent.EXTRA_EMAIL, new String[] {"bcoventry77@gmail.com"});
					try {
						i.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body_friendly, 
								getPackageManager().getPackageInfo(getPackageName(), 0).versionName,
								getPackageManager().getPackageInfo(getPackageName(), 0).versionCode,
								Build.MODEL,
								Build.VERSION.RELEASE
								));
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
					startActivity(i);
				}
			});
			
			sv.findViewById(R.id.buy_layout).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paid_link)));
					startActivity(i);
				}
			});
			
			builder.setView(sv);
			builder.setPositiveButton("Ok", null);
			
			break;
			
		case D_HELP_LAYOVER:
			
			dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar) {
			      @Override
			      protected void onCreate(Bundle savedInstanceState) {
			            super.onCreate(savedInstanceState);
			            final Dialog dialog = this;
			            setContentView(R.layout.help_layover);
			            getWindow().setLayout(LayoutParams.FILL_PARENT,
			                     LayoutParams.FILL_PARENT);
			            findViewById(R.id.help_layover_layout).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
			            });
			      }
			     
			};
			
			return dialog;
			
		case D_BURST:
			
			builder.setTitle("Number of flashes");
			ll = (LinearLayout)mLayoutInflater.inflate(R.layout.burst_dialog, null);
			final EditText burstEditText = (EditText) ll.findViewById(R.id.burst_edit_text);
			burstEditText.setText(Integer.toString(mBurstNumber));
			builder.setView(ll);
			builder.setPositiveButton("Set", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						mBurstNumber = Integer.valueOf(burstEditText.getText().toString());
					} catch (Exception e) {
					}
					setupUI();
				}
			});
			builder.setNegativeButton("Cancel", null);
			
			break;
			
		}
		
		return builder.create();
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		switch (id) {
		case D_SETTINGS:
			((SeekBar)dialog.findViewById(R.id.max_seek_bar)).setProgress(mMaxFrequency-2);
			((SeekBar)dialog.findViewById(R.id.color_seek_bar)).setProgress(mFlashColor);
			((CheckBox)dialog.findViewById(R.id.fine_check)).setChecked(mShowFine);
			((CheckBox)dialog.findViewById(R.id.show_burst_check)).setChecked(mBurstOption);
			((CheckBox)dialog.findViewById(R.id.persist_check)).setChecked(mPersistent);
			((CheckBox)dialog.findViewById(R.id.performance_hack_check)).setChecked(mPerformA);
			((CheckBox)dialog.findViewById(R.id.multi_kill_check)).setChecked(!mMultiKill);
			dialog.findViewById(R.id.color_sample).setBackgroundColor(getColor());
			break;
		case D_CALCULATIONS:
			((CheckBox)dialog.findViewById(R.id.use_rpm_check_box)).setChecked(mUseRPM);
			((CheckBox)dialog.findViewById(R.id.show_x2_check)).setChecked(mShowX2);
			((SeekBar)dialog.findViewById(R.id.multiplier_seek_bar)).setProgress(mMultiplier);
			//((TextView)dialog.findViewById(R.id.multiplier_text)).setText(mMultiplierStrings[mMultiplier]);
			((SeekBar)dialog.findViewById(R.id.late_flash_seek_bar)).setProgress(
					mLateThreshold == -1 ? 51 : mLateThreshold/5);
			((SeekBar)dialog.findViewById(R.id.flash_preempt_seek_bar)).setProgress(mFlashPreempt/5);
			break;
		case D_TROUBLESHOOT:
			((CheckBox)dialog.findViewById(R.id.multi_kill_check)).setChecked(!mMultiKill);
			((CheckBox)dialog.findViewById(R.id.preview_check)).setChecked(mPreviewHack);
			break;
		}
		
	}
    
    
    
  
    
    private void setupUI() {
    	if (mHideAll) {
    		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
    				WindowManager.LayoutParams.FLAG_FULLSCREEN);
    		setContentView(R.layout.hide);
    	}
    	else {
    		getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    	setContentView(R.layout.main);
	    	
	    	mBaseSeek = (SeekBar) findViewById(R.id.base_seek_bar);
	    	mBaseSeek.setMax(mMaxFrequency-1);
	        mBaseSeek.incrementProgressBy(1);
	        mBaseSeek.setProgress(mBaseFrequency-1);
	        mBaseSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	  			@Override
	  			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
	  				if (!fromUser)
	  					return;
	  				mBaseFrequency = progress+1;
	  				updateSliders(true, false);
	  			}
	  			@Override
	  			public void onStartTrackingTouch(SeekBar seekBar) {}
	  			@Override
	  			public void onStopTrackingTouch(SeekBar seekBar) {}
	        });
	        
	        if (mShowFine) {
		        mFineSeek = (SeekBar) findViewById(R.id.fine_tune_seek_bar);
		        mFineSeek.setMax(40);
		        mFineSeek.incrementProgressBy(1);
		        mFineSeek.setProgress((int) ((mFineTune + 1)*20));
		        mFineSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		  			@Override
		  			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
		  				if (!fromUser)
		  					return;
		  				mFineTune = ((float)progress)/20 - 1;
		  				updateSliders(false, false);
		  			}
		  			@Override
		  			public void onStartTrackingTouch(SeekBar seekBar) {}
		  			@Override
		  			public void onStopTrackingTouch(SeekBar seekBar) {}
		        });
	        }
	          
	        mDutySeek = (SeekBar) findViewById(R.id.duty_seek_bar);
	        mDutySeek.setMax(18);
	        mDutySeek.incrementProgressBy(1);
	        mDutySeek.setProgress((int) (mDuty*20) - 1);
	        mDutySeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	  			@Override
	  			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
	  				if (!fromUser)
	  					return;
	  				mDuty = ((float)progress + 1)/20;
	  				updateSliders(false, false);
	  			}
	  			@Override
	  			public void onStartTrackingTouch(SeekBar seekBar) {}
	  			@Override
	  			public void onStopTrackingTouch(SeekBar seekBar) {}
	        });
	          
	        updateSliders(false, false);
	        
	          
	        mSteadyCheck = (CheckBox)findViewById(R.id.steady_check);
	        mSteadyCheck.setChecked(mSteady);
	        mSteadyCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	
	  			@Override
	  			public void onCheckedChanged(CompoundButton b, boolean checked) {
	  				mSteady = checked;
	  				updateSliders(false, false);
	  				updateC(0);
	  				
	  			}
	
	        });
	        

			findViewById(R.id.menu_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					openOptionsMenu();
				}
			});
	          
	        findViewById(R.id.save_length).setOnClickListener(new OnClickListener() {
	
	  			@Override
	  			public void onClick(View v) {
	  				mOnLength = calculateTime(true);
	  				updateSliders(false, false);
	  				updateC(0);

	  			}
	          	
	        });
	        ((Button)findViewById(R.id.save_length)).setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					mOnLength = 0;
					updateSliders(false, false);
					updateC(0);
					return true;
				}
	        });
	          
	          
	        mUseScreenCheck = (CheckBox)findViewById(R.id.use_screen);
	        mUseScreenCheck.setChecked(mUseScreen);
	        mUseScreenCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	
	  			@Override
	  			public void onCheckedChanged(CompoundButton b, boolean checked) {
	  				mUseScreen^= true;
	  				if (mCamera == null) {
	  					mUseScreen = true;
	  					b.setChecked(true);
	  					Toast.makeText(StrobeLibActivity.this, "LED not found", Toast.LENGTH_LONG).show();
	  				}
	  				updateC(0);
	  				setProperBrightness();
	  				
	  			}
	        });
	     
	        mSupeDimCheck = (CheckBox)findViewById(R.id.supe_dim);
	        mSupeDimCheck.setChecked(mSupeDim);
	        mSupeDimCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	  			@Override
	  			public void onCheckedChanged(CompoundButton b, boolean checked) {
	  				mSupeDim^= true;
	  				setProperBrightness();
	  			}
	        });
	        
	        mAirplaneCheck = (CheckBox)findViewById(R.id.airplane_check);
	    	mAirplaneCheck.setChecked(mAirplane);
	    	mAirplaneCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	  			@Override
	  			public void onCheckedChanged(CompoundButton b, boolean checked) {
	  				if (Build.VERSION.SDK_INT >= 17) {
	  					Toast.makeText(StrobeLibActivity.this, "Apparently Google disabled this in 4.2", Toast.LENGTH_SHORT).show();
	  				}
	  				mAirplane^= true;
	  				
	  				setAirplaneMode(mAirplane);
	  			}
	        });
	    	
	    	if (!mShowFine) {
	    		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)findViewById(R.id.duty_text).getLayoutParams();
	    		params.addRule(RelativeLayout.BELOW, R.id.base);
	    		RelativeLayout rl = (RelativeLayout)findViewById(R.id.relative_layout);
	    		View v;
	    		
	    		for (int iii = 0; (v = rl.getChildAt(iii)) != null; iii++) {
	    			if (v.getId() == R.id.fine_tune_text)
	    				rl.removeViews(iii, 4);
	    			
	    		}
	    		
	    	}
	    	

	    	Button button = (Button) findViewById(R.id.toggle_running);
	    	
	        button.setOnClickListener(new OnClickListener() {
	        	
	  			@Override
	  			public void onClick(View v) {
	  				if (mService != null) {
		  				mService.mRunning^= true;
		  				mService.mTorch = false;
	  				}
	  				setProperBrightness();
	  				updateC(-1);
	  			}
	          	
	        });
	        button.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (mService != null) {
						mService.mTorch^= true;
						mService.mRunning = false;
					}
					setProperBrightness();
					updateC(-1);
					return true;
				}
	        });
	        
	        button = (Button) findViewById(R.id.do_burst);
	        button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mService != null) {
						mService.mTorch = false;
						mService.mRunning = false;
					}
					setProperBrightness();
					updateC(mBurstNumber);
				}
	        });
	        
	        button.setOnLongClickListener(new OnLongClickListener() {
	        	@Override
	        	public boolean onLongClick(View v) {
	        		showDialog(D_BURST);
	        		return true;
	        	}
	        });
	        
	        button.setVisibility(mBurstOption ? View.VISIBLE : View.GONE);
	        button.setText("   Burst (" + Integer.toString(mBurstNumber) + ")   ");
	    	
	    	findViewById(R.id.x2_layout).setVisibility(mShowX2 ? View.VISIBLE : View.GONE);
	    	
	    	findViewById(R.id.times_two_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					float nextFrequency = (mBaseFrequency + mFineTune) * 2;
					if (nextFrequency > mMaxFrequency + 1) {
						return;
					}
					nextFrequency = ((float)((int)(nextFrequency*100)))/100;
					mBaseFrequency = (int)nextFrequency;
					mFineTune = nextFrequency - mBaseFrequency;
					if (mBaseFrequency > mMaxFrequency) {
						mBaseFrequency--;
						mFineTune+= 1.0;
					}
					if (mBaseFrequency == 0)
						mBaseFrequency = 1;
					updateSliders(false, true);
					
				}
	    	});
	    	
	    	findViewById(R.id.times_half_button).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					float nextFrequency = (mBaseFrequency + mFineTune) / 2;
					nextFrequency = ((float)((int)(nextFrequency*100)))/100;
					mBaseFrequency = (int)nextFrequency;
					mFineTune = nextFrequency - mBaseFrequency;
					if (mBaseFrequency == 0) {
						mBaseFrequency = 1;
						mFineTune-= 1;
					}
					updateSliders(false, true);
					
				}
	    	});
	    	

			findViewById(R.id.lag_warning).setVisibility(mLagging ? View.VISIBLE : View.INVISIBLE);
	        
	        
	    	FrameLayout fl = (FrameLayout) findViewById(R.id.preview_holder);
	    	if (mCameraSurface != null) {
		    	ViewGroup parent = (ViewGroup) mCameraSurface.getParent();
		    	if (parent != null)
		    		parent.removeAllViews();
		    	if (mPreviewHack)
		    		fl.addView(mCameraSurface);
	    	}
		    	
	    	findViewById(R.id.main_linear_layout).invalidate();
		    	
	    	
    	}
    	
    	mHideAllCheck = (CheckBox)findViewById(R.id.hide_all);
    	mHideAllCheck.setChecked(mHideAll);
    	mHideAllCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
  			@Override
  			public void onCheckedChanged(CompoundButton b, boolean checked) {
  				mHideAll^= true;
  				setupUI();
  			}
        });
    	
    	LinearLayout adHolder = (LinearLayout)findViewById(R.id.ad_holder);
    	

    	

        mRoot = findViewById(R.id.main_linear_layout);
    	
    	
    	
    	
    }
    
    private void setAirplaneMode(boolean enabled) {
    	try {
	    	Settings.System.putInt(this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enabled ? 1 : 0);
	    	Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	    	intent.putExtra("state", enabled);
	    	sendBroadcast(intent);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    private void setWakeLock(boolean on) {
		if (on) {
			if (!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
    	}
    	else {
    		if (mWakeLock.isHeld()) {
    			mWakeLock.release();
    		}
    	}
	}

	
    
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent key) {
    	int keyCode = key.getKeyCode();
    	int action = key.getAction();
    	int repeatCount = key.getRepeatCount();
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_VOLUME_DOWN:
    		if (action == KeyEvent.ACTION_DOWN) {
    			decreaseFine();
    		}
    		if (action == KeyEvent.ACTION_MULTIPLE) {
    			for (int iii = 0; iii < repeatCount; iii++)
    				decreaseFine();
    		}
    		return true;
    		
    	case KeyEvent.KEYCODE_VOLUME_UP:
    		if (action == KeyEvent.ACTION_DOWN) {
    			increaseFine();
    		}
    		if (action == KeyEvent.ACTION_MULTIPLE) {
    			for (int iii = 0; iii < repeatCount; iii++)
    				increaseFine();
    		}
    		return true;
    		
    	}
    	
  
    	
    	return super.dispatchKeyEvent(key);
    }
    
    private void increaseFine() {
    	if (mBaseFrequency + mFineTune >= mMaxFrequency + 1)
    		return;
    	mFineTune += .05f;
    	if (mFineTune > 1.01) {
    		mFineTune--;
    		mBaseFrequency++;
    	}
    	updateSliders(false, true);
    }
    
    private void decreaseFine() {
    	if (mBaseFrequency + mFineTune <= 0)
    		return;
    	mFineTune -= .05f;
    	if (mFineTune < -1.01) {
    		mFineTune++;
    		mBaseFrequency--;
    	}
    	updateSliders(false, true);
    }
    
    private void setProperBrightness() {
    	if (mService == null) {
    		setScreenBrightness(-1);
    		return;
    	}
    	if (mService.mRunning) {
    		if (mUseScreen)
    			setScreenBrightness(1);
    		else {
    			if (mSupeDim)
    				setScreenBrightness(.005f);
    			else
    				setScreenBrightness(-1);
    		}
    	}
    	else {
    		if (mService.mTorch && mUseScreen) {
    			setScreenBrightness(1);
    		}
    		else
    			setScreenBrightness(-1);
    	}

    }
    
    
    private int getColor() {
    	if (mFlashColor == 0 || mFlashColor == 362)
    		return Color.WHITE;
    	return Color.HSVToColor(new float[] {mFlashColor-1, 1, 1});
    }
 
    
    private void setScreenBrightness(float bright) {
    	WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = bright;
		getWindow().setAttributes(lp);
    }
    
    
    
    private boolean setupCameraStuff() {
    	
    	try {
    		mCamera = Camera.open();
    	} catch (Exception e) {
    		Toast.makeText(this, "Could not connect to camera", Toast.LENGTH_LONG).show();
    		return false;
    	}
        if (mCamera == null) {
        	Toast.makeText(this, "Camera not detected", Toast.LENGTH_LONG).show();
        	return false;
        }
  
//        if (mPreviewHack) {
//	        try {
//				mCamera.setPreviewDisplay(mCameraSurface.getHolder());
//			} catch (Exception e) {
//				Toast.makeText(this, "Preview display failed (error)", Toast.LENGTH_LONG).show();
//				e.printStackTrace();
//			}
//        }
        
        try {
        	mCamera.startPreview();
        } catch (Exception e) {
        	Toast.makeText(this, "Failed to start preview (camera glitch)", Toast.LENGTH_LONG).show();
        	e.printStackTrace();
        	mCamera = null;
        	return false;
        }
        if (mFocusHack) {
        	try {
				mCamera.autoFocus(new AutoFocusCallback() {
		            public void onAutoFocus(boolean success, Camera camera) {
		            }
				});
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        mParamOn = mCamera.getParameters();
        mParamOff = mCamera.getParameters();
        mParamOff2 = mCamera.getParameters();
        Camera.Parameters testParam = mCamera.getParameters();
        if (mParamOn == null) {
        	Toast.makeText(this, "No parameters (camera error)", Toast.LENGTH_LONG).show();
        	mCamera.release();
        	mCamera = null;
        	return false;
        }
        
        List<String> flashModes = mParamOn.getSupportedFlashModes();
        if (flashModes == null) {
        	Toast.makeText(this, "Camera flash not detected", Toast.LENGTH_LONG).show();
        	mCamera.stopPreview();
        	mCamera.release();
        	mParamOn = null;
        	mParamOff = null;
        	mCamera = null;
        	return false;
        }
        
        if (!flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
        	Toast.makeText(this, "Flashlight mode not supported", Toast.LENGTH_LONG).show();
        	mCamera.stopPreview();
        	mCamera.release();
        	mParamOn = null;
        	mParamOff = null;
        	mCamera = null;
        	return false;
        }
        
        if (mPerformA) {
	        testParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
	        boolean failed = false;
	        try {
	        	Field mMapField = null;
	        	mMapField = Camera.Parameters.class.getDeclaredField("mMap");
	        	mMapField.setAccessible(true);
	        	((HashMap)mMapField.get(testParam)).clear();
	        	testParam.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
	        	mCamera.setParameters(testParam);							//this crashes on certain phones
	        	
	        	testParam = mCamera.getParameters();
	    		if (testParam.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON) && !failed) {
		
					((HashMap)mMapField.get(mParamOn)).clear();
					((HashMap)mMapField.get(mParamOff)).clear();
					((HashMap)mMapField.get(mParamOff2)).clear();
	        	}
			} catch (Exception e) {
			}
        }
        
    	
        mParamOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mParamOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mParamOff2.setFlashMode(Camera.Parameters.FLASH_MODE_ON);	//ON is still off, just a little variation
        
        try {
        	mCamera.setParameters(mParamOff);
        } catch (Exception e) {
        	mCamera.release();
        	mCamera = Camera.open();
        	try {
        		mCamera.setParameters(mParamOff2);
        	} catch (Exception e1) {
                Toast.makeText(this, "Hey, I\'m sorry, your camera is really unhappy. Email me at bcoventry77@gmail.com for help.", Toast.LENGTH_LONG).show();
            	mCamera.stopPreview();
            	mCamera.release();
            	mParamOn = null;
            	mParamOff = null;
            	mCamera = null;
            	return false;
        	}
        }
    	
        
        return true;
    	
    	
    }
    
    
    
    
    
    // this routine has a shadow in the service
    private int calculateTime(boolean on) {
    	mScr2 = (long) ((mBaseFrequency + mFineTune) * 100);
    	if (mScr2 < 1)
    		mScr2 = 1;
    	
    	if (mSteady) {
			mScr = (long) (100000000 / mScr2);
			mScr2 = mOnLength;
			if (mScr2 >= mScr)
				mScr2 = mScr-1;
			if (on)
				mScr = mScr2;
			else
				mScr = mScr - mScr2;
		}
		else {
			mScr = (long) (100000000 * (on ? mDuty : 1-mDuty)/mScr2);
		}
		
    	if (mScr < 1000 && !on && (mOnLength != 0))
    		mScr = 1;
    	return (int) mScr;
    	
    	
    }
    
    
    private void updateSliders(boolean resetFine, boolean moveSliders) {
    	if (resetFine) {
    		mFineTune = 0;
    		if (mShowFine)
    			mFineSeek.setProgress((int) ((mFineTune+1) * 20));
    	}
    	
    	if (!mHideAll) {
    		float factor = mMultipliers[mMultiplier] * (mUseRPM ? 60 : 1);
    		String suffix = (mUseRPM ? " RPM" : " Hz") + (mMultipliers[mMultiplier] != 1 ?
    				" (x" + mMultiplierStrings[mMultiplier] + ")" : "");
    		
    		((TextView)findViewById(R.id.base)).setText(formatFrequency(mBaseFrequency * factor, mMultipliers[mMultiplier] >= 1) + suffix);
    				
	    	if (mShowFine) {
		    	((TextView)findViewById(R.id.fine_tune_low)).setText(formatFrequency((mBaseFrequency-1)*factor, true));
		    	((TextView)findViewById(R.id.fine_tune_high)).setText(formatFrequency((mBaseFrequency+1)*factor, true));
		    	((TextView)findViewById(R.id.fine_tune)).setText(formatFrequency((mBaseFrequency+mFineTune)*factor, false) + suffix);
	    	}
	    	long onTime = calculateTime(true);
	    	long offTime = calculateTime(false);
	    	
	    	((TextView)findViewById(R.id.duty)).setText(Integer.toString((int)(mDuty*100)) + "%");
	    	((TextView)findViewById(R.id.cur_off_length)).setText(displayTime(offTime, onTime + offTime < 100000));
	    	((TextView)findViewById(R.id.cur_on_length)).setText(displayTime(onTime, onTime + offTime < 100000));
	    	((TextView)findViewById(R.id.on_length)).setText(displayTime(mOnLength, mOnLength < 100000));
    	}
    	
    	if (moveSliders) {
			if (mShowFine) {
				mFineSeek.setProgress((int) ((mFineTune + 1) * 20));
			}
    		mBaseSeek.setProgress(mBaseFrequency - 1);
    	}
    	
    	updateC(0);
    	
    	
    }
    
    private String displayTime(long micros, boolean decimal) {
    	String toReturn = "";
    	if (decimal) {
    		toReturn = Float.toString(((float)((int)(micros / 100))) / 10f);
    	}
    	else {
    		toReturn = Long.toString(micros / 1000);
    	}
    	
    	return toReturn + " ms";
    }
    
    
    private class QASet {
    	
    	public String mQuestion = "";
    	public String mAnswer = "";
    	public boolean mExpanded = false;
    	
    	public QASet(String question, String answer) {
    		mQuestion = question;
    		mAnswer = answer;
    	}
    	
    }
    
	private class QASetAdapter extends ArrayAdapter<QASet> {
		
		private ArrayList<QASet> items;
		
		public QASetAdapter(Context context, int textViewResourceId, ArrayList<QASet> list) {
			super(context, textViewResourceId, list);
			this.items = list;		
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			return makeQAItem(items.get(position));
		}
		
		
		
	}
	
	View makeQAItem(QASet set) {
		View v = mLayoutInflater.inflate(set.mExpanded ? R.layout.qa_item_expanded : R.layout.qa_item, null);
		
		((TextView)v.findViewById(R.id.question)).setText(set.mQuestion);
		if (set.mExpanded)
			((TextView)v.findViewById(R.id.answer)).setText(set.mAnswer);
		return v;
	}
    
    
    
    private String formatFrequency(float frequency, boolean round) {
    	if (round) {
    		return Integer.toString((int)(frequency));
    	}
    	return Float.toString(((float)Math.round(frequency*100))/100);
    }
    
    
    private void setFlashState(boolean state) {
    	if (state)
    		flashOn();
    	else
    		flashOff();
    	
    }
    
    private void flashOff() {
    	if (mUseScreen) {
    		mRoot.setBackgroundColor(Color.BLACK);
    	}
    	else {
	    	if (mCamera != null)
	    		mCamera.setParameters(mParamOff);
    	}
    }
    
    public void flashOn() {
    	if (mUseScreen) {
    		mRoot.setBackgroundColor(getColor());
    	}
    	else {
	    	if (mCamera != null)
	    		mCamera.setParameters(mParamOn);
    	}
    }
    
    
    private void updateC(int burst) {
    	if (mService != null) {
    		mService.mBaseFrequency = mBaseFrequency;
    		mService.mFineTune = mFineTune;
    		mService.mDuty = mDuty;
    		mService.mOnLength = mOnLength;
    		mService.mSteady = mSteady;
    		mService.mUseScreen = mUseScreen;
    		mService.mLateThreshold = mLateThreshold;
    		mService.mFlashPreempt = mFlashPreempt;
    		mService.mMultiKill = mMultiKill;
    		mService.updateStrober(burst);
    		setWakeLock(mService.mRunning || mService.mTorch);
    	}
    	else {
    		setWakeLock(false);
    	}
    }
    
    
	private ServiceConnection mTheServiceConnection = new ServiceConnection() {
	    	
    	@Override
    	public void onServiceConnected(ComponentName className, IBinder iBinder) {
    		mService = ((LocalBinder)iBinder).getService();    	
    		
    		mService.endForeground();
    		
    		mService.mBaseFrequency = mBaseFrequency;
    		mService.mFineTune = mFineTune;
    		mService.mDuty = mDuty;
    		mService.mOnLength = mOnLength;
    		mService.mSteady = mSteady;
    		mService.mUseScreen = mUseScreen;
    		mService.mScreenPost = mScreenPost;
    		
    		if (mService.mCamera == null) {
    			if (!setupCameraStuff()) {
    				mUseScreen = true;	
    				setupUI();
    			}
	    		mService.mCamera = mCamera;
	    		mService.mParamOn = mParamOn;
	    		mService.mParamOff = mParamOff;
	    		mService.mParamOff2 = mParamOff2;
    		}
    		else {
    			mCamera = mService.mCamera;
    			mParamOn = mService.mParamOn;
    			mParamOff = mService.mParamOff;
    			mParamOff2 = mService.mParamOff2;
    		}
    		
    		if (mService.mStroberThread == null) {
    			mService.mRunning = false;
    			mService.mTorch = false;
    			mService.startTheStrober();
    		}
    		
    		setWakeLock(mService.mRunning || mService.mTorch);
    		
    	}
    	
    	@Override
    	public void onServiceDisconnected(ComponentName className) {
    		mService = null;
    	}
    };

//    private static boolean isServiceRunning(Context context) {
//    	ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
//    	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//    		if ("com.tp77.StrobeLib.StrobeLibService".equals(service.service.getClassName())) {
//    				return true;
//    		}
//    	}
//    	return false;
//    }
//    
    
    
    
//    
//    public static String obfuscateHex(String str) {
//    	char[] input = str.toCharArray();
//    	for (int iii = 0; iii < str.length(); iii++)
//    		input[iii] = getNewChar(input[iii]);
//    	return new String(input);
//    }
//    
//    private static char getNewChar(char input) {
//    	if (input >= '0' && input <= '9') 
//    		return (char) (input-'0'+'A');
//    	
//    	if (input >= 'A' && input <='Z')
//    		return (char) (input-'A'+'K');
//    	
//    	if (input >= 'a' && input <='z')
//    		return (char) (input-'a'+'K');
//    	
//    	return 'Z';
//    	
//    }
    
   
    
}