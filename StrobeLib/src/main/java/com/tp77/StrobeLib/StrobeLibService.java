package com.tp77.StrobeLib;

import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v42.app.NotificationCompat;

public class StrobeLibService extends Service {

	
	private LocalBinder mTheIBinder;
	
	private PowerManager mPowerManager;
	private WakeLock mWakeLock;
	
	public int mBaseFrequency;
	public float mFineTune;
	public float mDuty;
	public int mOnLength;
	public boolean mSteady;
	public boolean mUseScreen;
	public int mLateThreshold;
	public int mFlashPreempt;
	public boolean mMultiKill;
	
	
	public boolean mRunning;
	public boolean mTorch;
	
	public Thread mStroberThread;
	
	public boolean mBound = false;
	
	public Camera mCamera;
	public Camera.Parameters mParamOn;
	public Camera.Parameters mParamOff;
	public Camera.Parameters mParamOff2;
	
	public Handler mScreenPost;
	private Message mScrMsg;
	public boolean mForeground;
	
	private long mScr;
	private long mScr2;
	
	
	
	static {
		System.loadLibrary("NDK2");	
	}


	private native void doStrobe(Camera camera, Camera.Parameters onParams, Camera.Parameters offParams, Camera.Parameters offParams2, 
			String onFlatten, String offFlatten, String offFlatten2, boolean useFlatten);
	private native void update(int onDuration, int offDuration, boolean enabled, boolean useScreen, boolean torch, int newBurst,
			int lateThreshold, int flashPreempt, boolean multiKill);
	private native void kill();
	private native int debugNumber();
	private native int checkLag();
	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		mTheIBinder = new LocalBinder();
		
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Strobe service wake lock");
		
		
	}
	
	
	@Override
	public void onDestroy() {
		stopTheStrober(false);
		endForeground();
		
		if (!mBound) {
			if (mCamera != null) {
				try {
					mCamera.stopPreview();
				} catch (Exception e) {}
				mCamera.release();
			}
		}
		setWakeLock(false);
	}
	
	@Override
	public int onStartCommand(Intent _intent, int flags, int startId) {
		return START_STICKY;
	}
	
	public void goForeground() {
		
	    NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
	    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

		Intent intent = new Intent(this, StrobeLibActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
	    Notification notification  = builder
	            .setContentIntent(PendingIntent.getActivity(
	    				this, 0, intent, 0))
	            .setSmallIcon(R.drawable.ic_notif)
	            .setWhen( System.currentTimeMillis())
	            .setContentTitle("Strobe")
	            .setContentText("Running...").build();
	    notificationManager.notify(2, notification);
		
		mForeground = true;
	}
	
	public void endForeground() {
		if (!mForeground)
			return;
		stopForeground(true);
	}
	
	private void sendScreenState(boolean on) {
		if (mBound && mScreenPost != null) {
	    	mScrMsg = mScreenPost.obtainMessage();
	    	mScrMsg.arg1 = (on ? StrobeLibActivity.SCREEN_ON : StrobeLibActivity.SCREEN_OFF);
	    	mScreenPost.sendMessage(mScrMsg);
		}
	}
	
	public void stopTheStrober(boolean i_am_the_strober) {
		mRunning = false;
		mTorch = false;
		kill();
		while (!i_am_the_strober && mStroberThread != null && mStroberThread.isAlive())
			kill();
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
    	
    	
		mStroberThread = (new Thread() {
    		@Override
    		public void run() {
    			setPriority(MAX_PRIORITY);
    			try {
					doStrobe(mCamera, mParamOn, mParamOff, mParamOff2,
							mParamOn != null ? mParamOn.flatten() : null,
							mParamOff != null ? mParamOff.flatten() : null,
							mParamOff2 != null ? mParamOff2.flatten() : null, finalDoFlatten);
				} catch (Exception e) {
    				e.printStackTrace();

					if ( StrobeLibService.this.mForeground ) {
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
				}
    		}
    	});
    	mStroberThread.start();
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

	
	public void updateStrober(int burst) {
		if (mRunning || mTorch) 
			setWakeLock(true);
		else
			setWakeLock(false);
		
		
		
		update(calculateTime(true), calculateTime(false), mRunning, mUseScreen, mTorch, burst, mLateThreshold, mFlashPreempt, mMultiKill);
	}
	
	// this routine has a shadow in the activity
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
	
	public int getDebugNumber() {
		return debugNumber();
	}
	
	public int doCheckLag() {
		return checkLag();
	}
	
	
	public class LocalBinder extends Binder {
		StrobeLibService getService() {
			return StrobeLibService.this;
		}
	};
	
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
	

}
