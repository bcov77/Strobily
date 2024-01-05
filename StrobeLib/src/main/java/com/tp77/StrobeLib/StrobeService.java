package com.tp77.StrobeLib;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class StrobeService extends Service {

	public static final int READY_NOT = 0;
	public static final int READY_OLD = 1;
	public static final int READY_NEW = 2;
	public static final int READY_NEW_W_CAMERA = 3;
	
	private LocalBinder mTheIBinder;
	
	private PowerManager mPowerManager;
	private WakeLock mWakeLock;
	private SharedPreferences mPrefs;
	
	public boolean mFlashing;
	public boolean mTorch;
	public int[] mFlashes;
	public boolean mLoop;
	
	public boolean mMusicing;
	private long mRunningNumber = System.currentTimeMillis();
	
	public Thread mStroberThread;
	
	public boolean mBound = false;
	
	public Camera mCamera;
	public Camera.Parameters mParamOn;
	public Camera.Parameters mParamOff;
	public Camera.Parameters mParamOff2;
	
//	public CameraDevice mCamera2 = null;
	public String mWhichCamera = null;
	
	public Handler mScreenPost;
	public boolean mForeground;
	public Handler mMusicDoneHandler;

	public Handler mBurstDoner;
	public Handler mServiceException;
	
	public int mWhoStartedIt = MainActivity.S_STROBE;
	
	private static final int N_FOREGROUND = 3;
	
	private BurnerThread[] mThreads = new BurnerThread[10];
	
	private boolean mAds = true;
	
	private Handler mToaster = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Toast.makeText(StrobeService.this, "Upgrade to full version to use continuously", Toast.LENGTH_LONG).show();
		}
	};
	
	
	static {
		System.loadLibrary("NDK3");
	}
	
	private native void doStrobe(Camera camera, Camera.Parameters onParams, Camera.Parameters offParams, Camera.Parameters offParams2, 
			String onFlatten, String offFlatten, String offFlatten2, boolean useFlatten, CameraManager camMan, String whichCamera );
	private native void update(int[] flashes, boolean loop, boolean flashing, boolean torch);
	private native void setSettings(boolean useLed, boolean useScreen, int lateThreshold,
			int flashPreempt, boolean gentle);
	private native void kill();
	private native int debugNumber();
	private native int checkLag2();
	private native int checkLag();
	private native void timeSync(long time);
	private native long getTime();
	private native long[] getDiagnostics();
	private native void otherThread(Camera camera, String offF);
	private native int[] getDutyData();
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		mTheIBinder = new LocalBinder();
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Background flasher");
	}
	
	
	@Override
	public void onDestroy() {
		mMusicing = false;
		stopTheStrober(false);
		endForeground();
		
		if (mCamera != null) {
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mCamera.release();
		}
		
		MainActivity.setTorchOn(false, this);
		
		setWakeLock(false);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	private static final String mNotifChannelId = "Strobily_channel";
	
	public void goForeground() {
		if ( ! mPrefs.getBoolean(MainActivity.P_NEEDS_NOTIF, false ) ) {
			Editor e = mPrefs.edit();
			e.putBoolean(MainActivity.P_NEEDS_NOTIF, true);
			e.commit();
		}
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		String str = "Currently flashing";
		if (mTorch) {
			str = "Torch enabled";
		}
		if (mMusicing) {
			str = "Currently flashing to the music";
		}
		
	    NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
	   
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	        int importance = NotificationManager.IMPORTANCE_HIGH;
	        NotificationChannel channel = notificationManager.getNotificationChannel(mNotifChannelId);
	        if (channel == null) {
	            channel = new NotificationChannel(mNotifChannelId, "Strobily", NotificationManager.IMPORTANCE_MAX);
	            notificationManager.createNotificationChannel(channel);
	        }
	    }
	    
	    
	    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, mNotifChannelId);

		
	    Notification notification  = builder
	            .setContentIntent(PendingIntent.getActivity(
	    				this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
	            .setSmallIcon(R.drawable.ic_notif)
	            .setWhen( System.currentTimeMillis())
	            .setContentTitle("Strobily")
	            .setPriority(Notification.PRIORITY_MAX)
	            .setContentText(str).build();
//	    notificationManager.notify(N_FOREGROUND, notification);
		
	    startForeground(N_FOREGROUND, notification);
		
		mForeground = true;
	}
	
	public void endForeground() {
		if (!mForeground)
			return;
		stopForeground(true);
	}
	
	public boolean isFlashReady() {
		boolean new_camera = mPrefs.getBoolean(MainActivity.P_NEW_CAMERA, false);
		if ( ! new_camera ) {
			return mCamera != null;
		}
		if ( mWhichCamera == null ) {
			return false;
		}
//		if ( ! mPrefs.getBoolean(MainActivity.P_OPEN_HACK, false) ) {
			return true;
//		}
//		return mCamera2 != null;
	}
	
	public void stopTheStrober(boolean i_am_the_strober) {

		Log.d("StobeService", "Stopping strober");
		mFlashing = false;
		mTorch = false;
		stopThreads();
		kill();
		while (!i_am_the_strober && mStroberThread != null && mStroberThread.isAlive())
			kill();

		Editor e = mPrefs.edit();
		e.putBoolean(MainActivity.P_WIDGET_RUNNING, false);
		MainActivity.apply(e);
		mStroberThread = null;
		Log.d("StrobeService", "Strober stopped");
	}

	public void startTheStrober() {
		boolean doFlatten = false;
    	try {
    		Method theNative = Camera.class.getDeclaredMethod("native_setParameters", String.class);
    		if (theNative != null)
    			doFlatten = true;
    	} catch (Exception e) {
    	}
    	final boolean finalDoFlatten = doFlatten;
    	
    	Log.d("StobeService", doFlatten ? "Flattening" : "Not Flattening");
    	
    	CameraManager camMan = null;
    	if ( mWhichCamera != null ) {
    		camMan = (CameraManager)getSystemService(CAMERA_SERVICE);
    	}
    	
    	final CameraManager useCamMan = camMan;
    	
		mStroberThread = (new Thread() {
    		@Override
    		public void run() {
    			setPriority(MAX_PRIORITY);
    			try {
    				doStrobe(mCamera, mParamOn, mParamOff, mParamOff2,
							mParamOn != null ? mParamOn.flatten() : null,
							mParamOff != null ? mParamOff.flatten() : null,
							mParamOff2 != null ? mParamOff2.flatten(): null, finalDoFlatten,
							useCamMan, mWhichCamera);
				} catch ( Exception e ) {
    				e.printStackTrace();
    				if ( mServiceException != null ) {
    					Message msg = new Message();
    					Bundle bundle = new Bundle();
    					bundle.putString("Toast", "Error: " + e.getMessage());
    					msg.setData(bundle);
    					mServiceException.sendMessage(msg);
					}
					if ( StrobeService.this.mForeground ) {
						endForeground();
					}
                    stopTheStrober(true);

					if (mCamera != null) {
						try {
							mCamera.stopPreview();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						try {
							mCamera.release();
						} catch ( Exception e1 ) {
							e1.printStackTrace();
						}
						mCamera = null;
					}
//					if (mCamera2 != null) {
//						try {
//							mCamera2.close();
//						} catch ( Exception ed ) {
//							ed.printStackTrace();
//						}
//						Log.d("StrobeService", "Setting null");
//						StrobeService.this.mCamera2 = null;
//					}
					mWhichCamera = null;
				}
    			Log.d("StrobeService", "Strobe complete");
    		}
    	});
    	mStroberThread.start();
    	
//    	Thread th = new Thread() {
//    		@Override
//    		public void run() {
//    			setPriority(MAX_PRIORITY);
//    			otherThread(mCamera, mParamOff.flatten());
//    		}
//    	};
//    	th.start();
    	
    	settingsUpdate();
    	
    	Log.d("StrobeService", Long.toString(System.currentTimeMillis()*1000) + " " + Long.toString(getTime()));
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
	
	public void settingsUpdate() {
		boolean useLed = mPrefs.getBoolean(MainActivity.P_USE_LED, true);
		boolean useScreen = mPrefs.getBoolean(MainActivity.P_USE_SCREEN, false);
		int lateThreshold = AdvancedFragment.lateThreshold(this);
		int flashPreempt = AdvancedFragment.flashPreempt(this);
		boolean gentle = AdvancedFragment.useGentle(this);
		
		this.setSettings(useLed, useScreen, lateThreshold, flashPreempt, gentle);
		
		if (mFlashing && mPrefs.getBoolean(AdvancedFragment.P_FULL_CPU, false)) {
			startThreads();
		} else {
			stopThreads();
		}
	}
	
	public void startThreads() {
		synchronized (mThreads) {
			if (mThreads[0] == null) {
				for (int iii = 0; iii < 10; iii++) {
					mThreads[iii] = new BurnerThread();
				}
			}
		}
	}
	

	public void stopThreads() {
		synchronized (mThreads) {
			if (mThreads[0] != null) {
				for (int iii = 0; iii < 10; iii++) {
					mThreads[iii].stopIt();
					mThreads[iii] = null;
				}
			}
		}
	}
	
	
	private class BurnerThread extends Thread {
		private boolean mStopped = false;
		
		public int mResult = 0;
		public long mLastSleep = 0;
		
		@Override
		public void run() {
			int increaser = 5;
			while (!mStopped) {
				increaser++;
				increaser*=8;
				increaser^=55;
				mResult = increaser;
				if (mLastSleep + 1000 < System.currentTimeMillis()) {
					try {
						sleep(1);
					} catch (Exception e) {
						e.printStackTrace();
					}
					mLastSleep = System.currentTimeMillis();
				}
			}
		}
		
		public BurnerThread() {
			setPriority(Thread.MIN_PRIORITY);
			start();
		}
		
		public void stopIt() {
			mStopped = true;
		}
	}
	
	
	public void controlUpdate(int[] flashes, boolean flashing, boolean torch, boolean loop, int whoStartedIt) {
		mFlashing = flashing;
		mTorch = torch;
		mLoop = loop;
		boolean widgetRunning = false;
		if (flashing) {
			mWhoStartedIt = whoStartedIt;
			if (whoStartedIt == MainActivity.S_WIDGET) {
				widgetRunning = true;
			}
		} else {
			mWhoStartedIt = MainActivity.S_STROBE;
		}
		Editor e = mPrefs.edit();
		e.putBoolean(MainActivity.P_WIDGET_RUNNING, widgetRunning);
		MainActivity.apply(e);
		
		MainActivity.setTorchOn(mTorch, this);
		if (mFlashing || mTorch)
			setWakeLock(true);
		else
			setWakeLock(false);
		
		if (flashes == null)
			flashes = new int[] {50000, 50000};
		
		for (int iii = 0; iii < flashes.length; iii++) {
			if (flashes[iii] > 1000000*100)
				flashes[iii] = 1000000*100;
			if (iii % 2 == 1) {
				if (flashes[iii] < 1000)
					flashes[iii] = 1000;
			}
		}

		mFlashes = flashes;
		update(mFlashes, mLoop, mFlashing, mTorch);
		
		if (mFlashing && mPrefs.getBoolean(AdvancedFragment.P_FULL_CPU, false))
			startThreads();
		else
			stopThreads();
		
	}
	
	public void syncTime(long time) {
		long elapsed = (time - System.currentTimeMillis())*1000;
		
		timeSync(elapsed + getTime());
	}
	
	public long timeGet() {
		return getTime()/1000;
	}
	
	public int getDebugNumber() {
		return debugNumber();
	}
	
	public int doCheckLag() {
		return checkLag();
	}
	

	public int doCheckLag2() {
		return checkLag2();
	}
	
	
	public long[] doGetDiagnostics() {
		return getDiagnostics();
	}
	
	public int[] doGetDutyData() {
		return getDutyData();
	}

	public class LocalBinder extends Binder {
		StrobeService getService() {
			return StrobeService.this;
		}
	};
	
	private void burstDone(boolean fakeParameter) {
		Editor e = mPrefs.edit();
		e.putBoolean(MainActivity.P_WIDGET_RUNNING, false);
		MainActivity.apply(e);
		if (mBound && mBurstDoner != null) {
			mBurstDoner.sendEmptyMessage(0);
		}
	}
	
	private void sendScreenState(boolean screenOn) {
		if (mBound && mScreenPost != null) {
			mScreenPost.sendEmptyMessage(screenOn ? StrobeLibActivity.SCREEN_ON : StrobeLibActivity.SCREEN_OFF);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		mBound = true;
		return mTheIBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		super.onUnbind(intent);
		mBound = false;
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public Handler mBpmUpdater = null;
	public Handler mBeater = null;

	
	private static final int BITRATE = 44100;
	private static final int FRAME_WIDTH = 512;
	private static final int INTENSITY_MEMORY = BITRATE/(FRAME_WIDTH/2);	// this is also beat samples per second
	private static final int BEAT_MEMORY = 1024;	// currently this holds 6 seconds
	
	private short[] mBuf = new short[FRAME_WIDTH/2];
	private double[] a = new double[FRAME_WIDTH];
	private double[] b = new double[FRAME_WIDTH];
	
	private double[] c = new double[BEAT_MEMORY];
	private double[] d = new double[BEAT_MEMORY];
	
	private double[] e = new double[FRAME_WIDTH/2];
	
	private double[] g = new double[BEAT_MEMORY*2];
	private double[] h = new double[BEAT_MEMORY*2];
	private double[] i = new double[BEAT_MEMORY*2];
	private double[] j = new double[BEAT_MEMORY*2];
	
	private double[][] mLastSecond = new double[6][];
	private double[][] mLastSecondDeriv = new double[6][];
	
	private double[] mSumLastSecond = new double[BEAT_MEMORY];
	private long[] mLastSecTimes = new long[BEAT_MEMORY];
	

	private int mFramePos = 0;
	private int mBeatPos = 0;
	
	private static final int[] BINS = {
		200*FRAME_WIDTH/BITRATE,
		400*FRAME_WIDTH/BITRATE,
		800*FRAME_WIDTH/BITRATE,
		1600*FRAME_WIDTH/BITRATE,
		3200*FRAME_WIDTH/BITRATE,
		FRAME_WIDTH/2 + 10000
	};
	
	private static final int CACHE = 10;
	private int mFreqCachePos = 0;
	public double mLastFreq = 0;
	
	private double[] mFreqCache = new double[CACHE];
	
	private long mStart = 0;
	private int mSinceSync = 0;
	
	private long mTimeDif = 0;

	private static final float CONCURRENT = 10;
	private static final float SAME = 		5;
	private static final int KEEP = 		3;
	private static final int FINAL_KEEP = 		12;
	private static final int NEEDED = 8;
	private static final int PEAKS = 		18;
	private static final float STD = 		18f;
	
	private static final int TOPS = 7;
	private static final double TOP_GAP = 5;
	
	private static final int COMBS = 8;
	
	public void startMusicing(boolean ads) {
		ads = false;
		mAds = ads;
		
		mMusicing = true;
		
		final long runningNumber = ++mRunningNumber;
		
		for (int iii = 0; iii < 6; iii++) {
			mLastSecond[iii] = new double[INTENSITY_MEMORY];
			mLastSecondDeriv[iii] = new double[INTENSITY_MEMORY];
		}
		
		for (int iii = 0; iii < KEEP; iii++) {
			mTops[iii] = new double[TOPS];
		}
		
		
		mLastFreq = 0;

		Arrays.fill(mKeepers, 0);
		Arrays.fill(mPast, 0);
		Arrays.fill(mSumLastSecond, 0);
		Arrays.fill(mFreqCache, 0);
		Arrays.fill(mPastSuccess, 0);
		
		mStart = System.currentTimeMillis();
		
		(new Thread() {
			@Override
			public void run() {
				setPriority(Thread.MAX_PRIORITY);
				
				FFT fft = new FFT(FRAME_WIDTH);
				

				AudioRecord ar = new AudioRecord(AudioSource.MIC, BITRATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 32768);
				
				ar.startRecording();
				

				double[] window = new double[INTENSITY_MEMORY /10];
				
				for (int iii = 0; iii < window.length; iii++) {
					window[iii] = 0.5d * (1-Math.cos(2*Math.PI*(iii+window.length)/(window.length*2)));
				}
				
				int read = 0;
				int curSize = 0;
				double[] curPow = new double[6];
				int bin = 0;
				double averagePower = 0;
				
				long time = 0;
				long prevTime = 0;
				
				while (mMusicing && runningNumber == mRunningNumber) {
					read = ar.read(mBuf, 0, FRAME_WIDTH/2-curSize);
					
					
					// this applies a hanning window
					for (int iii = 0; iii < read; iii++) {
						a[iii + curSize + FRAME_WIDTH/2] = mBuf[iii] * (0.5d * (1-Math.cos(2*Math.PI*(iii+curSize)/FRAME_WIDTH)));
						e[iii + curSize] = mBuf[iii];
					}
					
					curSize += read;
					
					if (curSize < FRAME_WIDTH/2) {
						continue;
					}
					
					time = System.currentTimeMillis();
					
					curSize = 0;
					
					Arrays.fill(b, 0);
					
					fft.fft(a, b);
					
					for (int iii = 0; iii < 6; iii++)
						curPow[iii] = 0;
					
					
					bin = 0;
					
					for (int iii = 1; iii < FRAME_WIDTH / 2; iii++) {
						if (iii > BINS[bin])
							bin++;
						curPow[bin] += Math.sqrt(a[iii]*a[iii] + b[iii]*b[iii]);
//						curPow[bin] += Math.sqrt(a[FRAME_WIDTH - 1 - iii]*a[FRAME_WIDTH - 1 - iii] 
//								+ b[FRAME_WIDTH - 1 - iii]*b[FRAME_WIDTH - 1 - iii]);
					}
					
					
					boolean anyBeat = false;
					double std = 0;
					double temp = 0;
					
					double sumLastSecond = 0;
					
					for (int iii = 0; iii < 6; iii++) {
						mLastSecond[iii][mFramePos] = curPow[iii];
						
						double lastSum = 0;
						double thisSum = 0;
						
						int index;
						for (int jjj = 0; jjj < window.length; jjj++) {
							index = mFramePos - jjj;
							if (index < 0)
								index += mLastSecond[iii].length;
							thisSum += mLastSecond[iii][index] * window[jjj];
							
						}
						for (int jjj = 0; jjj < window.length; jjj++) {
							index = mFramePos - jjj - 1;
							if (index < 0)
								index += mLastSecond[iii].length;
							lastSum += mLastSecond[iii][index] * window[jjj];
							
						}
						
						double deriv = thisSum - lastSum;
//						if (deriv < 0)
//							deriv = 0;
						
						mLastSecondDeriv[iii][mFramePos] = deriv;
						
						averagePower = 0;
						for (int jjj = 0; jjj < INTENSITY_MEMORY; jjj++) {
							averagePower += mLastSecondDeriv[iii][jjj];
						}
						averagePower /= INTENSITY_MEMORY;
						
						std = 0;
						for (int jjj = 0; jjj < INTENSITY_MEMORY; jjj++) {
							temp = mLastSecondDeriv[iii][jjj] - averagePower;
							std += temp*temp;
						}
						std = Math.sqrt(std) / (INTENSITY_MEMORY-1);
						
						if (iii == 0)
							std /= 2;
						
						if (iii < 5) {
							sumLastSecond += (curPow[iii]-averagePower) / std;
						}
						
						
//						if (iii < 5 && (curPow[iii]-averagePower)/std > 12)
//							anyBeat = true;
						
					}
					
					
					double avg = 0;
					
					synchronized (mSumLastSecond) {
						mBeatPos++;
						if (mBeatPos >= BEAT_MEMORY) {
							mBeatPos = 0;
						}
						
						mLastSecTimes[mBeatPos] = prevTime;
						prevTime = time;
						
						mSumLastSecond[mBeatPos] = sumLastSecond;
						for (int iii = 0; iii < BEAT_MEMORY; iii++) {
							avg += mSumLastSecond[iii];
						}
						
						avg/= BEAT_MEMORY;
						
						std = 0;
						
						for (int iii = 0; iii < BEAT_MEMORY; iii++) {
							temp = mSumLastSecond[iii] - avg;
							std += temp*temp;
						}
						
					}
					std = Math.sqrt(std) / (BEAT_MEMORY-1);
					
					if ((sumLastSecond - avg) / std > 7)
						anyBeat = true;
					
					
					
					mFramePos++;
					if (mFramePos >= INTENSITY_MEMORY) {
						mFramePos = 0;
						
					}
					
					
					
					if (anyBeat) {
						if (mBound && mBeater != null)
							mBeater.sendEmptyMessage(0);
						
						
					}
					
					
					for (int iii = 0; iii < FRAME_WIDTH/2; iii++)
						a[iii] = e[iii]*(0.5d * (1-Math.cos(2*Math.PI*iii/FRAME_WIDTH)));
					

					
					
				}
				
				
				ar.stop();
				ar.release();
				
				Log.d("MusicFragment", "Stopped");
				
			}
		}).start();
		
		
		(new Thread() {
			@Override
			public void run() {
				setPriority(Thread.MAX_PRIORITY-1);
				
				FFT fft = new FFT(BEAT_MEMORY);
				FFT fft2 = new FFT(BEAT_MEMORY*2);
				
				long[] times = new long[BEAT_MEMORY];
				
				double thisPow = 0;
				double last = 0;

				double[] window = new double[INTENSITY_MEMORY /10];
				
				for (int iii = 0; iii < window.length; iii++) {
					window[iii] = 0.5d * (1-Math.cos(2*Math.PI*(iii+window.length)/(window.length*2)));
				}
				
				while (mMusicing && runningNumber == mRunningNumber) {
					Arrays.fill(g, 0);
					synchronized (mSumLastSecond) {
						int index = 0;
						last = 0;
						double deriv = 0;
						for (int iii = 0; iii < BEAT_MEMORY; iii++) {
							thisPow = 0;
							
							
							
							for (int jjj = 0; jjj < window.length; jjj++) {
								index = iii + mBeatPos + 1 + jjj;
								while (index >= BEAT_MEMORY)
									index -= BEAT_MEMORY;
								thisPow += window[jjj] * mSumLastSecond[index];
							}
							
							deriv = thisPow - last;
							
							c[iii] = deriv;
							g[iii + g.length/4] = deriv;
							times[iii] = mLastSecTimes[index];
							last = thisPow;
						}
					}
//					
					Arrays.fill(d, 0);
					Arrays.fill(h, 0);
					
					fft.fft(c, d);
					
					for (int iii = 0; iii < BEAT_MEMORY; iii++) {
						
						c[iii] = c[iii]*c[iii] + d[iii]*d[iii];
					}
					
					Arrays.fill(d, 0);
					
					fft.ifft(c, d);
					

					Arrays.fill(d, 0);

					int len = INTENSITY_MEMORY/(60/60);
					if (len > BEAT_MEMORY/2)
						len = BEAT_MEMORY/2;
					
					double avg = 0;
					
					for (int iii = (int) (INTENSITY_MEMORY/(210d/60d)); iii < len; iii++) {
						d[iii] = c[iii];
					}
					
					Arrays.sort(d);
					
					double cut = d[d.length - PEAKS];;
					
					
					double[] peaks = new double[BEAT_MEMORY/4];	// this would be a lol case
					final double[] magnitudes = new double[BEAT_MEMORY/4];
					int peaksFound = 0;
					
					boolean isAboveCut = false;
					
					double x1, x2, x3, y1, y2, y3, denom, A, B, C, xv, yv, half, intHalf;
					
					
					for (int iii = (int) (INTENSITY_MEMORY/(210d/60d)); iii < len; iii++) {
						isAboveCut = c[iii] > cut;
						
						if (isAboveCut) {
							
							x1 = iii-1;
							x2 = iii;
							x3 = iii+1;
							y1 = c[iii-1];
							y2 = c[iii];
							y3 = c[iii+1];
							
							denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
							A     = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
							B     = (x3*x3 * (y1 - y2) + x2*x2 * (y3 - y1) + x1*x1 * (y2 - y3)) / denom;
							C     = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

							
							xv = -B / (2*A);
							yv = C - B*B / (4*A);
							
							if (xv < x1) {
								xv = x1;
								yv = c[(int)xv];
							}
							if (xv > x3) {
								xv = x3;
								yv = c[(int)xv];
							}
							

							if (A > 0) {
								xv = x2;
								yv = c[(int)xv];
							}

							peaks[peaksFound] = xv;
							magnitudes[peaksFound] = yv;
							
							half = xv*2;
							intHalf = (int)(xv*2);
							
							magnitudes[peaksFound] += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
							

							half = xv*4;
							intHalf = (int)(xv*4);
							
							magnitudes[peaksFound] += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
							
							
							peaksFound++;
							
						}
						
//						if (inPeak) {
//							if (isAboveCut) {
//								if (c[iii] > currentPeak) {
//									currentPeak = c[iii];
//									currentPeakPos = iii;
//								}
//								
//							} else {
//								peaks[peaksFound++] = currentPeakPos;
//								inPeak = false;
//							}
//						} else {
//							if (isAboveCut) {
//								inPeak = true;
//								currentPeak = c[iii];
//								currentPeakPos = iii;
//							}
//						}
						
					}
					
					Integer[] sortInd = new Integer[peaksFound];
					for (int iii = 0; iii < peaksFound; iii++) 
						sortInd[iii] = iii;
					
					Arrays.sort(sortInd, new Comparator<Integer>() {
						@Override
						public int compare(Integer lhs, Integer rhs) {
							if (magnitudes[lhs] > magnitudes[rhs])
								return 1;
							else
								return -1;
						}
						
					});
					
					
					
					if (peaksFound > 0) {
						
						for (int iii = 0; iii < KEEP-1; iii++) {
							for (int jjj = 0; jjj < TOPS; jjj++) {
								mTops[iii][jjj] = mTops[iii+1][jjj];
							}
						}
						
						int added = 0;
						
						for (int iii = 0; iii < peaksFound; iii++) {
							if (added == TOPS)
								break;
							
							xv = peaks[sortInd[peaksFound - iii - 1]];
							
//							boolean changed = true;
//							while (changed) {
//								changed = false;
//								double halfMag = 0;
//								
//								
//								
//								half = xv/2;
//								intHalf = (int)(xv/2);
//								
//								halfMag += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
//			
//								half = xv;
//								intHalf = (int)(xv);
//								
//								halfMag += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
//			
//								
//								half = xv*2;
//								intHalf = (int)(xv*2);
//								
//								halfMag += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
//								
//								
//								if (halfMag > lowestMag && INTENSITY_MEMORY / (xv/2) < 210f/60f ) {
//									xv = xv/2;
//									changed = true;
//								}
//								
//							}
							
							if (xv < 0.1f)
								continue;
							
							boolean tooClose = false;
							
							for (int jjj = 0; jjj < iii && jjj < TOPS; jjj++) {
								if (Math.abs(mTops[KEEP-1][jjj] - xv)*60d < TOP_GAP) {
									tooClose = true;
									break;
								}
							}
							
							if (tooClose)
								continue;
							
							mTops[KEEP-1][added++] = xv;
							
						}
						
						int[][] foundPatterns = new int[300][];
						double[] foundAvg = new double[300];
						int found = 0;
						
						int[] pat = new int[KEEP];
						
						boolean more = true;
						while (more) {
							avg = 0;
							for (int iii = 0; iii < KEEP; iii++)
								avg += mTops[iii][pat[iii]];
							
							avg /= KEEP;
							
							boolean allMatch = true;
							
							for (int iii = 0; iii < KEEP; iii++) {
								if (mTops[iii][pat[iii]] < 60/60 || Math.abs(mTops[iii][pat[iii]] - avg)*60d > SAME) {
									allMatch = false;
									break;
								}
							}
							
							if (allMatch) {
								foundPatterns[found] = new int[KEEP];
								for (int iii = 0; iii < KEEP; iii++)
									foundPatterns[found][iii] = pat[iii];
								foundAvg[found] = avg;
								found++;
							}
							
							
							for (int iii = 0; iii < KEEP; iii++) {
								if (++pat[iii] == TOPS) {
									pat[iii] = 0;
									if (iii == KEEP - 1)
										more = false;
								} else
									break;
							}
						}
						
						
						final int[] foundScores = new int[found];
						Integer[] scoreInd = new Integer[found];
						
						
						for (int iii = 0; iii < found; iii++) {
							scoreInd[iii] = iii;
							int score = 0;
							for (int jjj = 0; jjj < KEEP; jjj++) {
								score += foundPatterns[iii][jjj];
							}
							foundScores[iii] = score;
						}
						
						Arrays.sort(scoreInd, new Comparator<Integer>() {
							@Override
							public int compare(Integer lhs, Integer rhs) {
								return foundScores[lhs] - foundScores[rhs];
							}
						});
						
						int combs = COMBS;
						if (found < combs)
							combs = found;
						
						int cacheCombs = 0;
						for (int iii = 0; iii < CACHE; iii++) {
							if (mFreqCache[iii] != 0)
								cacheCombs++;
						}
					
						int pastCombs = 0;
						for (int iii = 0; iii < mPastSuccess.length; iii++) {
							if (mPastSuccess[iii] > 0.05f)
								pastCombs++;
						}
						
						double[] combsToTry = new double[combs + cacheCombs + pastCombs];
//						double[] combsToTry = new double[(210-60)/10*4 + 1 + pastCombs];
						added = 0;
						for (int iii = 0; iii < combs; iii++) {
							combsToTry[added++] = INTENSITY_MEMORY / foundAvg[scoreInd[iii]];
						}
						for (int iii = 0; iii < mFreqCache.length; iii++) {
							if (mFreqCache[iii] != 0) {
								combsToTry[added++] = mFreqCache[iii];
							}
						}
//						for (int iii = 0; iii < (210-60)/10*4 + 1; iii++) {
//							combsToTry[added++] = (60d + 2.5d*iii)/60d;
//						}
						for (int iii = 0; iii < mPastSuccess.length; iii++) {
							if (mPastSuccess[iii] > 0.05f) {
								combsToTry[added++] = mPastSuccess[iii];
							}
						}
						
						
						double bestComb = 0;
						double bestFrequency = 0;
						
						Arrays.fill(h, 0);
						fft2.fft(g, h);
						
						
//						if (combs > 5) {
//							boolean stop = true;
//							while (stop)
//								try {
//									sleep(1);
//								} catch (Exception e) {
//									
//								}
//						}
						
						long timeStamp = 0;
//						
						
						int startPos = g.length/4 - COMB_START;
						int endPos = (g.length/4)*3 - (int)(INTENSITY_MEMORY / (60f/60f)) * (COMB_SPIKES - 1) - COMB_START;
						
						
//						final double[] results = new double[(210-60)/10*4 + 1];
						
						for (int iii = 0; iii < combsToTry.length; iii++) {
							double frequency = combsToTry[iii];
							makeComb(i, INTENSITY_MEMORY / frequency);
							Arrays.fill(j, 0);
							
							fft2.fft(i, j);
							
							// (g + h_i_) * (i - j_i_) 
							
							double oldI = 0;
							for (int jjj = 0; jjj < i.length; jjj++) {
								oldI = i[jjj];
								i[jjj] = oldI*g[jjj] + h[jjj]*j[jjj];
								j[jjj] = h[jjj]*oldI - g[jjj]*j[jjj];
							}
							
							fft2.ifft(i, j);
							
							double thisMax = 0;
							for (int jjj = startPos; jjj < endPos; jjj++) {
								thisMax += Math.pow(Math.abs(i[jjj]), 1.8d);
							}
							
							
							if (thisMax > bestComb) {
								bestComb = thisMax;
								bestFrequency = frequency;
								
								
							}
							
							
//							if (iii < results.length)
//								results[iii] = thisMax;
						}
						
						
						
//						Integer[] resultsInd = new Integer[results.length];
//						for (int iii = 0; iii < resultsInd.length; iii++) {
//							resultsInd[iii] = iii;
//						}
//						
//						boolean changed = true;
//						
////						int tot = (210-60)/10*4 + 1;
//						
//						while (changed) {
//							changed = false;
//
//							Arrays.sort(resultsInd, new Comparator<Integer>() {
//								@Override
//								public int compare(Integer lhs, Integer rhs) {
//									if (results[lhs] > results[rhs])
//										return 1;
//									else 
//										return -1;
//								}
//							});
//							
//							
//							for (int iii = 0; iii < 3; iii++) {
//								double freq = (resultsInd[resultsInd.length - 1 - iii]*2.5d + 60d)/60d;
//								for (int jjj = 0; jjj < 3; jjj++) {
//									if (Math.abs((resultsInd[resultsInd.length - 1 - jjj]*2.5d + 60d)/60d - freq*2) < 1d) {
//										results[resultsInd[resultsInd.length - 1 - jjj]] += results[resultsInd[resultsInd.length - 1 - iii]];
//										results[resultsInd[resultsInd.length - 1 - iii]] = 0;
//										changed = true;
//										break;
//									}
//								}
//								if (changed)
//									break;
//							}
//							
//						}
						
						
//						bestFrequency = (resultsInd[resultsInd.length - 1 - 0] *2.5d + 60d)/60d;
						
						if (true) { //combs > 0) {
	
							double frequency = bestFrequency;
							mFreqCache[mFreqCachePos] = frequency;
							mFreqCachePos++;
							if (mFreqCachePos >= CACHE)
								mFreqCachePos = 0;
							
							String str = Double.toString(frequency*60) + " bpm / ";
							
//							if (Math.abs(frequency - 205/60f) *60f < 10) {
//								start++;
//								str = Long.toString(start) + " " + str;
//							}
							
							for (int iii = 0; iii < combs; iii++) {
								str += Integer.toString((int) (60* INTENSITY_MEMORY / foundAvg[scoreInd[iii]])) + " ";
							}
							
							Log.d("MusicFragment", str);
							
							
//							if (mRunning && mActivity.mService != null && frequency < 10 && frequency > 0.05f) {
//								mActivity.startFlashing(new int[] {50000, (int) (1000000/frequency - 50000)}, true);
//							}
							
							
							
							
							boolean newTempo = false;
							boolean concurrent = false;
							

							if (Math.abs(frequency*2 - mLastFreq)*60d < 5f) {
								frequency *= 2;
							}
							
							for (int iii = 0; iii < mPast.length-1; iii++) {
								mPast[iii] = mPast[iii+1];
								if (Math.abs(frequency*2 - mPast[iii]) *60d < 5f) {
									frequency *= 2;
								}
							}
							
							mPast[mPast.length-1] = frequency;
							
							avg = 0;
							for (int iii = 0; iii < mKeepers.length; iii++) {
								avg += mKeepers[iii];
							}
							
							avg /= NEEDED;
							
							double theAvg = avg;
							
							if (Math.abs(avg - frequency)*60d < CONCURRENT) {
								for (int iii = 0; iii < mKeepers.length-1; iii++) {
									mKeepers[iii] = mKeepers[iii+1];
								}
								mKeepers[NEEDED-1] = frequency;
								concurrent = true;
							} else {
								
								Integer[] pastInd = new Integer[mPast.length];
								for (int iii = 0; iii < pastInd.length; iii++) {
									pastInd[iii] = iii;
								}
								Arrays.sort(pastInd, new Comparator<Integer>() {
									@Override
									public int compare(Integer lhs, Integer rhs) {
										if (mPast[lhs] > mPast[rhs]) {
											return 1;
										} else
											return -1;
									}
								});
								
								int width = (NEEDED - 1) / 2;
								final double[] gaps = new double[mPast.length - NEEDED + 1];
								
								for (int iii = 0; iii < gaps.length; iii++) {
									gaps[iii] = mPast[pastInd[iii + NEEDED - 1]] - mPast[pastInd[iii]];
									if (mPast[pastInd[iii]] < 0.05f)
										gaps[iii] = 100;
									if (theAvg > mPast[pastInd[iii]] && theAvg < mPast[pastInd[iii + NEEDED - 1]])
										gaps[iii] = 100;
								}
								
								Integer[] gapsInd = new Integer[gaps.length];
								for (int iii = 0; iii < gapsInd.length; iii++) {
									gapsInd[iii] = iii;
								}
								

								Arrays.sort(gapsInd, new Comparator<Integer>() {
									@Override
									public int compare(Integer lhs, Integer rhs) {
										if (gaps[lhs] > gaps[rhs]) {
											return 1;
										} else
											return -1;
									}
								});
								
								if (gaps[gapsInd[0]]*60 < 10) {
									int set = gapsInd[0];
									newTempo = true;
									for (int iii = 0; iii < NEEDED; iii++) {
										mKeepers[iii] = mPast[pastInd[set + iii]];
									}
								}
								
								
//								int skip = 0;
//								for (skip = 0; skip < mPast.length-1; skip++) {
//									avg = 0;
//									for (int iii = 0; iii < mPast.length; iii++) {
//										if (iii != skip)
//											avg += mPast[iii];
//									}
//									avg /= FINAL_KEEP;
//									boolean anyBad = false;
//									for (int iii = 0; iii < mPast.length; iii++) {
//										if (iii != skip) {
//											if (Math.abs((avg-mPast[iii])/avg) > SAME) {
//												anyBad = true;
//												break;
//											}
//										}
//									}
//									
//									if (!anyBad) {
//										added = 0;
//										for (int iii = 0; iii < mPast.length; iii++) {
//											if (iii != skip)
//												mKeepers[added++] = mPast[iii];
//										}
//										newTempo = true;
//										break;
//									}
//									
//									
//								}
								
								
							}
							

							avg = 0;
							for (int iii = 0; iii < mKeepers.length; iii++) {
								avg += mKeepers[iii];
							}
							
							avg /= NEEDED;
							
							if (concurrent) {
								Log.d("MusicFragment", "Concurrent " + Double.toString(avg*60) + " bpm");
							}
							if (newTempo) {
								Log.d("MusicFragment", "New tempo " + Double.toString(avg*60) + " bpm " + Long.toString(System.currentTimeMillis() - mStart));
								for (int iii = 0; iii < mPastSuccess.length-1; iii++) {
									mPastSuccess[iii] = mPastSuccess[iii+1];
								}
							
							}
							
							mPastSuccess[mPastSuccess.length - 1] = avg;
							
							
							
							frequency = avg;
							
							if ((newTempo || concurrent) && mMusicing && frequency < 10 && frequency > 0.05f) {
								controlUpdate(new int[] {50000, (int) (1000000/frequency - 50000)}, true, false, true, 
										MainActivity.S_OTHER);
								mLastFreq = frequency;
								if (mBound && mBpmUpdater != null)
									mBpmUpdater.sendEmptyMessage((int)(frequency*60));
								if (newTempo || mSinceSync == 20) {
									
									int firstPeak = makeComb(i, INTENSITY_MEMORY / frequency);
									Arrays.fill(j, 0);
									
									fft2.fft(i, j);
									
									// (g + h_i_) * (i - j_i_) 
									
									double oldI = 0;
									for (int jjj = 0; jjj < i.length; jjj++) {
										oldI = i[jjj];
										i[jjj] = oldI*g[jjj] + h[jjj]*j[jjj];
										j[jjj] = h[jjj]*oldI - g[jjj]*j[jjj];
									}
									
									fft2.ifft(i, j);
									
									double highest = 0;
									int highestPos = 0;
									for (int jjj = startPos; jjj < endPos; jjj++) {
										if (i[jjj] > highest) {
											highest = i[jjj];
											highestPos = jjj;
										}
									}
									
									
										
									try {
										timeStamp = times[highestPos - g.length/4 + firstPeak];
										syncTime((long) (timeStamp ));// + 500/frequency));
									} catch (Exception e) {
										e.printStackTrace();
									}
									
									
//									if (frequency*60 > 139 && frequency*60 < 140) {
//										boolean stop = true;
//										while (stop) {
//											try {
//												sleep(1);
//											} catch (Exception e) {}
//										}
//									}
//										
									
									
									
//									syncTime((long) (timeStamp ));// + 500/frequency));
								}
							}
							

							mSinceSync ++;
							
							
							
						}
						
						
						
					}
					
					
					
//					
//					
//					double highestMag = 0;
//					int highestIndex = 0;
//					
//					for (int iii = 0; iii < peaksFound; iii++) {
//						if (magnitudes[iii] > highestMag) {
//							highestMag = magnitudes[iii];
//							highestIndex = iii;
//						}
//						if (magnitudes[iii] < lowestMag)
//							lowestMag = magnitudes[iii];
//					}
//
//					xv = peaks[highestIndex];
//					
//					boolean changed = true;
//					while (changed) {
//						changed = false;
//						double halfMag = 0;
//						
//						
//						
//						half = xv/2;
//						intHalf = (int)(xv/2);
//						
//						halfMag += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
//	
//						half = xv;
//						intHalf = (int)(xv);
//						
//						halfMag += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
//	
//						
//						half = xv*2;
//						intHalf = (int)(xv*2);
//						
//						halfMag += (c[(int) intHalf + 1] - c[(int) intHalf]) * (half - intHalf) + c[(int)intHalf];
//						
//						
//						if (halfMag > lowestMag && INTENSITY_MEMORY / (xv/2) < 210f/60f ) {
//							xv = xv/2;
//							changed = true;
//						}
//						
//					}
////					
////					Arrays.sort(magnitudes);
////					
////					cut = 0;
////					if (peaksFound > 15) {
////						cut = magnitudes[peaksFound-15];
////					}
////					
//////					if (peaksFound > 0)
//////						mMax = magnitudes[peaksFound-1];
////					
////					ArrayList<FreqFamily> list = new ArrayList<FreqFamily>();
////					
////					for (int iii = 0; iii < peaksFound; iii++) {
////						if (c[peaks[iii]] < cut)
////							continue;
////						double freq = INTENSITY_MEMORY/(double)peaks[iii];
////						boolean matched = false;
////						for (int jjj = 0; jjj < list.size(); jjj++) {
////							if (list.get(jjj).isHarmonic(freq, c[peaks[iii]], false)) {
////								matched = true;
////							}
////						}
////						if (!matched) {
////							list.add(new FreqFamily(freq, c[peaks[iii]]));
////						}
////					}
////					
////					FreqFamily[] toRet = list.toArray(new FreqFamily[list.size()]);
////					if (toRet.length > 0) {
////						Arrays.sort(toRet, new Comparator<FreqFamily>() {
////							@Override
////							public int compare(FreqFamily lhs, FreqFamily rhs) {
//////								if (lhs.mHarmonics != rhs.mHarmonics)
//////									return lhs.mHarmonics - rhs.mHarmonics;
////								
////								if (lhs.mMagSum > rhs.mMagSum)
////									return 1;
////								else
////									return -1;
////							}
////							
////						});
//						
//					
					
					try {
						sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
				
				
			}
		}).start();
		
		
		
		
	}
	
	
	private double[][] mTops = new double[KEEP][];
	
//	private double[] mKeepers = new double[FINAL_KEEP];
//	private double[] mPast = new double[FINAL_KEEP + 1];
	
	private double[] mKeepers = new double[NEEDED];
	private double[] mPast = new double[FINAL_KEEP];
	
	private double[] mPastSuccess = new double[10];
	
	
	

	private static final int COMB_START = 10;
	private static final int COMB_SPIKES = 3;
	
	public static int makeComb(double[] buf, double byteFrequency) {
		if (byteFrequency > 300) {
			Log.d("Music Fragment", "Bad comb!!!");
			boolean stop = true;
			while (stop) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		Arrays.fill(buf, 0);
		if (byteFrequency == 0)
			return 0;
//		for (int iii = 0; iii < byteFrequency*3; iii++) {
//			buf[iii] = Math.sqrt(2 + 2*Math.cos(2*Math.PI/byteFrequency*(iii-byteFrequency/2)));
//		}
		
		for (int iii = 0; iii < COMB_SPIKES; iii++) {
			int start = (int)(byteFrequency * (iii)) + COMB_START;
			for (int jjj = 0; jjj < 5+1; jjj++) {
				buf[start + jjj] = 1;
				buf[start - jjj] = 1;
			}
		}
		
		return (int)10;
	}
	
	
	
	
}
