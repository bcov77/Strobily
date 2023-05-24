
// For future Brian, use this one, the other one realistically should be deleted


#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stdio.h>
#include <time.h>

#define DEBUG_TAG "Strobe_NDK"


#define C_ON 	0
#define C_OFF 	1
#define C_OFF2 	2

static int test = 0;


static int nFlashIndex = 0;
static int nNumFlashes = 0;
static jlong lFlashStart = 0;		// first flash in micros
static int bLoop = 0;				// loop the flashes
static int bLed = 1;
static int bScreen = 0;
static int bAbort = 0;
static int bKilled = 0;
static int bGentle = 1;
static int nScreenOn = 0;
static int nLedOn = 0;
static int nLateThreshold = 10000000;
static int nFlashPreempt = 500;
static int nLag = 0;
static int nLag2 = 0;
static int bTorch = 0;
static int bFlashing = 0;
static jlong lNextFlash = 0;
static int bBurstDone = 0;

static jlong nFlashSync = 0;
static jlong nNextFlashSync = 0;

static int bNextLed = 1;
static int bNextScreen = 0;
static int nNextNumFlashes = 0;
static int bNextFlashing = 0;
static int bNextLoop = 0;
static int bNextTorch = 0;
static int bNextUsingArray = 0;

static int bUsingArray = 0;
static int anFlashes0[10000];
static int anFlashes1[10000];

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
static jmethodID midSendScreenState;


// New camera

static jobject jCamMan;
static jobject jWhichCamera;

static jclass clsCamMan;
static jmethodID midSetTorchMode;



//




static int debug = -1;
static int bFlatten;

static int nOnLen = 0;
static int nOffLen = 0;

#define DI_FRAME_START 0
#define DI_ON 1
#define DI_ONS 2
#define DI_ON_TO_OFF 3
#define DI_OFF 4
#define DI_OFFS 5
#define DI_OFF_TO_ON 6

#define DI_CATEGORIES 7
#define DI_LENGTH 100

#define DI_SIZE DI_CATEGORIES*DI_LENGTH

static jlong anDiagnostics[DI_SIZE+1];
static jint nDiagPos = 0;

static jlong lMultiThreadGo = 0;

static jlong lOnTime = 0;
static jlong lCountStart = 0;
static int totalFlashes = 0;
static int totalSkips = 0;
static int totalLags = 0;

static jlong lOnCommandTime = 0;

jlong elapsed(jlong);
void delay(jlong);
void msleep2(long);
void _nsleep(const struct timespec *);
jlong timespecToJlong(const struct timespec *);
void incrementDiagnostic();
void addDiagnostic(jlong, int);
void clearDiagnostics();
//void Java_com_tp77_StrobeLib_StrobeService_otherThread(JNIEnv * env, jobject this);


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


    if (micros > 500000 && bAbort)
        micros = 500000;

	while (micros > 150000 && !bAbort && !bKilled)
	{
		msleep2(100000);
		micros-= 100000;

        if (micros > 500000 && bAbort)
            micros = 500000;
	}

    if ( bKilled ) return;

	if (micros - nFlashPreempt > 0)
		msleep2(micros-nFlashPreempt);

	while (!bReady && !(original > 1000000 && bAbort) && !bKilled)
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



