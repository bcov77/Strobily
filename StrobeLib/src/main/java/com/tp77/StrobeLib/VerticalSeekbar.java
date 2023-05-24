package com.tp77.StrobeLib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;

public class VerticalSeekbar extends View {

	public interface VerticalSeekbarListener {
		public void onSeekbarChange(float progress);
	};
	
	private static final int TOP = 20;
	private static final int BOTTOM = 20;
	private static final int RIGHT = 30;
	private static final int CIRCLE_RADIUS = 100;
	private static final int BLACK_RADIUS = 160;
	private static final int SLIDER_HEIGHT = 20;
	
	private static final int TOUCH_RADIUS = 20;
	
	private int mTop = 0;
	private int mBottom = 0;
	private int mRight = 0;
	private int mTouchRadius = 0;
	private int mCircleRadius = 0;
	private int mSliderHeight = 0;
	private int mBlackRadius = 0;
	
	private Context mContext = null;
	
	private int mMode = 0;
	
	
	private static final int M_NOT_TOUCHING = 0;
	private static final int M_TOUCHING = 1;
	private static final int M_SEEKING = 2;
	private static final int M_WHEELING = 4;
	private static final int M_TWO_FINGER = 5;

	private int mHeight = 0;
	private int mWidth = 0;
	
	private float mTouchStartX = 0;
	private float mTouchStartY = 0;
	
	private float dp;
	private int mSlop;
	private int mProgress = 7;
	private double mAngle = 0;
	
	private int mMax = 30;
	private boolean mUseRpm = false;
	
	private VerticalSeekbarListener mListener = null;
	
