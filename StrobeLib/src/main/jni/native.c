




#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stdio.h>
#include <time.h>

#define DEBUG_TAG "Strobe_NDK"

#define C_ON 	0
#define C_OFF 	1
#define C_OFF2 	2



static long nOnLen = 50000;
static long nOffLen = 50000;
static int bFlashing = 0;
static int bKilled = 0;
static int bScreen = 0;
static int bAbort = 0;
static int bTorch = 0;
static int mFlashOn = 0;
static int mScreenOn = 0;
static jlong lNextFlash = 0;
static int nLateThreshold = 10000000;
static int nFlashPreempt = 500;
static int nBurst = -1;
static int nLag = 0;
static int bNextScreen = 0;
static int bMultiKill = 1;




static jobject jOffParam = 0;
static jobject jOnParam = 0;
static jobject jOffParam2 = 0;

static jobject jOffFlatten;
static jobject jOffFlatten2;
static jobject jOnFlatten;

static jobject jCamera;

static jclass clsString;
static jclass clsCamera;
static jmethodID midSetParameters;
static jmethodID midNativeSetParameters;

static int bFlatten;


jlong elapsed(jlong);
void delay(jlong);
void msleep2(long);
void _nsleep(const struct timespec *);
jlong timespecToJlong(const struct timespec *);
void changeCamera(JNIEnv *, int);


jlong elapsed(jlong lTime)
{
	struct timespec time;

	clock_gettime(CLOCK_MONOTONIC, &time);
	return timespecToJlong(&time) - lTime;

}


void delay(jlong lFinishTime)
{
	struct timespec time;
	int bReady = 0;
	jlong original;
	jlong micros;

	clock_gettime(CLOCK_MONOTONIC, &time);
	micros = lFinishTime - timespecToJlong(&time);
	original = micros;

	if (micros < 1000)
		return;


	while (micros > 1000000 && !bAbort)
	{
		msleep2(1000000);
		micros-= 1000000;
	}

	if (micros > 500000 && bAbort)
		micros = 500000;

	if (micros - nFlashPreempt > 0)
		msleep2(micros-nFlashPreempt);
	while (!bReady && !(original > 1000000 && bAbort))
	{
		clock_gettime(CLOCK_MONOTONIC, &time);
		bReady = (lFinishTime < timespecToJlong(&time));
	}
}

void msleep2(long micros)
{
	struct timespec time={0};
	time.tv_sec = micros/1000000;
	time.tv_nsec = (micros%1000000)*1000;
	_nsleep(&time);
}

void _nsleep(const struct timespec *req)
{
	struct timespec temp_rem;
	if (nanosleep(req, &temp_rem)==-1)
		_nsleep(&temp_rem);

}

jlong timespecToJlong(const struct timespec *ts)
{
	return (jlong)(ts->tv_sec)*1000000 + (jlong)(ts->tv_nsec/1000);
}


void changeCamera(JNIEnv * env, int mode)
{
	if (bFlatten)
	{
		switch (mode)
		{
		case C_ON:
			(*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOnFlatten);
			break;
		case C_OFF:
			(*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOffFlatten);
			break;
		case C_OFF2:
			(*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOffFlatten2);
			break;
		}
	}
	else
	{
		switch (mode)
		{
		case C_ON:
			(*env)->CallVoidMethod(env, jCamera, midSetParameters, jOnParam);
			break;
		case C_OFF:
			(*env)->CallVoidMethod(env, jCamera, midSetParameters, jOffParam);
			break;
		case C_OFF2:
			(*env)->CallVoidMethod(env, jCamera, midSetParameters, jOffParam2);
			break;
		}
	}
}