int changeFlash(JNIEnv * env, jobject this, int mode, int maxOff, int maxOn)
{

	int cameraMode = mode;
	if (nLedOn > maxOff && !bLed)
		cameraMode = C_OFF;
	if ((bLed || nLedOn > maxOff) && ( jCamera || jCamMan )) {
		if (bFlatten)
		{
			switch (cameraMode)
			{
			case C_ON:
				if ((!bGentle && nLedOn < maxOn) || nLedOn < 1) {
					jboolean jb = 1;
					if (jCamera) (*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOnFlatten);
					else         (*env)->CallVoidMethod(env, jCamMan, midSetTorchMode, jWhichCamera, jb);
				}
				if (nLedOn < 1)
					nLedOn = 1;
				else
					nLedOn++;
				break;
			case C_OFF:
				if ((!bGentle && nLedOn > maxOff) || nLedOn > 0) {
					jboolean jb = 0;
					if (jCamera) (*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOffFlatten);
					else         (*env)->CallVoidMethod(env, jCamMan, midSetTorchMode, jWhichCamera, jb);
				}
				if (nLedOn > 0)
					nLedOn = 0;
				else
					nLedOn--;
				break;
			case C_OFF2:
				if ((!bGentle && nLedOn > maxOff) || nLedOn > 0) {
					jboolean jb = 0;
					if (jCamera) (*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOffFlatten2);
					else         (*env)->CallVoidMethod(env, jCamMan, midSetTorchMode, jWhichCamera, jb);
				}
				if (nLedOn > 0)
					nLedOn = 0;
				else
					nLedOn--;
				break;
			}
		}
		else
		{
			switch (cameraMode)
			{
			case C_ON:
				if ((!bGentle && nLedOn < maxOn) || nLedOn < 1) {
					jboolean jb = 1;
					if (jCamera) (*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOnParam);
					else         (*env)->CallVoidMethod(env, jCamMan, midSetTorchMode, jWhichCamera, jb);
				}
				if (nLedOn < 1)
					nLedOn = 1;
				else
					nLedOn++;
				break;
			case C_OFF:
				if ((!bGentle && nLedOn > maxOff) || nLedOn > 0) {
					jboolean jb = 0;
					if (jCamera) (*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOffParam);
					else         (*env)->CallVoidMethod(env, jCamMan, midSetTorchMode, jWhichCamera, jb);
				}
				if (nLedOn > 0)
					nLedOn = 0;
				else
					nLedOn--;
				break;
			case C_OFF2:
				if ((!bGentle && nLedOn > maxOff) || nLedOn > 0) {
					jboolean jb = 0;
					if (jCamera) (*env)->CallVoidMethod(env, jCamera, midNativeSetParameters, jOffParam2);
					else         (*env)->CallVoidMethod(env, jCamMan, midSetTorchMode, jWhichCamera, jb);
				}
				if (nLedOn > 0)
					nLedOn = 0;
				else
					nLedOn--;
				break;
			}

		}
	}
	if ((*env)->ExceptionCheck(env)) {
        return 1;
    }

	int nScreenMode = mode;
	if (!bScreen && nScreenOn > 0)
		nScreenMode = C_OFF;
	if (bScreen || nScreenOn > 0)
	{
		jboolean jbScr;
		switch (mode)
		{
		case C_ON:
			jbScr = 1;
			if (nScreenOn < 1)
				(*env)->CallVoidMethod(env, this, midSendScreenState, jbScr);
			nScreenOn = 1;
			break;
		case C_OFF:
		case C_OFF2:
			jbScr = 0;
			if (nScreenOn > 0)
				(*env)->CallVoidMethod(env, this, midSendScreenState, jbScr);
			nScreenOn = 0;
			break;
		}
	}

	if ((*env)->ExceptionCheck(env)) {
        return 1;
    }

    return 0;
}


void computeThisOnOff() {
    int use_val = abs(nFlashIndex % 5000); // it's unclear why this is needed but we get segfaults without it
	if (bUsingArray)
	{
		nOnLen = anFlashes1[use_val*2+0];
		nOffLen = anFlashes1[use_val*2+1];
	} else
	{
		nOnLen = anFlashes0[use_val*2+0];
		nOffLen = anFlashes0[use_val*2+1];
	}
}

void advanceIndex() {
	nFlashIndex++;
	if (nFlashIndex >= nNumFlashes) {
		if (bLoop)
			nFlashIndex = 0;
		else
		{
			if (bFlashing)
				bBurstDone = 1;
			bFlashing = 0;
			nFlashIndex = nNumFlashes; // prevent crazy roll-over situations
		}
	}
}


