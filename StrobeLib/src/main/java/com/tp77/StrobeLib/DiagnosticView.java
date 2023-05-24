package com.tp77.StrobeLib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class DiagnosticView extends View {

	public final static int LEFT_RIGHT_MARGIN = 8; // dp
	
	public final static int DI_FRAME_START = 0;
	public final static int DI_ON = 1;
	public final static int DI_ONS = 2;
	public final static int DI_ON_TO_OFF = 3;
	public final static int DI_OFF = 4;
	public final static int DI_OFFS = 5;
	public final static int DI_OFF_TO_ON = 6;

	public final static int DI_CATEGORIES = 7;
	public final static int DI_LENGTH = 100;
	
	public final static int DI_SIZE = DI_CATEGORIES*DI_LENGTH;
	
	public final static int DI_LED_ON = 7;
	public final static int DI_LED_OFF = 8;
	
	private long[] mData;
	private float dp = 0;
	
	private Paint[] mPaints;
	
	private boolean mShowLedState = false;
	
	
	public DiagnosticView(Context context) {
		super(context);
		init();
	}
	
	public DiagnosticView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private static final int[] COLORS = new int[] {
		0, 0, 0,
		255, 255, 0,
		249, 168, 37,
		0, 96, 100,
		124, 77, 255,
		69, 39, 160,
		0, 96, 100,
		255, 255, 255,
		30, 30, 30
		
	};
	
	private void init() {
		dp = getResources().getDisplayMetrics().density;
		
		mPaints = new Paint[COLORS.length/3];
		for (int iii = 0; iii < mPaints.length; iii++) {
			Paint p = new Paint();
			p.setAntiAlias(true);
			p.setARGB(255, COLORS[iii*3+0], COLORS[iii*3+1], COLORS[iii*3+2]);
			p.setStyle(Style.FILL);
			mPaints[iii] = p;
		}
		
		mTick.setAntiAlias(true);
		mTick.setStrokeWidth(2*dp);
		mTick.setARGB(255, 255, 255, 255);
		
		mTickText.setAntiAlias(true);
		mTickText.setARGB(255, 255, 255, 255);
		mTickText.setTextSize(10*dp);
		mTickText.setTextAlign(Align.CENTER);
		
		
	}
	
	public void setData(long[] data, boolean showLedState)
	{
		mData = data;
		invalidate();
		mShowLedState = showLedState;
	}
	
	
	Paint mTick = new Paint();
	Paint mTickText = new TextPaint();
	

	
	public void onDraw(Canvas c) {
		
		long[] stamps = new long[DI_LENGTH];
		
		c.drawARGB(255, 30, 30, 30);
		if (mData == null)
			return;
		
		float height = c.getHeight();
		float width = c.getWidth();
		
	
		long longest = 0;
		int number = 0;
		long lastStamp = -1;
		long stamp = 0;
		long period = 0;
		
		int pos = (int)mData[DI_SIZE];
	
		while (number < DI_LENGTH-1) {
			pos--;
			if (pos < 0)
				pos = DI_LENGTH-1;
			if (mData[pos*DI_CATEGORIES] == -1)
				break;
			
			stamp = mData[pos*DI_CATEGORIES];
			stamps[number] = stamp;
			
			if (lastStamp != -1)
			{
				period = lastStamp - stamp;
				if (period > longest)
					longest = period;
				
			}
			
			lastStamp = stamp;
			
			number++;
		}
		
		long[] tempStamps = new long[number];
		for (int iii = 0; iii < number; iii++) {
			tempStamps[iii] = stamps[number - iii - 1];
		}
		
		stamps = tempStamps;
		
		int step = getGraphStep(longest);
		
		long graphMax = longest;
		
		int useNumber = number - 1;
		if (useNumber < 10)
			useNumber = 10;
		
		
		
		float textAreaHeight = 20*dp;
		float marginWidth = LEFT_RIGHT_MARGIN * dp;
		float plotHeight = height - textAreaHeight;
		float plotWidth = width - marginWidth*2;
		
		float laneHeight = plotHeight / useNumber;
		
		float unitToPx = plotWidth/graphMax;
		
		int startLane = useNumber - number + 1;
		
		
		pos++;
		long lastTime = 0;
		int lastType = -1;
		long nowTime = 0;
		
		int start = DI_ONS;
		int adder = DI_OFFS - DI_ONS;
		
		if (!mShowLedState) {
			start = 1;
			adder = 1;
		}
		
		
		for (int iii = 0; iii < number; iii++) {
			
			if (pos == DI_LENGTH)
				pos = 0;
			
			for (int jjj = start; jjj < DI_CATEGORIES; jjj += adder) {
				nowTime = mData[pos*DI_CATEGORIES + jjj];
				if (nowTime == -1)
					continue;
				if (lastType != -1)
					addSegment(c, stamps, number, lastType, lastTime, nowTime, startLane, unitToPx, 
						laneHeight, marginWidth);
				lastType = jjj;
				lastTime = nowTime;
				
				if (mShowLedState) {
					if (jjj == DI_ONS)
						lastType = DI_LED_ON;
					if (jjj == DI_OFFS)
						lastType = DI_LED_OFF;
				}
			}
			
			
			pos++;
			
		}
		
		
		int numberSteps = (int) ((float)graphMax/step + 1.7f);
		float tickCenter = laneHeight*useNumber;
		float textTop = tickCenter + 14*dp;
		float tickLength = 6*dp;
//		float tickStep = plotWidth/(numberSteps-1);
		
		for (int iii = 0; iii < numberSteps; iii++) {
			float fiii = iii;
			if (iii == numberSteps -1)
				fiii = (float)graphMax/step;
			float x = marginWidth + fiii*step/graphMax*plotWidth;
			c.drawLine(x, tickCenter-tickLength/2, x, tickCenter+tickLength/2, mTick);
			c.drawText(Integer.toString((int) (fiii*step/1000 + 0.5f)), x, textTop, mTickText);
		}
		
		
		
		
	}
	
	private void addSegment(Canvas c, long[] stamps, int number, int type, long start, long end, int startLane, float unitToPx, 
			float laneHeight, float leftMargin) {
		
		int pos = 0;
		while (pos < number && start >= stamps[pos])
			pos++;
		
		if (pos == number)
			return;
		if (pos == 0)
			return;
		
		int startPos = pos-1;
		pos--;
		
		while (pos < number && end > stamps[pos])
			pos++;
		
		if (pos == number)
			return;
		if (pos == 0)
			return;
		
		int endPos = pos-1;
		
		for (int iii = startPos; iii <= endPos; iii++) {
			long startT = stamps[iii];
			long length = stamps[iii+1] - startT;
			
			
			float y = iii*laneHeight;
			float left = leftMargin;
			float right = leftMargin + length*unitToPx;
			
			if (iii == startPos)
				left = leftMargin + (start - startT)*unitToPx;
			if (iii == endPos)
				right = leftMargin + (end - startT)*unitToPx;
			

			c.drawRect(left, y, right, y+laneHeight, mPaints[type]);
			
		}
		
		
	}
	
	private final static int[] STEPS = {1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000};
	
	private int getGraphStep(long longest) {
		int pos = 0;
		while (longest/STEPS[pos] > 6 && pos < STEPS.length-1)
			pos++;
		
		return STEPS[pos];
		
	}
	
	
	
}
