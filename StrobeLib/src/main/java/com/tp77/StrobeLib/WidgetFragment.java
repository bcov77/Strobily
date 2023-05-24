package com.tp77.StrobeLib;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WidgetFragment extends MyFragment {
	
	private static final int S_START = 0;
	private static final int S_SELECTED = 1;
	
	public static final String W_PATTERN = "wPattern";
	public static final String W_LOOP = "wLoop";
	public static final String W_SCREEN = "wScreen";
	public static final String W_LED = "wLed";
	
	
	
	private SharedPreferences mPrefs;
	private View mRoot = null;
	
	private int mState = S_START;
	
	private String mPattern = "";


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity)activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
	}
	
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle bundle) {
		
		final View v = inflater.inflate(R.layout.frag_widget_fragment, null);
		mRoot = v;
		
		
		mRoot.findViewById(R.id.up).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doState(S_START);	
			}
		});
		
		
		mRoot.findViewById(R.id.done).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mRoot.findViewById(R.id.focus_catch).requestFocus();
				
				boolean oneLine = ((CheckBox)mRoot.findViewById(R.id.one_line)).isChecked();
				String label = ((EditText)mRoot.findViewById(R.id.label_edit_text)).getText().toString();
				
				if (oneLine) {
					label = label.replace(' ', '\u00a0');
				}
				if ( label.length() == 0 ) {
					label += '\u00a0';
				}
				boolean loop = ((CheckBox)mRoot.findViewById(R.id.loop)).isChecked();
				boolean screen = ((CheckBox)mRoot.findViewById(R.id.screen)).isChecked();
				boolean led = ((CheckBox)mRoot.findViewById(R.id.led)).isChecked();
				
//				Bundle bundle = new Bundle();
//				bundle.putString(W_PATTERN, mPattern);
//				bundle.putBoolean(W_LOOP, loop);
//				bundle.putBoolean(W_LED, led);
//				bundle.putBoolean(W_SCREEN, screen);
				
				
				
				Intent intent = new Intent(mActivity.getApplicationContext(), MainActivity.class);
//				intent.putExtra(getString(R.string.S_WIDGET), bundle);
				intent.setAction("asdf");
				intent.putExtra(W_PATTERN, mPattern);
				intent.putExtra(W_LOOP, loop);
				intent.putExtra(W_LED, led);
				intent.putExtra(W_SCREEN, screen);
				

//			    Intent addIntent = new Intent();
//			    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
//			    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
//			    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(
//			    		mActivity.getApplicationContext(), R.drawable.ic_free));
//
//			    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
//			    mActivity.getApplicationContext().sendBroadcast(addIntent);

				ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(mActivity, "com.tp77.StrobeLib");
				builder.setIntent(intent)
						.setIcon(IconCompat.createWithResource(
								mActivity.getApplicationContext(), R.drawable.ic_free))
						.setActivity( new ComponentName(mActivity.getApplicationContext(), MainActivity.class))
						.setShortLabel(label)
						.setLongLabel(label)
						.setDisabledMessage("Disabled?");


				try {
					if (!ShortcutManagerCompat.requestPinShortcut(mActivity, builder.build(), null)) {
						Toast.makeText(mActivity, "Error! Your homescreen might not support shortcuts!", Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mActivity, "Error creating shortcut!", Toast.LENGTH_SHORT).show();
				}

				
				
			}
		});
		
		
		return mRoot;
		
	}
	
	
	private void doState(int state) {
		boolean changed = state != mState;
		mState = state;
		
		
		mRoot.findViewById(R.id.state0).setVisibility(state == S_START ? View.VISIBLE : View.GONE);
		mRoot.findViewById(R.id.state1).setVisibility(state == S_SELECTED ? View.VISIBLE : View.GONE);
		
		
		
		switch (state) {
		case S_START:
			PatternStorage storage = null;
			try {
				storage = PatternStorage.getInstance(mActivity);
			}  catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(mActivity, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
			}
			
			
			ListView lv = (ListView) mRoot.findViewById(R.id.pattern_list);
			
			if (lv.getFooterViewsCount() == 0) {
				View v = new View(mActivity);
				lv.addFooterView(v);
			}
			if (lv.getHeaderViewsCount() == 0) {
				View v = new View(mActivity);
				lv.addHeaderView(v);
			}

			if ( storage != null ) {
				final String[] names = storage.getPatterns();
				lv.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.list_patterns_widget, names) {
					@Override
					public View getView(final int position, View recycleView, ViewGroup parent) {
						LinearLayout ll = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.list_patterns_widget, null);
						((TextView) ll.findViewById(R.id.name)).setText(names[position]);

						return ll;
					}
				});
				lv.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
											int position, long id) {
						mPattern = names[position - 1];
						doState(S_SELECTED);
					}
				});
			}
			lv.setFooterDividersEnabled(true);
			lv.setHeaderDividersEnabled(true);
			
			
			
			break;
		case S_SELECTED:
			((TextView)mRoot.findViewById(R.id.pattern)).setText(mPattern);
			
			if (changed) {
				((EditText)mRoot.findViewById(R.id.label_edit_text)).setText(mPattern);
				((CheckBox)mRoot.findViewById(R.id.one_line)).setChecked(false);
				((CheckBox)mRoot.findViewById(R.id.loop)).setChecked(true);
				((CheckBox)mRoot.findViewById(R.id.led)).setChecked(mActivity.useLed());
				((CheckBox)mRoot.findViewById(R.id.screen)).setChecked(mActivity.useScreen());
				
			}
			
			
			
			
			break;
		}
		
		
		
		
		
		
		
		
		
	}
	

	@Override
	void updateSliders(boolean fromMain) {
		
		doState(mState);
		
	}
	
	
}