void Java_com_tp77_StrobeLib_StrobeService_doStrobe(JNIEnv * env, jobject this, jobject objCamera, jobject objOnParam, jobject objOffParam, jobject objOffParam2,
		jobject objOnFlatten, jobject objOffFlatten, jobject objOffFlatten2, jboolean objFlatten, jobject objCamMan, jobject objWhichCamera)
{

	jmethodID midBurstDone;
	jclass clsStrobeActivity;
	int iii;
	int mLastOffCount;
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


	jCamMan = objCamMan;
	jWhichCamera = objWhichCamera;




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

	if (objCamMan != 0) {
		clsCamMan = (*env)->FindClass(env, "android/hardware/camera2/CameraManager");
		midSetTorchMode = (*env)->GetMethodID(env, clsCamMan, "setTorchMode",
				"(Ljava/lang/String;Z)V");


	}


	clsStrobeActivity = (*env)->FindClass(env, "com/tp77/StrobeLib/StrobeService");
	midBurstDone = (*env)->GetMethodID(env, clsStrobeActivity, "burstDone", "(Z)V");
	midSendScreenState = (*env)->GetMethodID(env, clsStrobeActivity, "sendScreenState", "(Z)V");

	int nOffDoubler = 0;
	int bWhichOff = 0;
	int nOffCount = 0;

	jlong lTemp = 0;
	jlong lOnDuration = 0;

	jlong lAfterFirst = 0;

	jlong lAssumedDuration = 0;

	clearDiagnostics();

	while (!bKilled)
	{

		bScreen = bNextScreen;
		bLed = bNextLed;

		nFlashSync = nNextFlashSync;
		nNextFlashSync = 0;

		if (bAbort) {
			bUsingArray = bNextUsingArray;
			nNumFlashes = nNextNumFlashes;
			bLoop = bNextLoop;
			bFlashing = bNextFlashing;
			bTorch = bNextTorch;

			if (bFlashing)
			{
				clearDiagnostics();
				addDiagnostic(lNextFlash, DI_FRAME_START);
			}

			totalFlashes = 0;
			totalLags = 0;
			totalSkips = 0;
			lOnTime = 0;
			lOnCommandTime = 0;
			lCountStart = elapsed(0);

			nLag = 0;
			nLag2 = 0;
			nFlashIndex = 0;
			bAbort = 0;
		}

		if (bFlashing)
		{
			if (elapsed(0) < lNextFlash + nLateThreshold)
			{
				totalFlashes++;
				computeThisOnOff();
				advanceIndex();


				lAssumedDuration = 15000;
				if (totalFlashes -1 > 3)
				{
					lAssumedDuration = (long)(lOnCommandTime * 1.5 / (totalFlashes -1));
				}

				if (nOnLen >= 0) {

					lTemp = elapsed(0);

					addDiagnostic(elapsed(0), DI_ON);
					lAfterFirst = -1;



					if (nOnLen + nOffLen > 50000 && !bGentle) {
						if (nOffLen > lAssumedDuration*3)
						{
							if ( changeFlash(env, this, C_ON, -10000, 10000) ) return;
							lAfterFirst = elapsed(0);
						}
						if (nOnLen > lAssumedDuration*2)
						{
							if ( changeFlash(env, this, C_ON, -10000, 10000) ) return;
							if (lAfterFirst == -1)
								lAfterFirst = elapsed(0);
						}
						if (nOnLen > lAssumedDuration)
						{
							if ( changeFlash(env, this, C_ON, -10000, 10000) ) return;
							if (lAfterFirst == -1)
								lAfterFirst = elapsed(0);
						}
					}

					debug++;
//					lMultiThreadGo = elapsed(0) + 2000;
					if ( changeFlash(env, this, C_ON, -10000, 10000) ) return;
					if (lAfterFirst == -1)
						lAfterFirst = elapsed(0);

					addDiagnostic(lAfterFirst, DI_ONS);

					addDiagnostic(elapsed(0), DI_ON_TO_OFF);

					lOnDuration = lAfterFirst - lTemp;
					lOnCommandTime += lOnDuration;

//					addDiagnostic(elapsed(0), DI_OFF);

					lNextFlash += nOnLen;
					if (nOnLen)
						delay(lNextFlash);

				} else {
					addDiagnostic(elapsed(0), DI_ON);
					addDiagnostic(elapsed(0), DI_ONS);
					addDiagnostic(elapsed(0), DI_ON_TO_OFF);
				}

				if (!bTorch)
				{
					addDiagnostic(elapsed(0), DI_OFF);
					if ( changeFlash(env, this, C_OFF, -10000, 10000) ) return;

					addDiagnostic(elapsed(0), DI_OFFS);

					lOnTime += elapsed(0) - lAfterFirst;


					nOffDoubler = lAssumedDuration;
					bWhichOff = 0;

					while (nOffLen - elapsed(lNextFlash) > nOffDoubler && !bGentle)
					{
						if (nOffLen - elapsed(lNextFlash) > nOffDoubler + 5000)
						{
							nOffCount = nOffDoubler;
							while (nOffCount > 500000 && !bAbort && !bKilled)
							{
								msleep2(nOffCount);
								nOffCount -= 500000;
							}
							if (!bAbort && !bKilled && nOffCount > 0)
								msleep2(nOffCount);

							if ( changeFlash(env, this, bWhichOff ? C_OFF : C_OFF2, -10000, 10000) ) return;
						}
						nOffDoubler *= 2;
						bWhichOff ^= 1;
					}


					lNextFlash += nOffLen;


				} else
				{
					addDiagnostic(elapsed(0), DI_OFF);
					addDiagnostic(elapsed(0), DI_OFFS);
				}


			} else {
				lNextFlash += nOnLen + nOffLen;
				totalSkips++;
			}



			computeThisOnOff();

			if (nFlashSync != 0) {
				jlong nSync = nFlashSync;
				nFlashSync = 0;

				// this can only happen in standard stobing mode

				test = 0;

				while (lNextFlash > nSync + nOffLen + nOnLen)
				{
					nSync += nOffLen + nOnLen;
					test++;
				}

				if ((nSync - lNextFlash) < (nOffLen + nOnLen)/4)
				{
					nSync += nOffLen + nOnLen;
				}


				lNextFlash = nSync;
			}





			while (!bKilled && bFlashing && elapsed(0) > lNextFlash + nOffLen + nOnLen)
			{
				incrementDiagnostic();
				addDiagnostic(lNextFlash, DI_FRAME_START);

				lNextFlash += nOffLen + nOnLen;
				nLag++;
				nLag2++;
				totalLags++;
				advanceIndex();
				computeThisOnOff();
			}


			addDiagnostic(elapsed(0), DI_OFF_TO_ON);
			delay(lNextFlash);

			incrementDiagnostic();
			addDiagnostic(lNextFlash, DI_FRAME_START);

		}


		if (!bKilled && !bFlashing)
		{
			if (bTorch) {
				if ( changeFlash(env, this, C_ON, -10000, 3) ) return;
			} else {
				if ( changeFlash(env, this, C_OFF, -3, 10000) ) return;
            }

			if (!bAbort)
				msleep2(100000);

			lNextFlash = elapsed(0);
		}

		if (bBurstDone)
		{
			(*env)->CallVoidMethod(env, this, midBurstDone, 0);
			bBurstDone = 0;
		}


	}




}

