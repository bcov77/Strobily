package com.tp77.StrobeLib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ClickGuard extends RelativeLayout {

	private static final int CLICKS = 3;  // on this click it goes through
	private static final long TIMEOUT = 1000; // ms till timeout
	
	
	public ClickGuard(Context context) {
		super(context);
		init(context);
	}
	public ClickGuard(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public interface OnClickOkListener {
		public void onClickOk();
	}
	
	
	private Context mContext = null;
	private OnClickOkListener mListener = null;
	
	private long mLastClick = 0;
	private int mClicks = 0;
	
	
	private void init(Context context) {
		mContext = context;
		
		this.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mClicks++;
				if (mClicks == CLICKS) {
					resetGuard();
					mListener.onClickOk();
					return;
				}
				
				mLastClick = System.currentTimeMillis();
				
				TextView topLeft = (TextView) ClickGuard.this.findViewById(R.id.ad_top_left);
				TextView topRight = (TextView) ClickGuard.this.findViewById(R.id.ad_top_right);
				TextView center = (TextView) ClickGuard.this.findViewById(R.id.ad_center);
				View green = ClickGuard.this.findViewById(R.id.ad_green);
				
				topLeft.setVisibility(View.VISIBLE);
				topRight.setVisibility(View.VISIBLE);
				
				topLeft.setText(Integer.toString(mClicks));
				topRight.setText(Integer.toString(mClicks));
				
				green.setVisibility(View.VISIBLE);
				center.setVisibility(View.VISIBLE);
				
				final int fclicks = mClicks;
				
				ClickGuard.this.post(new Runnable() {
					@Override
					public void run() {
						if (fclicks != mClicks)
							return;
						
						if (mLastClick + TIMEOUT < System.currentTimeMillis()) {
							resetGuard();
						} else {
							ClickGuard.this.post(this);
						}
						
					}
				});
			}
			
		});
	}
	
	
	private void resetGuard() {
		TextView topLeft = (TextView) ClickGuard.this.findViewById(R.id.ad_top_left);
		TextView topRight = (TextView) ClickGuard.this.findViewById(R.id.ad_top_right);
		TextView center = (TextView) ClickGuard.this.findViewById(R.id.ad_center);
		View green = ClickGuard.this.findViewById(R.id.ad_green);

		topLeft.setVisibility(View.GONE);
		topRight.setVisibility(View.GONE);
		green.setVisibility(View.GONE);
		center.setVisibility(View.GONE);
		
		mLastClick = 0;
		mClicks = 0;
		
		
	}
	
	public void setOnClickOkListener(OnClickOkListener listener) {
		mListener = listener;
	}
	
	

}
