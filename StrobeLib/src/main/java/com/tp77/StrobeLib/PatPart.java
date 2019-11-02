package com.tp77.StrobeLib;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

public class PatPart {

	public static final int T_FLASH = 0;
	public static final int T_FLASH_DELAY = 1;
	public static final int T_DELAY = 2;
	public static final int T_PATTERN = 3;
	
	public int mType = 0;
	public float mFrequency = 0;
	public int mFlashes = 0;
	public int mDuty = 0;
	public int mTime = 0;
	public boolean mUseTime = false;
	public String mName = "";
	
	
	public static final int VERSION = 1;
	
	
	public PatPart(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		mType = T_FLASH;
		mFrequency = prefs.getFloat(StrobeFragment.P_FREQUENCY, 10f);
		mFlashes = 1;
		mDuty = prefs.getInt(StrobeFragment.P_DUTY, 50);
		mTime = prefs.getInt(StrobeFragment.P_ON_LENGTH, 50000);
		mUseTime = prefs.getBoolean(StrobeFragment.P_USE_SAVED, false);
		mName = "";
		
		
	}
	
	public PatPart(PatPart p) {
		mType = p.mType;
		mFrequency = p.mFrequency;
		mFlashes = p.mFlashes;
		mDuty = p.mDuty;
		mTime = p.mTime;
		mUseTime = p.mUseTime;
		mName = p.mName;
	}
	
	public PatPart(ObjectInput o, int version) throws IOException {
		mType = o.readInt();
		mFrequency = o.readFloat();
		mFlashes = o.readInt();
		mDuty = o.readInt();
		mTime = o.readInt();
		mUseTime = o.readBoolean();
		mName = o.readUTF();
	}
	
	public void writeExternal(ObjectOutput o) throws IOException {
		o.writeInt(mType);
		o.writeFloat(mFrequency);
		o.writeInt(mFlashes);
		o.writeInt(mDuty);
		o.writeInt(mTime);
		o.writeBoolean(mUseTime);
		o.writeUTF(mName);
	}
	
	public static int fillInCommands(int start, int[] pattern, PatPart[] pat, MainActivity context, int depth) {
		for (int iii = 0; iii < pat.length; iii++) {
			start = pat[iii].fillInCommands(start, pattern, context, depth);
		}
		
		return start;
	}
	
	public int fillInCommands(int start, int[] pattern, MainActivity context, int depth) {

		depth += 1;
		if ( depth > 1000 ) {
			context.toastOnUiThread(context, "1000 levels of recursion! Not going deeper!", Toast.LENGTH_SHORT);
			return start;
		}

		if (mType == T_PATTERN) {
			PatPart[] pat = getPattern(context);
			if (pat != null) {
				return PatPart.fillInCommands(start, pattern, pat, context, depth);
			} else 
				return start;
		}

		// This one won't recurse because it's not a pattern
		int flashes = getCommandSpots(context, 0) / 2;
		
		for (int iii = 0; iii < flashes; iii++) {
			pattern[start++] = calculateTime(true);
			pattern[start++] = calculateTime(false);
		}
		
		return start;
	}
	
	private PatPart[] getPattern(Context context) {
		PatternStorage storage = null;
		try {
			storage = PatternStorage.getInstance(context);
		}  catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(context, "Error opening database! Storage may not be writable.", Toast.LENGTH_SHORT).show();
		}
		if ( storage != null ) {
			return storage.getPattern(mName);
		} else {
			// Is this actually correct? Look to this line if there are weird crashes
			return new PatPart[] {new PatPart(context)};
		}
	}
	
	public static int getCommandSpots(PatPart[] pat, MainActivity context, int depth) {
		int commands = 0;
		for (int iii = 0; iii < pat.length; iii++) {
			commands += pat[iii].getCommandSpots(context, depth);
		}
		return commands;
	}
	
	public int getCommandSpots(MainActivity context, int depth) {
		depth += 1;
		if ( depth > 1000 ) {
			context.toastOnUiThread(context, "1000 levels of recursion! Not going deeper!", Toast.LENGTH_SHORT);
			return 0;
		}

		if (mType == T_PATTERN) {
			PatPart[] pattern = getPattern(context);
			if (pattern == null)
				return 0;
			
			return getCommandSpots(pattern, context, depth);
		}
		
		
		if (mType != T_DELAY) {
			return mFlashes*2;
		}
		return 2;
		
	}
	
	


	public int calculateTime(boolean on) {
		double frameWidth = 1000000.0d/mFrequency;
		
		if (mType == T_PATTERN)
			return 0;
		
		if (mType == T_FLASH_DELAY) {
			if (on)
				return -1;
			else
				return (int)frameWidth;
		}
		
		if (mType == T_DELAY) {
			if (on)
				return -1;
			else
				return mTime;
		}
		
		if (mUseTime) {
			int onTime = mTime;
			if (onTime > frameWidth)
				onTime = (int)frameWidth;
			
			if (on) {
				return onTime;
			} else {
				return (int)(frameWidth - onTime);
			}
		}
		
		if (on) {
			return (int)(frameWidth*mDuty/100);
		} else {
			return (int)(frameWidth*(100-mDuty)/100);
		}
	}
	
	
	private static final String[] TYPES = new String[] {"Flashes", "Skips", "Delay"};
	
	public String getTypeTitle(Context context) {
		if (mType <= 2)
			return TYPES[mType];
		String toRet = mName;
		if (getPattern(context) == null)
			toRet += "<gone>";
		return toRet;
	}
	
	public String getFrequencyText(boolean useRpm) {
		float toUse = mFrequency;
		String unit = " hz";
		if (useRpm) {
			toUse *= 60;
			unit = " rpm";
		}
		
		return String.format("%.03f", toUse) + unit;
		
	}
	
	public Spanned getTimeText() {
		int offTime = calculateTime(false);
		int onTime = calculateTime(true);
		
		String str = "";
		
		if (mType == T_DELAY) {
			str += StrobeFragment.displayTime(offTime, offTime < 100000, true);
		} else {
			str += "<b>";
			str += StrobeFragment.displayTime(onTime, offTime + onTime < 100000, true);
			str += "</b> - ";
			str += StrobeFragment.displayTime(offTime, offTime + onTime < 100000, true);
		}
		
		return Html.fromHtml(str);
	}
	
	
	
}