void Java_com_tp77_StrobeLib_StrobeLibService_doStrobe(JNIEnv * env, jobject this, jobject objCamera, jobject objOnParam, jobject objOffParam, jobject objOffParam2,
		jobject objOnFlatten, jobject objOffFlatten, jobject objOffFlatten2, jboolean objFlatten)
{


	jclass clsStrobeActivity;
	jmethodID midSendScreenState;
	int iii;
	int mLastOffCount;
	int nOffDoubler;
	int nOffCount;
	int bWhichOff;
	jboolean jbScr;
	jstring jsFlashMode;
	jobject objParameters;



	jOffParam = objOffParam;
	jOffParam2 = objOffParam2;
	jOnParam = objOnParam;

	jOnFlatten = objOnFlatten;
	jOffFlatten = objOffFlatten;
	jOffFlatten2 = objOffFlatten2;
	bFlatten = objFlatten;

	jCamera = objCamera;



	bKilled = 0;
	bFlashing = 0;
	bScreen = 0;

	if (objCamera != 0) {
		clsString = (*env)->FindClass(env, "java/lang/String");
		clsCamera = (*env)->FindClass(env, "android/hardware/Camera");
		midSetParameters = (*env)->GetMethodID(env, clsCamera, "setParameters",
					"(Landroid/hardware/Camera$Parameters;)V");
		if (bFlatten)
		{
			midNativeSetParameters = (*env)->GetMethodID(env, clsCamera, "native_setParameters",
					"(Ljava/lang/String;)V");
		}

	}

	clsStrobeActivity = (*env)->FindClass(env, "com/tp77/StrobeLib/StrobeLibService");
	midSendScreenState = (*env)->GetMethodID(env, clsStrobeActivity, "sendScreenState", "(Z)V");





	while (!bKilled)
	{
		if (bAbort) {
			lNextFlash = elapsed(0);
			bAbort = 0;
		}
		if (bFlashing)
		{
			if (!bScreen)
			{
				if (objCamera != 0 && (elapsed(0) < lNextFlash + nLateThreshold))
				{
					if (nOnLen + nOffLen > 50000 && bMultiKill) {
						if (nOnLen > 50000)
							changeCamera(env, C_ON);
						if (nOnLen > 30000)
							changeCamera(env, C_ON);
						if (nOnLen > 10000)
							changeCamera(env, C_ON);
					}
					changeCamera(env, C_ON);
					mFlashOn = 1;

					if (nBurst > 0)
					{
						if (--nBurst == 0)
							bFlashing = 0;
					}

				}
			}
			else
			{
				jbScr = 1;
				(*env)->CallVoidMethod(env, this, midSendScreenState, jbScr);
				mScreenOn = 1;

				if (nBurst > 0)
				{
					if (--nBurst == 0)
						bFlashing = 0;
				}
			}
			if (nOnLen)
			{
				lNextFlash+= nOnLen;
				delay(lNextFlash);
			}

			if (!bKilled && !bTorch)
			{
				if (!bScreen)
				{
					if (objCamera != 0)		//tested to be .75 ms, call it 2.5 to be safe
					{
//						if (nOffLen > 40000)
//							changeCamera(env, C_OFF2);
//						if (nOffLen > 25000)
//							changeCamera(env, C_OFF);
//						if (nOffLen > 15000)
//							changeCamera(env, C_OFF2);
//						if (nOffLen > 6000)
//							changeCamera(env, C_OFF);
//						if (nOffLen > 3000)
//							changeCamera(env, C_OFF2);

						changeCamera(env, C_OFF);


						nOffDoubler = 250;
						bWhichOff = 0;

						while (nOffLen - elapsed(lNextFlash) > nOffDoubler && bMultiKill)
						{
							if (nOffLen - elapsed(lNextFlash) > nOffDoubler + 5000)
							{
								nOffCount = nOffDoubler;
								while (nOffCount > 500000 && !bAbort)
								{
									msleep2(nOffCount);
									nOffCount -= 500000;
								}
								if (!bAbort && nOffCount > 0)
									msleep2(nOffCount);

								changeCamera(env, bWhichOff ? C_OFF : C_OFF2);
							}
							nOffDoubler*= 2;
							bWhichOff^= 1;
						}



						mFlashOn = 0;
					}
				}
				else
				{
					jbScr = 0;
					(*env)->CallVoidMethod(env, this, midSendScreenState, jbScr);
					mScreenOn = 0;
				}
				lNextFlash+= nOffLen;
				while (elapsed(0) > lNextFlash + nOffLen + nOnLen)		//de-lagger
				{
					lNextFlash+= nOffLen + nOnLen;
					nLag++;
				}

				delay(lNextFlash);
			}
		}
		if (!bKilled && !bFlashing)
		{
			if (bTorch && ((bScreen && mScreenOn < 2) || (!bScreen && mFlashOn < 2)))
			{

				if (!bScreen)
				{
					if (objCamera != 0)
					{
						changeCamera(env, C_ON);
						mFlashOn++;
					}
				}
				else
				{
					jbScr = 1;
					(*env)->CallVoidMethod(env, this, midSendScreenState, jbScr);
					mScreenOn++;
				}

			}

			if (mFlashOn > -3 && !(bTorch && !bScreen))
			{
				if (objCamera != 0)
				{
					msleep2(50000);
					changeCamera(env, C_OFF2);
					msleep2(50000);
					changeCamera(env, C_OFF);
					if (mFlashOn > 0)
						mFlashOn = 0;
					else
						mFlashOn--;
				}
			}
			if (mScreenOn > -3 && !(bTorch && bScreen))
			{
				jbScr = 0;
				(*env)->CallVoidMethod(env, this, midSendScreenState, jbScr);
				if (mScreenOn > 0)
					mScreenOn = 0;
				else
					mScreenOn--;
			}

			if (!bAbort)
				msleep2(100000);
			lNextFlash = elapsed(0); // clever way of getting the current time
		}

		bScreen = bNextScreen;

	}



}

void Java_com_tp77_StrobeLib_StrobeLibService_update(JNIEnv * env, jobject this, jint jOnLen, jint jOffLen, jboolean jFlashing, jboolean jUseScreen, jboolean jTorch,
		jint jNewBurst, jint jLateThreshold, jint jFlashPreempt, jboolean jMultiKill)
{
	nOnLen = jOnLen;
	nOffLen = jOffLen;
	bFlashing = jFlashing;
	bNextScreen = jUseScreen;
	bTorch = jTorch;
	if (jNewBurst != 0)
		nBurst = jNewBurst;
	if (nBurst > 0)
		bFlashing = 1;
	if (jLateThreshold == -1)
		jLateThreshold = 10000000;
	nLateThreshold = jLateThreshold;
	nFlashPreempt = jFlashPreempt;
	bMultiKill = jMultiKill;
	bAbort = 1;

}


void Java_com_tp77_StrobeLib_StrobeLibService_kill(JNIEnv * env, jobject this)
{
	bKilled = 1;
}


jint Java_com_tp77_StrobeLib_StrobeLibService_debugNumber(JNIEnv * env, jobject this)
{
	return sizeof(long);
}

jint Java_com_tp77_StrobeLib_StrobeLibService_checkLag(JNIEnv * env, jobject this)
{
	jint toReturn = nLag;
	nLag = 0;
	return toReturn;
}