	private Bitmap mSliderCircles[] = null;
	private Bitmap mSlider = null;
	
	
	private int[] CIRCLES = new int[] {
		R.drawable.slider_circle1,	
		R.drawable.slider_circle2,
		R.drawable.slider_circle3,
		R.drawable.slider_circle4,
		R.drawable.slider_circle5
	};
	
	
	public VerticalSeekbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}

	public VerticalSeekbar(Context context) {
		super(context);
		mContext = context;
		init();
	}

	private void init() {
		dp = getResources().getDisplayMetrics().density;
		mSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
		
		mTop = (int) (TOP*dp + 0.5f);
		mBottom = (int) (BOTTOM*dp + 0.5f);
		mRight = (int) (RIGHT*dp + 0.5f);
		mTouchRadius = (int) (TOUCH_RADIUS*dp + 0.5f);
		mCircleRadius = (int) (CIRCLE_RADIUS*dp + 0.5f);
		mSliderHeight = (int) (SLIDER_HEIGHT*dp + 0.5f);
		mBlackRadius = (int) (BLACK_RADIUS*dp + 0.5f);
		
		mSliderCircles = new Bitmap[5];
		for (int iii = 0; iii < 5; iii++) {
			Drawable d = getResources().getDrawable(CIRCLES[iii]);
			Bitmap bitmap = ((BitmapDrawable)d).getBitmap();
			mSliderCircles[iii] = Bitmap.createScaledBitmap(bitmap, mCircleRadius, mCircleRadius*2, true);
		}

		Drawable d = getResources().getDrawable(R.drawable.slider);
		Bitmap bitmap = ((BitmapDrawable)d).getBitmap();
		mSlider = Bitmap.createScaledBitmap(bitmap, mSliderHeight*3, mSliderHeight, true);
	
		mMax = SettingsFragment.getMaxFrequency(mContext);
		mUseRpm = AdvancedFragment.useRPM(mContext);
		
	}
	
	public void setListener(VerticalSeekbarListener listener) {
		mListener = listener;
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    	int mode = MeasureSpec.getMode(heightMeasureSpec);
    	int size = MeasureSpec.getSize(heightMeasureSpec);
    	
    	
    	if (mode == MeasureSpec.UNSPECIFIED) {
    		heightMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    	}
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		mHeight = getMeasuredHeight();
		mWidth = getMeasuredWidth();
		
	}
	
	
	Paint complete = new Paint();
	Paint notComplete = new Paint();
	Paint circle = new Paint();
	Paint black = new Paint();
	Paint angle = new Paint();
	TextPaint circleText = new TextPaint();
	TextPaint scaleText = new TextPaint();
	
	@Override
	public void onDraw(Canvas c) {
		
		complete.setARGB(255, 0, 0xC0, 0xC0);
		complete.setStrokeWidth((int)(8*dp + 0.5f));
		
		notComplete.setARGB(255, 0xA0, 0xA0, 0xA0);
		notComplete.setStrokeWidth((int)(8*dp + 0.5f));
		
		angle.setARGB(255, 255, 255, 255);
		angle.setStrokeWidth((int)(4*dp + 0.5f));
		angle.setAntiAlias(true);
		
		circle.setAntiAlias(true);
		circle.setFilterBitmap(true);

		circleText.setColor(Color.WHITE);
		circleText.setTextSize(12*dp);
		circleText.setAntiAlias(true);
		circleText.setTextAlign(Paint.Align.RIGHT);

		scaleText.setColor(Color.WHITE);
		scaleText.setTextSize(12*dp);
		scaleText.setAntiAlias(true);
		scaleText.setTextAlign(Paint.Align.RIGHT);
		
		black.setAntiAlias(true);
		black.setColor(Color.BLACK);
		
		int yPos = getSeekY();
		float sliderX = mWidth - mRight;
		

		String str;
		if (mUseRpm) {
			str = Integer.toString(mMax*60) + " rpm";
		} else {
			str = Integer.toString(mMax) + " hz";
		}
		c.drawText(str, sliderX - (int)(8*dp + 0.5f), 
				mTop + (int)(8*dp + 0.5f), scaleText);

		if (mUseRpm) {
			str = "0 rpm";
		} else {
			str = "0 hz";
		}
		c.drawText(str, sliderX - (int)(8*dp + 0.5f), 
				mHeight - mBottom, scaleText);
		
		if (mMode != M_NOT_TOUCHING && mMode != M_TWO_FINGER) {
			c.drawCircle(sliderX, yPos, mBlackRadius, black);
			c.drawBitmap(mSliderCircles[mProgress%5], mWidth-mRight-mCircleRadius, yPos-mCircleRadius, circle);
		}
		
		c.drawLine(sliderX, mTop, sliderX, yPos, notComplete);
		c.drawLine(sliderX, yPos, sliderX, mHeight - mBottom, complete);
		
		if (mMode != M_NOT_TOUCHING && mMode != M_TWO_FINGER) {
			c.drawLine(sliderX, yPos, (float)(sliderX - Math.cos(mAngle)*mCircleRadius), 
					(float)(yPos - Math.sin(mAngle)*mCircleRadius), angle);

			if (mUseRpm) {
				str = Integer.toString(mProgress*60) + " rpm";
			} else {
				str = Integer.toString(mProgress) + " hz";
			}
			c.drawText(str, sliderX - mCircleRadius - (int)(4*dp + 0.5f), 
					yPos + (int)(4*dp + 0.5f), circleText);
		
			c.save();
			c.rotate(45, sliderX, yPos);
			

			if (mUseRpm) {
				str = Integer.toString(mProgress*60 + 30) + " rpm";
			} else {
				str = Integer.toString(mProgress) + ".5 hz";
			}
			c.drawText(str, sliderX - mCircleRadius - (int)(4*dp + 0.5f), 
					yPos + (int)(4*dp + 0.5f), circleText);
			c.restore();
		

			c.save();
			c.rotate(-45, sliderX, yPos);
			
			if (mUseRpm) {
				str = Integer.toString(mProgress*60 - 30) + " rpm";
			} else {
				str = Integer.toString(mProgress-1) + ".5 hz";
			}
			c.drawText(str, sliderX - mCircleRadius - (int)(4*dp + 0.5f), 
					yPos + (int)(4*dp + 0.5f), circleText);
			c.restore();
			
		}
		
		c.save();
		c.rotate((float)(mAngle*180/Math.PI), sliderX, yPos);
		c.drawBitmap(mSlider, (float)(sliderX - 1.75*mSliderHeight), 
				(float)(yPos - 0.5*mSliderHeight), circle);
		c.restore();
		
		
		
		
	}
	
	
	
	@SuppressLint("NewApi")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float curX = event.getX();
		float curY = event.getY();

		boolean okForInitialY = Math.abs(curY - getSeekY()) < mSliderHeight / 2;

		boolean inWheelSpace = curX < mWidth - mRight - mTouchRadius;
		boolean inVerticalSlop = Math.abs(curY - getSeekY()) < mSlop;

		int action = event.getAction();
		if (Build.VERSION.SDK_INT >= 8)
			action = event.getActionMasked();
		
		switch (action) {
		
		
		case MotionEvent.ACTION_POINTER_DOWN:
		
		case MotionEvent.ACTION_DOWN:

			if (mMode != M_NOT_TOUCHING) {
				mMode = M_TWO_FINGER;
				invalidate();
				break;
			}
			
			if (inWheelSpace) {
				return false;
			}
			
//			if (!inVerticalSlop && mMode != M_NOT_TOUCHING) {
//				mMode = M_TWO_FINGER;
//				break;
//			}

			mMode = M_TOUCHING;
			
			((ViewGroup)getParent()).requestDisallowInterceptTouchEvent(true);
			invalidate();
			if ( mMode != M_TOUCHING ) break;
			
		case MotionEvent.ACTION_MOVE:
			
			if (mMode == M_TWO_FINGER)
				break;
			
			int upper = mTop;
			int lower = mHeight - mBottom;
			int range = lower-upper;
			int yPos = getSeekY();
			
			
			
			
			
			if (inWheelSpace)
				mMode = M_WHEELING;
			
			if (mMode == M_WHEELING) {
				if (!inWheelSpace && inVerticalSlop) {
					mMode = M_TOUCHING;
					mAngle = 0;
				}
				else
					mAngle = Math.atan2(yPos - curY, mWidth - mRight - curX);
				
				if (mAngle > Math.PI / 2)
					mAngle = Math.PI / 2;
				if (mAngle < -Math.PI / 2)
					mAngle = -Math.PI / 2;
				
				if (mProgress == 0 && mAngle < 0.001 * Math.PI / 2)
					mAngle = 0.001 * Math.PI / 2;

				if ( mProgress == 1 && getRealProgress() < 0.001f )
					mAngle = (-1 + 0.001) * Math.PI / 2;

				if (mListener != null)
					mListener.onSeekbarChange(getRealProgress());
				invalidate();
			}
			
			
			if (mMode == M_TOUCHING) {
				if (!inVerticalSlop)
					mMode = M_SEEKING;
			}
			
			if (mMode == M_SEEKING) {
				int progress = 0;
				
				if (curY < upper)
					progress = mMax;
				else if (curY > lower)
					progress = 0;
				else
					progress = (int)((float)mMax * (1 -  (float)(curY - upper)/(float)(range)) + 0.5f);

				if (mProgress == 0 && mAngle < 0.001 * Math.PI / 2)
					mAngle = 0.001 * Math.PI / 2;

				if (progress != mProgress) {
					mProgress = progress;
					mAngle = 0;
					if (mProgress == 0 && mAngle < 0.001 * Math.PI / 2)
						mAngle = 0.001 * Math.PI / 2;
					invalidate();
					if (mListener != null)
						mListener.onSeekbarChange(getRealProgress());
				}
				
			}
			
			
			break;
			
		case MotionEvent.ACTION_UP:
			mMode = M_NOT_TOUCHING;
			((ViewGroup)getParent()).requestDisallowInterceptTouchEvent(false);
			invalidate();
			
			break;
			
		case MotionEvent.ACTION_CANCEL:
			mMode = M_NOT_TOUCHING;
			((ViewGroup)getParent()).requestDisallowInterceptTouchEvent(false);
			invalidate();
			
			break;
		
		
		
		
		}
		
		
		return true;
		
		
	}
	
	public void setProgress(float progress) {
		if (progress > mMax)
			progress = mMax;
		mProgress = (int)progress;
		mAngle = (progress - (float)mProgress) * Math.PI / 2;
		invalidate();
	}
	
	public float getRealProgress() {
		return (float) (mProgress + mAngle / (Math.PI / 2));
	}
	
	private int getSeekY() {
		return (int)((mHeight - mTop - mBottom) * (1-(float)mProgress/(float)mMax) + mTop + 0.5f);
	}
	
	
	
}