void Java_com_tp77_StrobeLib_StrobeService_otherThread(JNIEnv * env, jobject this, jobject objCamera, jobject offF)
{

	jclass zclsString = (*env)->FindClass(env, "java/lang/String");
	jclass zclsCamera = (*env)->FindClass(env, "android/hardware/Camera");
	jmethodID zmidSetParameters = (*env)->GetMethodID(env, zclsCamera, "setParameters",
				"(Landroid/hardware/Camera$Parameters;)V");


	jmethodID zmidNativeSetParameters = (*env)->GetMethodID(env, zclsCamera, "native_setParameters",
				"(Ljava/lang/String;)V");


	delay(elapsed(0) + 1000000);

//	debug = (int)(0xFFFFFF&(jlong)(jOffFlatten));

	delay (elapsed(0) + 1000000);
	while (!bKilled)
	{
		if (lMultiThreadGo != 0)
		{
			delay(lMultiThreadGo);
			addDiagnostic(elapsed(0), DI_ONS);


//			if (jCamera == NULL || zmidNativeSetParameters == NULL || jOffFlatten == NULL)
//				return;

			(*env)->CallVoidMethod(env, objCamera, zmidNativeSetParameters, offF);


			lMultiThreadGo = 0;
			addDiagnostic(elapsed(0), DI_OFF_TO_ON);
		}

		delay(elapsed(0) + 1002);


	}



}







void Java_com_tp77_StrobeLib_StrobeService_update(JNIEnv * env, jobject this, jintArray jFlashes,
		jboolean jLoop, jboolean jFlashing, jboolean jTorch)
{
	int iii;
	jint len = (*env)->GetArrayLength(env, jFlashes);
	jint* jintFlashes = (*env)->GetIntArrayElements(env, jFlashes, 0);
	if (bUsingArray)
	{
		for (iii = 0; iii < len; iii++) {
			anFlashes0[iii] = jintFlashes[iii];
		}
		bNextUsingArray = 0;
	} else
	{
		for (iii = 0; iii < len; iii++) {
			anFlashes1[iii] = jintFlashes[iii];
		}
		bNextUsingArray = 1;
	}
	(*env)->ReleaseIntArrayElements(env, jFlashes, jintFlashes, 0);
	nNextNumFlashes = len/2;
	bNextLoop = jLoop;
	bNextFlashing = jFlashing;
	bNextTorch = jTorch;
	bAbort = 1;

}



