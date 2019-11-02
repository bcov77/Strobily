package com.tp77.StrobeLib;

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HelpFragment extends MyFragment {
	

	private String[] mQ = null;
	private String[] mA = null;
	
	
	private LayoutInflater mInflater = null;
	
	private SharedPreferences mPrefs;
	private View mRoot = null;
	


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		mInflater = mActivity.getLayoutInflater();
	}
	
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle bundle) {
		
		final View v = inflater.inflate(R.layout.frag_help_fragment, null);
		mRoot = v;
		

		parseQandA();
		
		loadList();
		
		return mRoot;
		
	}
	

	
	private void loadList() {
		
		LinearLayout ll = (LinearLayout)mRoot.findViewById(R.id.help_ll);
		
		for (int iii = 0; iii < mQ.length; iii++) {
			if (mA[iii] == null) {
				TextView title = (TextView)mInflater.inflate(R.layout.list_help_title, ll, false);
				title.setText(mQ[iii]);
				ll.addView(title, ll.getChildCount());
			} else {
				final LinearLayout qa = (LinearLayout)mInflater.inflate(R.layout.list_q_and_a, ll, false);
				((TextView)qa.findViewById(R.id.q)).setText(mQ[iii]);
				final TextView a = (TextView)qa.findViewById(R.id.a);
				a.setText(Html.fromHtml(mA[iii]));
				qa.findViewById(R.id.qa_button).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						ImageView icon = (ImageView)qa.findViewById(R.id.icon);
						if (a.getVisibility() == View.GONE) {
							a.setVisibility(View.VISIBLE);
							icon.setImageResource(R.drawable.ic_down_arrow2);
						} else {
							a.setVisibility(View.GONE);
							icon.setImageResource(R.drawable.ic_action_next_item);
						}
						
					}
				});
				ll.addView(qa, ll.getChildCount());	
			}
		}
		
		
		
	}
	
	
	private void parseQandA() {
		
		ArrayList<String> q = new ArrayList<String>();
		ArrayList<String> a = new ArrayList<String>();
		

		String[] qas = getResources().getStringArray(R.array.q_and_a);
		
		int pos = 0;
		while (pos < qas.length) {
			if (qas[pos].charAt(0) == '-') {
				q.add(qas[pos].substring(1));
				a.add(null);
				pos++;
				continue;
			}
			
			q.add(qas[pos++]);
			String answer = qas[pos++];
			answer = answer.replace("\n", "<br>");
			a.add(answer);
			
		}
		
		mQ = q.toArray(new String[q.size()]);
		mA = a.toArray(new String[a.size()]);
		
		
	}
	
	
	
	
	

	@Override
	void updateSliders(boolean fromMain) {
		
		
	}
	
	
}
