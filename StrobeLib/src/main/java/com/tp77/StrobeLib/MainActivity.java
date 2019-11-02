package com.tp77.StrobeLib;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import com.tp77.StrobeLib.StrobeService.LocalBinder;
import com.tp77.StrobeLib.TitleScroller.OnTitleClickListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v42.app.Fragment;
import android.support.v42.app.FragmentActivity;
import android.support.v42.app.FragmentManager;
import android.support.v42.app.FragmentPagerAdapter;
import android.support.v42.app.FragmentTransaction;
import android.support.v42.view.PagerAdapter;
import android.support.v42.view.ViewPager;
import android.support.v42.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	public static final String P_VERSION_SEEN = "versionSeen";
	public static final String P_SEEN_BETA = "seen_beta";
	public static final String P_NEW_CAMERA = "new_camera";
	public static final String P_OPEN_HACK = "open_hack";
	public static final String P_TUTORIAL_RESET = "tutorial_reset";
	public static final String P_DIAGNOSTIC_SET = "diagnostic_set";

	public static final String P_OPENS = "opens";
	public static final String P_RATED = "rated";
	
	public static final int S_STROBE = 0;
	public static final int S_TACHO = 1;
	public static final int S_OTHER = 2;
	public static final int S_WIDGET = 3;
	
	public StrobeService mService = null;
	
	public static final int F_DIAGNOSTIC = -1;
	
	public static final int F_ADVANCED = 0;
	public static final int F_SETTINGS = 1;
	public static final int F_STROBE = 2;
	public static final int F_MUSIC = 3;
	public static final int F_TACHO = 4;
	public static final int F_PATTERN = 5;
	public static final int F_WIDGET = 6;
	public static final int F_TURNTABLE = 7;
	public static final int F_ABOUT = 8;
	public static final int F_HELP = 9;
	
	public static final int D_UPDATE_FREQ = 0;
	
	
	
	public static final String[] TITLES_W_DIAGNOSTIC = new String[] {
			"Advanced",
			"Settings",
			"Diagnostic",
			"Strobe",
			"Music",
			"Tacho",
			"Pattern",
			"Widget",
			"Turntable",
			"About",
			"Help"
	};
	
	public static final String[] TITLES = new String[] {
		"Advanced",
		"Settings",
		"Strobe",
		"Music",
		"Tacho",
		"Pattern",
		"Widget",
		"Turntable",
		"About",
		"Help"
	};
	
	private MyFragment[] mFragments = new MyFragment[TITLES_W_DIAGNOSTIC.length];
	
	
	public static final String P_WHICH_SCREEN = "whichScreen";
	public static final String P_USE_LED = "useLed";
	public static final String P_USE_SCREEN = "useScreen";
	
	public static final String P_TORCH_ON = "torchOn";
	public static final String P_WIDGET_RUNNING = "widgetRunning";
	public static final String P_LAST_WIDGET = "lastWidget";
	
	public static final String P_SAVED_INSTALL = "uuid";
	
	private LayoutInflater mLayoutInflater;
	private PowerManager mPowerManager;
	private WakeLock mWakeLock;
	private SharedPreferences mPrefs;
	
	private MyAdapter mAdapter = null;
	private ViewPager mPager = null;

	private boolean mHidden = false;
	
	private boolean mPaused = false;
	
	private View mRoot = null;
	
	private SurfaceView mCameraSurface = null;
	private boolean mSurfaceEmbedded = false;
	
	private int mScreenColor = 0;
	
	private TitleScroller mTitleScroller;
	
	public Handler mBeater = null;
	public Handler mBpmUpdater = null;
	
	public boolean mAds = true;
	private boolean mOverride = false;
	
	private int mLastFragIndex = -1;
	private int mStartThisFragsHandlers = -1;
	
	private boolean mWidgetAppeared = false;	// if true, we need to start the widget
	private String mWidgetPattern = null;		// != null is flag for widget mode
	private boolean mWidgetLoop = false;
	private boolean mWidgetScreen = false;
	private boolean mWidgetLed = false;
	
	private boolean mFromNewIntent = false;

	private boolean mSettingUpCameraStuff = false;
	private boolean mAwaitingNewCamera = false;

	private static final int REQUEST_CAMERA_PERMISSION = 1;
	private static final int REQUEST_MICROPHONE_PERMISSION = 2;
	
	public boolean mRequestedCamera = false;
	
	public OnPageChangeListener mopcl;
	
	private int mServicePostNum = 0;
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		this.setIntent(intent);
		mFromNewIntent = true;
		onCreate(null);
	}
	
	@Override
	public void onCreate(Bundle bundle) {
		if (!mFromNewIntent)	
			super.onCreate(bundle);
		mFromNewIntent = false;
		
        mAds = getPackageName().contains("Ad");

		mPrefs =  PreferenceManager.getDefaultSharedPreferences(this);
		Editor e3 = mPrefs.edit();
		e3.putInt(SettingsFragment.P_FLASH_COLOR, 1);
		MainActivity.apply(e3);
		Toast.makeText(this, "Color set to red", Toast.LENGTH_SHORT).show();
		
		mLayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "com.tp77.StrobeLib:Main app flasher");


		if ( ! mPrefs.getBoolean(P_DIAGNOSTIC_SET, false ) ) {
			Editor e = mPrefs.edit();
			e.putBoolean(P_DIAGNOSTIC_SET, true);
			e.putBoolean(AdvancedFragment.P_DIAGNOSTIC, true);
			e.putBoolean(DiagnosticFragment.P_SHOW_LED_STATE, true);
			apply(e);
		}



		setContentView(R.layout.act_main_activity);
		
		Bundle widgetBundle = getIntent().getBundleExtra(getString(R.string.S_WIDGET));
		mWidgetAppeared = false;
		if (widgetBundle == null) {
			Intent intent = getIntent();
			if (intent.hasExtra(WidgetFragment.W_PATTERN)) {
				widgetBundle = new Bundle();
				widgetBundle.putString(WidgetFragment.W_PATTERN, intent.getStringExtra(WidgetFragment.W_PATTERN));
				widgetBundle.putBoolean(WidgetFragment.W_LOOP, intent.getBooleanExtra(WidgetFragment.W_LOOP, false));
				widgetBundle.putBoolean(WidgetFragment.W_LED, intent.getBooleanExtra(WidgetFragment.W_LED, false));
				widgetBundle.putBoolean(WidgetFragment.W_SCREEN, intent.getBooleanExtra(WidgetFragment.W_SCREEN, false));
			}
		}
		if (widgetBundle != null) {
			mWidgetAppeared = true;
			mWidgetPattern = widgetBundle.getString(WidgetFragment.W_PATTERN);
			mWidgetLoop = widgetBundle.getBoolean(WidgetFragment.W_LOOP, true);
			mWidgetScreen = widgetBundle.getBoolean(WidgetFragment.W_SCREEN, true);
			mWidgetLed = widgetBundle.getBoolean(WidgetFragment.W_LED, true);
			
			Editor ed = mPrefs.edit();
			ed.putString(P_LAST_WIDGET, mWidgetPattern);
			MainActivity.apply(ed);
		} else {
			mWidgetPattern = null;
			if (mPrefs.getBoolean(P_WIDGET_RUNNING, false)) {
				mWidgetPattern = mPrefs.getString(P_LAST_WIDGET, "");
			}
		}
		if (mWidgetPattern != null)
			((TextView)findViewById(R.id.widget_name)).setText(mWidgetPattern);
		

		findViewById(R.id.normal).setVisibility(mWidgetPattern == null ? View.VISIBLE : View.GONE);
		findViewById(R.id.widget).setVisibility(mWidgetPattern != null ? View.VISIBLE : View.GONE);
		
		FrameLayout adHolder = (FrameLayout)findViewById(R.id.ad_holder);
		
		

		mTitleScroller = (TitleScroller)findViewById(R.id.title_scroller);
		mRoot = findViewById(R.id.root);
		mPager = (ViewPager)findViewById(R.id.view_pager);
		mLastFragIndex = -1;

		mAdapter = new MyAdapter(getSupportFragmentManager());
		mPager.setAdapter(mAdapter);
		
		setupScroller(false);
		

		int screen = mPrefs.getInt(P_WHICH_SCREEN, F_STROBE+1);
		mPager.setCurrentItem(screen);
		mTitleScroller.setSelected(screen);
		mopcl = new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int state) {
			}
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				mTitleScroller.setPosition(position, positionOffset);
			}
			@Override
			public void onPageSelected(int position) {
				mTitleScroller.setSelected(position);
				if (mLastFragIndex != -1) {
					MyFragment frag = mFragments[mLastFragIndex];
					if (frag != null) {
						frag.stopHandlers();
					}
				}
				mLastFragIndex = position;

				MyFragment frag = mFragments[mLastFragIndex];
				if (frag != null) {
					frag.startHandlers();
				} else {
					mStartThisFragsHandlers = position;
				}
			}
		};
		mopcl.onPageSelected(screen);

		mPager.setOnPageChangeListener(mopcl);
		
		mTitleScroller.setListener(new OnTitleClickListener() {
			@Override
			public void onTitleClick(int position) {
				mPager.setCurrentItem(position, true);
			}
		});
		

		findViewById(R.id.stop).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopFlashing();
				finish();
			}
		});

		findViewById(R.id.led_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleLedScreenPress(true, false);	
			}
		
		});


		findViewById(R.id.screen_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleLedScreenPress(false, false);	
			}
		
		});
		
		
		
		findViewById(R.id.torch_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setTorchOn(!mPrefs.getBoolean(P_TORCH_ON, false), MainActivity.this);
				setTorchCheck();
				
				if (mPrefs.getBoolean(P_TORCH_ON, false)) {
					torchOn();
				} else {
					torchOff();
				}
				
			}
		});
		
		setTorchCheck();
		setBackground(null);
		
		
		findViewById(R.id.hide_check).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mHidden ^= true;
				handleHide();
			}
		});
		
		if (!useLed() && !useScreen()) {	// not really sure how this happens
//			setUseLed(true);
//			setLedScreenCheck();
			handleLedScreenPress(false, true);
		}



		if ( ! mWidgetAppeared ) {
			int opens = mPrefs.getInt(P_OPENS, 0);
			opens++;
			{
				Editor e = mPrefs.edit();
				e.putInt(P_OPENS, opens);
				apply(e);
			}

			if ( ! mPrefs.getBoolean(P_TUTORIAL_RESET, false ) ) {
				Editor e = mPrefs.edit();
				e.putBoolean(P_TUTORIAL_RESET, true);
				e.putBoolean(TutorialDialog.P_TUTORIAL_FINISHED, false);
				apply(e);
			}


			if (!TutorialDialog.isTutorialFinished(this))
				TutorialDialog.showDialog(TutorialDialog.N_INTRO, -1, this);
			else {

				int versionSeen = mPrefs.getInt(P_VERSION_SEEN, 0);
				int thisVersion;
				try {
					thisVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
				} catch (Exception e) {
					e.printStackTrace();
					thisVersion = 0;
				}

				boolean showing_dia = false;
				if (versionSeen != thisVersion) {
					Editor e = mPrefs.edit();
					e.putInt(P_VERSION_SEEN, thisVersion);
					apply(e);
					EveryDialog.showDialog(EveryDialog.N_UPDATE, this);
					showing_dia = true;
				}

				if (!showing_dia && opens % 12 == 0 && !mPrefs.getBoolean(P_RATED, false)) {
					EveryDialog.showDialog(EveryDialog.N_RATE, this);
				}
				//			if ( !showing_dia && ! mPrefs.getBoolean(P_SEEN_BETA, false) ) {
				//				EveryDialog.showDialog(EveryDialog.N_BETA, this);
				//			}

			}
		}
		
	}
	
	
	public void setupScroller(boolean refresh) {
		mTitleScroller.setTitles(getTitles());

		if (!refresh)
			return;
		
		FragmentManager sfm = getSupportFragmentManager();
		FragmentTransaction t = sfm.beginTransaction();
		List<Fragment> frags = getSupportFragmentManager().getFragments();
		for (Fragment frag : frags) {
			if (frag == null)
				continue;
			t.remove(frag);
			
		}
		t.commit();

		refreshFragments();
		
	}
	
	public String[] getTitles() {
		if (mPrefs.getBoolean(AdvancedFragment.P_DIAGNOSTIC, false))
			return TITLES_W_DIAGNOSTIC;
		else
			return TITLES;
	}
	
	public void setPage(int page) {
		mPager.setCurrentItem(page);
		mTitleScroller.setSelected(page);
		mopcl.onPageSelected(page);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mPaused = false;
		
		int screen = mPrefs.getInt(P_WHICH_SCREEN, F_STROBE+1);
		mPager.setCurrentItem(screen);
		mTitleScroller.setSelected(screen);
		mopcl.onPageSelected(screen);
		
		
		
		// This is a workaround for a bug in Oreo
		// onResume gets called when the app is still in background
		Intent intent = new Intent(MainActivity.this, StrobeService.class);
		try {
			startService(intent);
		} catch (Exception e) {
//			Toast.makeText(this, "Sorry! Please restart the app", Toast.LENGTH_SHORT).show();
		}
		bindService(intent, mTheServiceConnection, Context.BIND_AUTO_CREATE);

		updateScreenColor();
		setProperBrightness();
		setLedScreenCheck();
		handleHide();
	}
	
	
	@Override
	public void onPause() {
		super.onPause();

		mServicePostNum++;
		Editor e = mPrefs.edit();
		e.putInt(P_WHICH_SCREEN, mPager.getCurrentItem());
		apply(e);
		
		boolean persistent = mPrefs.getBoolean(SettingsFragment.P_PERSIST, true);
		
		if (mService == null || !persistent || !useLed() 
				|| !(mService.mFlashing || mService.mTorch || mService.mMusicing)) {
			Camera c = null;
			if (mService != null) {
				c = mService.mCamera;
				mService.stopTheStrober(false);
				mService.mBound = false;
				mService.mCamera = null;
				mService.stopSelf();
				unbindService(mTheServiceConnection);
				mService = null;
			}
			
			if (c != null) {
				try {
					c.stopPreview();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				c.release();
				
			}
			
		} else {
			mService.goForeground();
			unbindService(mTheServiceConnection);
		}
		mPaused = true;
		
		setWakeLock(false);
		setScreenBrightness(-1);
		
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
	}
	
	@Override
    public boolean dispatchKeyEvent(KeyEvent key) {
    	int keyCode = key.getKeyCode();
    	int action = key.getAction();
    	int repeatCount = key.getRepeatCount();
    	
    	MyFragment mVolumeHandler = mFragments[mPager.getCurrentItem()];
    	
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_VOLUME_DOWN:
    		if (action == KeyEvent.ACTION_DOWN) {
    			mVolumeHandler.onVolumeDown();
    		}
    		if (action == KeyEvent.ACTION_MULTIPLE) {
    			for (int iii = 0; iii < repeatCount; iii++)
        			mVolumeHandler.onVolumeDown();
    		}
    		return true;
    		
    	case KeyEvent.KEYCODE_VOLUME_UP:
    		if (action == KeyEvent.ACTION_DOWN) {
    			mVolumeHandler.onVolumeUp();
    		}
    		if (action == KeyEvent.ACTION_MULTIPLE) {
    			for (int iii = 0; iii < repeatCount; iii++)
        			mVolumeHandler.onVolumeUp();
    		}
    		return true;
    		
    	}
    	
  
    	
    	return super.dispatchKeyEvent(key);
    }
	
	
	
	
	public void applyPreview() {
		if (mPrefs.getBoolean(SettingsFragment.P_PREVIEW_HACK, false) && 
				!mPrefs.getBoolean(P_NEW_CAMERA, false ) ) {
			
			if (mCameraSurface == null) {
				mCameraSurface = new SurfaceView(this);
				SurfaceHolder holder = mCameraSurface.getHolder();
				holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				holder.addCallback(new SurfaceHolder.Callback() {
					@Override
					public void surfaceCreated(SurfaceHolder holder) {
					}

					@Override
					public void surfaceChanged(SurfaceHolder holder,
							int format, int width, int height) {
						if (holder.getSurface() == null) {
							return;
						}
						if (mService != null && mService.mCamera != null) {
							Camera c = mService.mCamera;
							try {
								c.stopPreview();
							} catch (Exception e) {
								e.printStackTrace();
							}
							try {
								c.setPreviewDisplay(holder);
							} catch (Exception e) {
								e.printStackTrace();
								try {
									c.setPreviewDisplay(null);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}
							try {
								c.startPreview();
							} catch (Exception e) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(MainActivity.this, "Wtf? Email me if you can reproduce this",
												Toast.LENGTH_SHORT).show();
									}
								});
							}
						}
					}

					@Override
					public void surfaceDestroyed(SurfaceHolder holder) {
						if (mService != null && mService.mCamera != null) {
							Camera c = mService.mCamera;
							try {
								c.stopPreview();
							} catch (Exception e) {
								e.printStackTrace();
							} 
							try {
								c.setPreviewDisplay(null);
							} catch (Exception e) {
								e.printStackTrace();
							}
							try {
								c.startPreview();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
				});
			}
			
			
			if (!mSurfaceEmbedded) {
				
				FrameLayout fl = (FrameLayout)findViewById(R.id.preview_holder);
				fl.addView(mCameraSurface);
				
				mSurfaceEmbedded = true;
			}
			
			
			
		} else {
			
			if (mCameraSurface == null)
				return;
			
			if (mSurfaceEmbedded) {
				FrameLayout fl = (FrameLayout)findViewById(R.id.preview_holder);
				fl.removeAllViews();
				
				mSurfaceEmbedded = false;
			}
			
		}
	}
	
	public void clickNewCamera(boolean enabled, boolean doSetupScroller) {
		if ( enabled && Build.VERSION.SDK_INT < 23 ) {
			Toast.makeText(this, "Update to Android 6.0 for this", Toast.LENGTH_SHORT).show();
			enabled = false;
		}
		torchOff();
		stopFlashing();
    	Editor e = mPrefs.edit();
    	e.putBoolean(P_NEW_CAMERA, enabled);
    	apply(e);
    	setupCameraStuff();
        applyPreview();
    	if (doSetupScroller) {
			setupScroller(true);
		}
	}
	
	public void clickOpenHack(boolean enabled) {
    	Editor e = mPrefs.edit();
    	e.putBoolean(P_OPEN_HACK, enabled);
    	if ( ! enabled ) {
    		e.putBoolean(SettingsFragment.P_PREVIEW_HACK, false);
    	}
    	apply(e);
    	setupCameraStuff();
	}
	
	
	@SuppressLint("NewApi")
	private void setupNewCamera() {
		if (mService != null && mService.isFlashReady() ) {
			return;
		}
		if ( Build.VERSION.SDK_INT < 21 ) {
	    	Editor e = mPrefs.edit();
	    	e.putBoolean(P_NEW_CAMERA, false);
	    	apply(e);
	    	setupCameraStuff();
		}
		if ( mAwaitingNewCamera ) return;
		mAwaitingNewCamera = true;
		tearDownCamera();

		final MainActivity activity = this;
		final StrobeService service = mService;

		CameraManager camMan = (CameraManager)getSystemService(CAMERA_SERVICE);

		// It needs to have a flash and preferably we want the back camera
		String bestCameraId = null;

		try {
			for(final String cameraId : camMan.getCameraIdList()){
				CameraCharacteristics characteristics = null;
				try {
					characteristics = camMan.getCameraCharacteristics(cameraId);
				} catch (Exception e) {
					// sometimes throws RuntimeException
					e.printStackTrace();
					continue;
				}
				boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
				if ( ! hasFlash ) continue;
				if ( bestCameraId == null ) {
					bestCameraId = cameraId;
				}
				int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
				if(cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
					bestCameraId = cameraId;
					break;
				}
			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}

		if ( bestCameraId == null ) {
			toastOnUiThread(MainActivity.this, "No flash on device?", Toast.LENGTH_SHORT);
			handleLedScreenPress(true, true);
			mAwaitingNewCamera = false;
			return;
		}

		if ( mService != null ) {
			mService.mWhichCamera = bestCameraId;
		}

//		if ( ! mPrefs.getBoolean(P_OPEN_HACK, false) ) {
		weJustGotCamera();
		mAwaitingNewCamera = false;
		return;
//		}

//		CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
//
//			@Override
//			public void onOpened(CameraDevice camera) {
//				mService.mCamera2 = camera;
//				mAwaitingNewCamera = false;
//				weJustGotCamera();
//			}
//			@Override
//			public void onDisconnected(CameraDevice camera) {
//				mAwaitingNewCamera = false;
//				boolean needsClick = false;
//				if ( service != null ) {
//					try {
//						if ( mService.mCamera2 == null ) {
//							toastOnUiThread(MainActivity.this, "Camera error", Toast.LENGTH_SHORT);
//							needsClick = true;
//						}
//						mService.mCamera2 = null;
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//				if (needsClick) {
//					clickOpenHack(false);
//				}
//			}
//			@Override
//			public void onError(CameraDevice camera, int error) {
//				mAwaitingNewCamera = false;
//				boolean needsClick = false;
//				if ( service != null ) {
//					try {
//						if ( mService.mCamera2 == null ) {
//
//							toastOnUiThread(MainActivity.this, "Camera error (" + Integer.toString(error) + ")",
//									Toast.LENGTH_SHORT);
//							needsClick = true;
//
//						}
//						mService.mCamera2 = null;
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//				if (needsClick) {
//					clickOpenHack(false);
//				}
//			}
//		};




//		try {
//			camMan.openCamera(bestCameraId, callback, null);
//		} catch (CameraAccessException e) {
//			toastOnUiThread(MainActivity.this, "Error requesting camera", Toast.LENGTH_SHORT);
//			e.printStackTrace();
//
//			handleLedScreenPress(true, true);
//			mAwaitingNewCamera = false;
//			return;
//		}

	}

	private void weJustGotCamera() {
		if ( mService != null ) {
			if (mService.mStroberThread == null) {
				mService.mFlashing = false;
				mService.mTorch = false;
				mService.mMusicing = false;
				mService.startTheStrober();

				if (mPrefs.getBoolean(P_TORCH_ON, false)) {
						torchOn();
				}
			}
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				applyPreview();
				doWidget();
			}
		});



	}
	
	
	@SuppressLint("NewApi")
	private void tearDownCamera() {
		if ( mService != null ) {
			Camera c = null;
//			CameraDevice c2 = null;
			c = mService.mCamera;
//			c2 = mService.mCamera2;
			mService.mCamera = null;
//			mService.mCamera2 = null;
			mService.mWhichCamera = null;
			mService.stopTheStrober(false);
			
			if (c != null) {
				try {
					c.stopPreview();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				c.release();
			}			
//			if (c2 != null && Build.VERSION.SDK_INT >= 23) {
//				c2.close();
//			}
		}
		MainActivity.this.runOnUiThread( new Runnable() {
			@Override
			public void run() {
				if (mCameraSurface != null) {
					FrameLayout fl = (FrameLayout)findViewById(R.id.preview_holder);
					fl.removeAllViews();

					mSurfaceEmbedded = false;
					mCameraSurface = null;
				}
				mSurfaceEmbedded = false;
			}
		});
	}
	
	private class CameraException extends Exception {
		
	}
	
	public void setupCameraStuff() {
		
		if (mSettingUpCameraStuff) {
			return;
		}
		
		if ( ! selfPermissionGranted( Manifest.permission.CAMERA ) ) {

			if ( ! mRequestedCamera ) {
				mRequestedCamera = true;
				ActivityCompat.requestPermissions(MainActivity.this, 
						new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
			} else {
				Toast.makeText(this, "Camera permission denied! (Fix in System Settings)", Toast.LENGTH_SHORT ).show();
				handleLedScreenPress(true, true);
			}
			
			return;
		}
		
		if ( mPrefs.getBoolean(P_NEW_CAMERA, false) ) {
			setupNewCamera();
			return;
		}

		
		Thread th = new Thread() {
			@Override
			public void run() {
				if (mSettingUpCameraStuff) {
					return;
				}
				if (mService != null && mService.isFlashReady() ) {
					return;
				}

				mSettingUpCameraStuff = true;
				tearDownCamera();
				CameraException ex = new CameraException();
				
				Camera camera = null;
				
				try {

					try {
						camera = Camera.open();
					} catch (Exception e) {
						e.printStackTrace();
						toastOnUiThread(MainActivity.this, "Could not connect to camera", Toast.LENGTH_SHORT);
						throw ex;
					}
					
					if (camera == null) {
						toastOnUiThread(MainActivity.this, "Camera not detected", Toast.LENGTH_SHORT);
						throw ex;
					}
					
					try {
						camera.startPreview();
					} catch (Exception e) {
						toastOnUiThread(MainActivity.this, "Failed to start preview (camera glitch, duration)", Toast.LENGTH_SHORT);
						e.printStackTrace();
						throw ex;
					}
				
					Camera.Parameters paramOn = camera.getParameters();
					Camera.Parameters paramOff = camera.getParameters();
					Camera.Parameters paramOff2 = camera.getParameters();
					Camera.Parameters testParam = camera.getParameters();
					
					
					if (paramOn == null) {
						toastOnUiThread(MainActivity.this, "No parameters (camera error)", Toast.LENGTH_SHORT);
						camera.release();
						throw ex;
					}
					

					boolean flashOn = paramOn.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH;
					boolean actual = mPrefs.getBoolean(P_TORCH_ON, false);
					if (!(flashOn ^ actual) && useLed()) {
						setTorchOn(flashOn, MainActivity.this);
						setTorchCheck();
					}
					
					if (mPrefs.getBoolean(AdvancedFragment.P_PERFORM_A, false)) {
						testParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
						boolean failed = false;
						try {
							Field mMapField = null;
							mMapField = Camera.Parameters.class.getDeclaredField("mMap");
							mMapField.setAccessible(true);
							((HashMap)mMapField.get(testParam)).clear();
							testParam.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
							camera.setParameters(testParam);
							
							testParam = camera.getParameters();
							if (testParam.getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)) {
								((HashMap)mMapField.get(paramOn)).clear();
								((HashMap)mMapField.get(paramOff)).clear();
								((HashMap)mMapField.get(paramOff2)).clear();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					
					paramOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
					paramOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					paramOff2.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
						
					try {
						camera.setParameters(paramOff);
					} catch (Exception e) {
						camera.release();
						camera = Camera.open();
						try {
							camera.setParameters(paramOff2);
						} catch (Exception e1) {
							e1.printStackTrace();
							toastOnUiThread(MainActivity.this, "Fatal camera error.\nReboot may be necessary", Toast.LENGTH_LONG);
							try {
								camera.stopPreview();
							} catch (Exception e2) {
								e2.printStackTrace();
							}
							camera.release();
							throw ex;
						}
					}
						
						
					if (mService != null) {
						mService.mParamOn = paramOn;
						mService.mParamOff = paramOff;
						mService.mParamOff2 = paramOff2;
						mService.mCamera = camera;
//						toastOnUiThread(MainActivity.this, "Camera good", Toast.LENGTH_SHORT);
					}

					weJustGotCamera();

					if (mPaused) {
						if (camera != null) {
							try {
								camera.stopPreview();
							} catch (Exception e1) {
								e1.printStackTrace();
							}
							camera.release();
							
						}
						tearDownCamera();
					}
					
					
				} catch (Exception e) {
					e.printStackTrace();
					if (!(e instanceof CameraException))
						toastOnUiThread(MainActivity.this, "Camera error", Toast.LENGTH_SHORT);

					handleLedScreenPress(true, true);
					
					if (camera != null) {
						try {
							camera.stopPreview();
						} catch (Exception e1) {
							e.printStackTrace();
						}
						camera.release();
						
					}
					
					tearDownCamera();
					

				}
				mSettingUpCameraStuff = false;
				

				
				
			}
		};
		
		th.setPriority(Thread.MAX_PRIORITY);
		th.start();
		
	}
	

	private void doWidget() {
		if (!mWidgetAppeared)
			return;

		PatternStorage ps = null;
		try {
			ps = PatternStorage.getInstance(this);
		}  catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
			return;
		}
		PatPart[] pattern = ps.getPattern(mWidgetPattern);
		
		if (pattern == null) {
			toastOnUiThread(this, "Pattern not found", Toast.LENGTH_LONG);	
			return;
		}
		
		if (mService.mCamera != null) {
			setUseLed(mWidgetLed);
		}
		setUseScreen(mWidgetScreen);
		if ( mService != null ) {
			mService.settingsUpdate();
		}

		int commands = PatPart.getCommandSpots(pattern, this, 0);
		
		int[] flashes = new int[commands];
		
		PatPart.fillInCommands(0, flashes, pattern, this, 0);
		
		startFlashing(flashes, mWidgetLoop, S_WIDGET);
		
		
		
	}
	
	
	static public void toastOnUiThread(final Activity context, final String str, final int length) {
		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, str, length).show();
			}
		});
	}
	
	
	public boolean serviceReady(boolean showError) {
		if (mService != null) {
			if (!useLed())
				return true;
			if (mService.isFlashReady())
				return true;
		}
		if (showError)
			toastOnUiThread(this, "Not ready yet!", Toast.LENGTH_SHORT);
		return false;
	}
	
	
	public void torchOn() {
		updateScreenColor();
		setBackground(null);
		if (!serviceReady(false))
			return;
		if (mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
		mService.controlUpdate(null, false, true, false, S_OTHER);
		mService.mMusicing = false;
		setTorchCheck();
		setProperBrightness();
		setWakeLock(true);
	}
	
	public void torchOff() {
		setBackground(null);
		setWakeLock(false);
		if (!serviceReady(false))
			return;
		if (mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
		mService.controlUpdate(null, false, false, false, S_OTHER);
		setProperBrightness();
		setTorchCheck();
		mService.mMusicing = false;
	}
	
	public void startFlashing(int[] flashes, boolean loop, int who) {
		updateScreenColor();
		if (!serviceReady(true))
			return;
		if (mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
		mService.mMusicing = false;
//		String to_toast = "";
//		for ( int flash : flashes ) {
//			to_toast += " " + Integer.toString(flash);
//		}
//		toastOnUiThread(this, to_toast, Toast.LENGTH_LONG);
		if ( flashes.length > 10000 ) {
			int[] temp_flashes = new int[10000];
			for ( int i = 0; i < temp_flashes.length; i++ ) {
				temp_flashes[i] = flashes[i];
			}
			flashes = temp_flashes;
			toastOnUiThread(this, "Limiting to 5,000 flashes", Toast.LENGTH_SHORT);
		}
		mService.controlUpdate(flashes, true, false, loop, who);
		setProperBrightness();
		setWakeLock(true);
	}
	
	public void updateIfRunning(int[] flashes, boolean loop) {
		if (!serviceReady(false))
			return;
		if (mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
		if (mService.mFlashing)
			mService.controlUpdate(flashes, true, false, loop, mService.mWhoStartedIt);
		mService.mMusicing = false;
	}
	
	public void stopFlashing() {
		setWakeLock(false);
		if (!serviceReady(false))
			return;
		if (mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
		mService.mMusicing = false;
		mService.controlUpdate(null, false, false, false, mService.mWhoStartedIt);
		MainActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setBackground(null);
				setProperBrightness();
				setTorchCheck();
			}
		});
	}
	
	public void startMusicing() {
		updateScreenColor();
		if (!serviceReady(true))
			return;
		stopFlashing();
		mService.startMusicing(false);
		setWakeLock(true);
		setProperBrightness();
	}
	
	public void stopMusicing() {
		if (!serviceReady(false)) 
			return;
		if (mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
		mService.mMusicing = false;
		stopFlashing();
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

    public void updateScreenColor() {
    	mScreenColor = SettingsFragment.getColor(SettingsFragment.getFlashColor(this));
    }
    
    public boolean useLed() {
    	return mPrefs.getBoolean(P_USE_LED, true);
    }
    
    public boolean useScreen() {
    	return mPrefs.getBoolean(P_USE_SCREEN, false);
    }
    
    
    public void setUseScreen(boolean useIt) {
    	Editor e = mPrefs.edit();
    	e.putBoolean(P_USE_SCREEN, useIt);
    	apply(e);
		setProperBrightness();
    }
	
    public void setUseLed(boolean useIt) {
    	Editor e = mPrefs.edit();
    	e.putBoolean(P_USE_LED, useIt);
    	apply(e);
		setProperBrightness();
    }
	
    public static void setTorchOn(boolean on, Context context) {
    	Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	e.putBoolean(P_TORCH_ON, on);
    	apply(e);
    }
    
    private void setTorchCheck() {
		((CheckBox)findViewById(R.id.torch_check)).setChecked(mPrefs.getBoolean(P_TORCH_ON, false));
    	
    }
    
    public void setLedScreenCheck() {
    	runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    	    	((CheckBox)findViewById(R.id.led_check)).setChecked(useLed());
    	    	((CheckBox)findViewById(R.id.screen_check)).setChecked(useScreen());
    		}
    	});
    }

	public void handleLedScreenPress(boolean ledPressed, boolean forced) {
		boolean useLed = useLed();
		boolean useScreen = useScreen();
		
		if (ledPressed) {
			setUseScreen(true);
			setUseLed(!useLed && ! forced);
		} else {
			setUseScreen(!useScreen && ! forced);
			setUseLed(true);
		}
		
		if (useLed() && mService != null) {
			if ( ! mService.isFlashReady()) {
				Toast.makeText(this, "Camera issues.\nTry restarting app", Toast.LENGTH_SHORT).show();
				setUseLed(false);
				setUseScreen(true);
			}
		}
		
		setLedScreenCheck();
		if (mService != null)
			mService.settingsUpdate();
	}
	
	private void handleHide() {
		((CheckBox)findViewById(R.id.hide_check)).setChecked(mHidden);
		hideV(R.id.view_pager, false);
		hideV(R.id.lower_divider, false);
		hideV(R.id.torch_check, true);
		hideV(R.id.led_check, true);
		hideV(R.id.screen_check, true);
		hideV(R.id.top_divider, false);
		hideV(R.id.upper_divider, false);
		hideV(R.id.title_scroller, false);
		
		if (mHidden) {
    		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
    				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
    		getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
	}
    
	private void hideV(int id, boolean gone) {
		findViewById(id).setVisibility(mHidden ? (gone ? View.GONE : View.INVISIBLE) : View.VISIBLE);
	}
	
	public void override(boolean override) {
		mOverride = override;
		findViewById(R.id.override).setVisibility(mOverride ? View.VISIBLE : View.GONE);
	}
    
	private ServiceConnection mTheServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			mService = ((LocalBinder)iBinder).getService();
			
			mService.endForeground();

			mService.mServiceException = new Handler() {
				@Override
				public void handleMessage(Message msg) {

					String message = msg.getData().getString("Toast");
					toastOnUiThread(MainActivity.this, message, Toast.LENGTH_LONG);
				}
			};
			
			mService.mBurstDoner = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					stopFlashing();
					if (mWidgetPattern != null)
						finish();
				}
			};
			
			mService.mScreenPost = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					Boolean on = msg.what == StrobeLibActivity.SCREEN_ON;
					setBackground(on);
				}
			};
			
			mService.mMusicDoneHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					stopMusicing();
				}
			};
			
			musicHandlerUpdate();
			
			boolean weDoWidget = false;
			
			if (!mService.isFlashReady()) {
				Log.d("MainActivity", "Flash was not ready");
				setupCameraStuff();		// this is asynchronous
			} else {
				Log.d("MainActivity", "Flash was ready");
				weDoWidget = true;
			}
			
			setWakeLock(mService.mFlashing || mService.mTorch || mService.mMusicing);
			setProperBrightness();
			setTorchCheck();
			
			final TextView tv = (TextView)findViewById(R.id.lag);

	    	(new Handler() {
	    		@Override
	    		public void handleMessage(Message msg) {
	    			
	    			if (mService != null) {
		    			this.sendEmptyMessageDelayed(0, 100);
		    			int checkLag = mService.doCheckLag();
		    			boolean visible = tv.getVisibility() == View.VISIBLE;
		    			if (!visible && checkLag <= 0)
		    				return;
	    				tv.setVisibility(checkLag > 0 ? View.VISIBLE : View.GONE);
	    				if (checkLag == 696969)
	    					tv.setText("-- Hz");
	    				else
	    					tv.setText(Integer.toString(checkLag) + " Hz");

//	    				((CheckBox)findViewById(R.id.torch_check)).setText(Integer.toString(mService.getDebugNumber()));
	    			}
	    			
	    			
	    			
	    		}
	    	}).sendEmptyMessage(0);
	    	
	    	if (weDoWidget)
	    		doWidget();
			
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};
	
	public class MyAdapter extends FragmentPagerAdapter {
		public MyAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int arg0) {
			int origArg0 = arg0;
			if (mPrefs.getBoolean(AdvancedFragment.P_DIAGNOSTIC, false)) {
				if (arg0 == F_STROBE)
					arg0 = F_DIAGNOSTIC;
				if (arg0 > F_STROBE)
					arg0--;
			}
			
			MyFragment frag = null;
			switch (arg0) {
			case F_DIAGNOSTIC:
				frag = new DiagnosticFragment();
				break;
			case F_ADVANCED:
				frag = new AdvancedFragment();
				break;
			case F_SETTINGS:
				frag = new SettingsFragment();
				break;
			case F_STROBE:
				frag = new StrobeFragment();
				break;
			case F_MUSIC:
				frag = new MusicFragment();
				break;
			case F_TACHO:
				frag = new TachoFragment();
				break;
			case F_PATTERN:
				frag = new PatternFragment();
				break;
			case F_WIDGET:
				frag = new WidgetFragment();
				break;
			case F_TURNTABLE:
				frag = new TurntableFragment();
				break;
			case F_ABOUT:
				frag = new AboutFragment();
				break;
			case F_HELP:
				frag = new HelpFragment();
				break;
			}
			if (mStartThisFragsHandlers == origArg0) {
				mStartThisFragsHandlers = -1;
				frag.requestStartHandlers();
			}
			
			mFragments[origArg0] = frag;
			
			return frag;
		}

		@Override
		public int getCount() {
			return getTitles().length;
		}
		
		@Override
		public int getItemPosition(Object obj) {
			return PagerAdapter.POSITION_NONE;
		}
		
		
	}
	
	
	@SuppressLint("NewApi")
	public static void apply(Editor e) {
		if (Build.VERSION.SDK_INT >= 9)
			e.apply();
		else
			e.commit();
	}
	
	
	private void setBackground(Boolean on) {
		if (on == null || mService == null || !(mService.mFlashing || mService.mTorch || mService.mMusicing)) {
			mRoot.setBackgroundColor(Color.TRANSPARENT);
		} else {
			if (on) {
				mRoot.setBackgroundColor(mScreenColor);
			} else {
				mRoot.setBackgroundColor(Color.BLACK);
			}
		}
	}
	
	public void musicHandlerUpdate() {
		if (mService == null)
			return;
		mService.mBeater = mBeater;
		mService.mBpmUpdater = mBpmUpdater;
		if (mService.mLastFreq != 0 && mBpmUpdater != null && mService.mMusicing == true)
			mBpmUpdater.sendEmptyMessage((int) (mService.mLastFreq*60));
		if (mService.mMusicing == false && mBpmUpdater != null)
			mBpmUpdater.sendEmptyMessage(-1);
	}
	
	public void refreshFragments() {
		mAdapter.notifyDataSetChanged();
		
	}
	
	
	public void setProperBrightness() {
		if (mService == null) {
			setScreenBrightness(-1);
			return;
		}
		
		boolean screen = useScreen();
		
		if (mService.mFlashing || mService.mMusicing){
			boolean led = useLed();
			
			if (screen) {
				setScreenBrightness(1);
			} else if (led && mPrefs.getBoolean(SettingsFragment.P_DIM, false)) {
				setScreenBrightness(0.005f);
			} else {
				setScreenBrightness(-1);
			}
		} else if (mService.mTorch && screen) {
			setScreenBrightness(1);
		} else {
			setScreenBrightness(-1);
		}
	}
	

    private void setScreenBrightness(final float bright) {
    	runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    	    	WindowManager.LayoutParams lp = getWindow().getAttributes();
    			lp.screenBrightness = bright;
    			getWindow().setAttributes(lp);
    		}
    	});
    }
    
    public void volumeUp() {
    	if (mService.mWhoStartedIt == S_OTHER)
    		return;
    	float freq = mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
    	freq += 0.05f;
    	
    	volumeUpdate(freq);
    	
    }
	public void volumeDown() {
    	if (mService.mWhoStartedIt == S_OTHER)
    		return;
    	
    	float freq = mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
    	

		freq -= 0.05f;
		if (freq <= 0)
			freq = 0;
		
		volumeUpdate(freq);
		
	}
	
	public void volumeUpdate(float freq) {

		Editor e = mPrefs.edit();
		e.putFloat(StrobeFragment.P_FREQUENCY, freq);
		MainActivity.apply(e);

		int[] flashes = null;
		if (mService.mWhoStartedIt == S_STROBE)
			flashes = new int[] {calculateTime(true), calculateTime(false)};
		if (mService.mWhoStartedIt == S_TACHO)
			flashes = new int[] {0, (int) (1000000/freq)};
		
		if (flashes != null)
			updateIfRunning(flashes, true);
	}
	

	private int calculateTime(boolean on) {
		float freq = mPrefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
		boolean usedSaved = mPrefs.getBoolean(StrobeFragment.P_USE_SAVED, false);
		int savedTime = mPrefs.getInt(StrobeFragment.P_ON_LENGTH, 50000);

		int mDuty = mPrefs.getInt(StrobeFragment.P_DUTY, 50);

		double frameWidth = 1000000.0d/freq;

		if (usedSaved) {
			int onTime = savedTime;
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
	
	public MyFragment getCurrentFragment() {
		return mFragments[mPager.getCurrentItem()];
	}
	
	public void updateSliders() {
		MyFragment frag = getCurrentFragment();
		if (frag != null)
			frag.updateSliders(false);
	}

	
	@SuppressLint("NewApi")
	public Boolean selfPermissionGranted(String permission) {
        // For Android < Android M, self permissions are always granted.
        Boolean result = true;

  
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            result = MainActivity.this.checkSelfPermission(permission)
                    == PackageManager.PERMISSION_GRANTED;
        }

        return result;
    }
	
	@Override
	public void onRequestPermissionsResult(
	        int requestCode,
	        String permissions[],
	        int[] grantResults) {
		if ( requestCode == REQUEST_CAMERA_PERMISSION ) {
			setupCameraStuff();
		}
		if ( requestCode == REQUEST_MICROPHONE_PERMISSION ) {
			if (! selfPermissionGranted(Manifest.permission.RECORD_AUDIO) ) {
				Toast.makeText(this, "Mircophone permission denied! (Fix in System Settings)", Toast.LENGTH_SHORT ).show();
			}
		}
	}
	
	public boolean haveMicrophone() {
		if ( ! selfPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
				ActivityCompat.requestPermissions(MainActivity.this, 
						new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE_PERMISSION);
			return false;
		}
		return true;
	}
	/*
	
	advanced
	 use rpm
	 show x2 and x1/2
	 Use multi commands
	 performance hack
	 late flash rejection threshold
	 flash preempt
	
	settings
	 preview hack
	 max frequency
	 persist on app close
	 dim screen
	 show burst
	 screen flash color
	 
	
	strobe
	 base frequency
	 fine tuning
	 duty cycle
	 steady on
	 save on
	 x1/2 x2
	 running
	 burst
	 
	music
	
	tachometer
	
	patterns
	
	turn table
	
	
	
	
	torch page
	indicator for volume buttons
		torch based on state
	manual
		diagnostic update
		diagnostic legend
		diagnostic option
		realtime reporting of frequency and duty
	accuracy vs party check
	presets in opening dialog
		widget
	
	
	
	

	
	
	*/
	
}