// this is for things that don't require an abort

void Java_com_tp77_StrobeLib_StrobeService_setSettings(JNIEnv * env, jobject this, jboolean jLed, jboolean jScreen,
		jint jLateThreshold, jint jFlashPreempt, jboolean jGentle)
{
	bNextLed = jLed;
	bNextScreen = jScreen;
	if (jLateThreshold == -1)
		jLateThreshold = 10000000;
	nLateThreshold = jLateThreshold;
	nFlashPreempt = jFlashPreempt;
	bGentle = jGentle;


}

jlong Java_com_tp77_StrobeLib_StrobeService_getTime(JNIEnv * env, jobject this) {
	return elapsed(0);
}

void Java_com_tp77_StrobeLib_StrobeService_timeSync(JNIEnv * env, jobject this, jlong jFlashSync) {
	nNextFlashSync = jFlashSync;
}

void Java_com_tp77_StrobeLib_StrobeService_kill(JNIEnv * env, jobject this)
{
	bKilled = 1;
}


jint Java_com_tp77_StrobeLib_StrobeService_debugNumber(JNIEnv * env, jobject this)
{
	return debug;
}




jint Java_com_tp77_StrobeLib_StrobeService_checkLag(JNIEnv * env, jobject this)
{
	jint toReturn = nLag;
	nLag = 0;
	if (toReturn == 0)
		return -1;
	if (totalFlashes < 3)
		return 696969;
	return (int)((float)totalFlashes * 1000000 / elapsed(lCountStart));
}



jint Java_com_tp77_StrobeLib_StrobeService_checkLag2(JNIEnv * env, jobject this)
{
	jint toReturn = nLag2;
	nLag2 = 0;
	return toReturn;
}

void incrementDiagnostic()
{
	nDiagPos++;
	if (nDiagPos == DI_LENGTH)
		nDiagPos = 0;
	int iii;
	for (iii = 0; iii < DI_CATEGORIES; iii++)
	{
		anDiagnostics[nDiagPos*DI_CATEGORIES + iii] = -1;
	}
}


void addDiagnostic(jlong value, int type)
{
	anDiagnostics[type + nDiagPos*DI_CATEGORIES] = value;
}

void clearDiagnostics()
{
	int iii;
	for (iii = 0; iii < DI_SIZE; iii++)
	{
		anDiagnostics[iii] = -1;

	}
	nDiagPos = 0;

}

jlongArray Java_com_tp77_StrobeLib_StrobeService_getDiagnostics(JNIEnv * env, jobject this)
{

	jlongArray toRet;


	toRet = (*env)->NewLongArray(env, DI_SIZE+1);
	if (toRet == NULL)
		return NULL;

	anDiagnostics[DI_SIZE] = nDiagPos;

	(*env)->SetLongArrayRegion(env, toRet, 0, DI_SIZE+1, anDiagnostics);

	return toRet;


}


jintArray Java_com_tp77_StrobeLib_StrobeService_getDutyData(JNIEnv * env, jobject this)
{
	jintArray toRet;

    int useTotalFlashes = totalFlashes;
    if ( useTotalFlashes <= 0 ) useTotalFlashes = 1;
	int correction = totalSkips - totalLags;
	int useFlashes = useTotalFlashes;

	if (correction > 0)
		useFlashes += correction;

	int period = elapsed(lCountStart) / useFlashes;


	toRet = (*env)->NewIntArray(env, 2);
	if (toRet == NULL)
		return NULL;

	jint beforeRet[2];
	beforeRet[0] = lOnTime / useTotalFlashes;
	beforeRet[1] = period - beforeRet[0];

	if (useFlashes < 3) {
		beforeRet[0] = -1;
		beforeRet[1] = -1;
	}


	(*env)->SetIntArrayRegion(env, toRet, 0, 2, beforeRet);


	return toRet;
}



