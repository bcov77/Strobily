package com.tp77.StrobeLib;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MyNumberPicker extends FrameLayout {
	
	
	private static final String B_SUPER = "bSuper";
	private static final String B_MAX = "bMax";
	private static final String B_MIN = "bMin";
	private static final String B_STEP = "bStep";
	private static final String B_SUFFIX = "bSuffix";
	private static final String B_VALUE = "bValue";
	
	private int mMax;
	private int mMin;
	private int mStep;
	private String mSuffix;
	
	private Context mContext;
	
	private int mValue;
	
	private TextView tv;
	
	private OnNumberChangedListener mListener = null;

	
	interface OnNumberChangedListener {
		public void onNumberChanged(int newVal);
	};
	
	public MyNumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		numberPickerInit(context);
	}
	
	public MyNumberPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		numberPickerInit(context);
	}
	
	public MyNumberPicker(Context context) {
		super(context);
		numberPickerInit(context);
		
	}
	
	@Override
	public Parcelable onSaveInstanceState() {
		
		Bundle bundle = new Bundle();
		bundle.putParcelable(B_SUPER, super.onSaveInstanceState());
		bundle.putInt(B_MAX, mMax);
		bundle.putInt(B_MIN, mMin);
		bundle.putInt(B_STEP, mStep);
		bundle.putInt(B_VALUE, mValue);
		bundle.putString(B_SUFFIX, mSuffix);
		
		
		return bundle;
	}
	
	@Override
	public void onRestoreInstanceState(Parcelable state) {
		
		if (state instanceof Bundle) {
			
			Bundle bundle = (Bundle) state;
			
			super.onRestoreInstanceState(bundle.getParcelable(B_SUPER));
			mMax = bundle.getInt(B_MAX);
			mMin = bundle.getInt(B_MIN);
			mStep = bundle.getInt(B_STEP);
			setValue(bundle.getInt(B_VALUE));
			mSuffix = bundle.getString(B_SUFFIX);
			
			return;
			
		}
		
		super.onRestoreInstanceState(state);
		
	}
	
	
	private void numberPickerInit(Context context) {
		mContext = context;
		
		RelativeLayout rl = (RelativeLayout)((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.wid_number_picker, null);
		addView(rl);
		
		rl.findViewById(R.id.plus).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				int toSet = mValue + mStep;
				if (toSet > mMax)
					toSet = mMax;
				setValue(toSet);
				if (mListener != null) 
					mListener.onNumberChanged(toSet);
			}
		});
		
		rl.findViewById(R.id.minus).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				int toSet = mValue - mStep;
				if (toSet < mMin)
					toSet = mMin;
				setValue(toSet);
				if (mListener != null) 
					mListener.onNumberChanged(toSet);
			}
		});
		
		tv = (TextView)rl.findViewById(R.id.number);
		
	}
	
	public void setMinMaxStepSuff(int min, int max, int step, String suffix) {
		mMin = min;
		mMax = max;
		mStep = step;
		mSuffix = suffix;
	}
	
	public void setValue(int value) {
		mValue = value;
		tv.setText(Integer.toString(mValue) + mSuffix);
	}
	
	public int getValue() {
		return mValue;
	}

	public void setOnNumberChangedListener(OnNumberChangedListener listener) {
		mListener = listener;
	}
	
}
