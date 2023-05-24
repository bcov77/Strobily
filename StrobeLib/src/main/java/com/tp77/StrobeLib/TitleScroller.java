package com.tp77.StrobeLib;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TitleScroller extends LinearLayout {

	public interface OnTitleClickListener {
		public void onTitleClick(int position);
	};
	
	private Context mContext;
	
	private float mTextWidth;
	
	private float dp;
	
	private int mDividerWidth = 0;
	
	private OnTitleClickListener mListener = null;
	
	private LayoutInflater mLayoutInflater = null;
	
	private int mSelected = -1;
	
	private float mTextSize = 0;
	
	public TitleScroller(Context context) {
		super(context);
		mContext = context;
		init();
	}
	public TitleScroller(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
	}
	
	private void init() {
		this.setOrientation(LinearLayout.HORIZONTAL);
		dp = getResources().getDisplayMetrics().density;
		
		mLayoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mDividerWidth = (int) (1 * dp + 0.5f);
		if (mDividerWidth == 0)
			mDividerWidth = 1;
		
		int width = ((Activity)mContext).getWindowManager().getDefaultDisplay().getWidth();
		
		mTextSize = width * 0.06f;
		
	}

	public void setListener(OnTitleClickListener listener) {
		mListener = listener;
	}

	public void setTitles(String[] titles) {
		this.removeAllViews();

		
		for (int iii = 0; iii < titles.length; iii++) {
				String title = titles[iii];
					
			LinearLayout ll = (LinearLayout)mLayoutInflater.inflate(R.layout.part_title, null);
			TextView tv = (TextView)ll.findViewById(R.id.title);
			tv.setText(title);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
			
			final int position = iii;
			
			tv.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mListener != null)
						mListener.onTitleClick(position);
				}
			});
			
			this.addView(ll);
			
			LinearLayout.LayoutParams lp;
			
			if (iii + 1 < titles.length) {
				View v = new View(mContext);
				lp = new LinearLayout.LayoutParams(mDividerWidth, (int) (30f*dp + 0.5f));
				lp.gravity = Gravity.CENTER_VERTICAL;
				v.setLayoutParams(lp);
				v.setBackgroundColor(0x4000C0C0);
				this.addView(v);
			}
		}
		if (mSelected != -1)
			setSelected(mSelected);
	}
	
	public void setPosition(int position, float percentOffset) {
		this.scrollTo((int) ((position + percentOffset - 1)*(mTextWidth+(int)(dp + 0.5f))) - (int)(dp + 0.5f), 0);
	}
	
	public void setSelected(int position) {
		if (mSelected != -1) {
			LinearLayout ll = (LinearLayout)getChildAt(mSelected*2);
			ll.findViewById(R.id.highlight).setVisibility(View.INVISIBLE);
		} 

		mSelected = position;
		LinearLayout ll = (LinearLayout)getChildAt(mSelected*2);
		ll.findViewById(R.id.highlight).setVisibility(View.VISIBLE);
		
	}
	
	
	@Override
	public void onMeasure(int widthSpec, int heightSpec) {
		
		if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) {
			super.onMeasure(widthSpec, heightSpec);
			return;
		}
		
		int width = MeasureSpec.getSize(widthSpec);

		LinearLayout.LayoutParams lp;
		
		mTextWidth = (width-4*mDividerWidth)/3f;
		for (int iii = 0; iii < this.getChildCount(); iii++) {
			
			if (iii % 2 == 0) {
				LinearLayout ll = (LinearLayout)this.getChildAt(iii);
				lp = (LayoutParams) ll.getLayoutParams();
				lp.width = (int) mTextWidth;
				ll.setLayoutParams(lp);
			}
		}
		
		super.onMeasure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), heightSpec);
	}
	
	
	
	
	
}
