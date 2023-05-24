package com.tp77.StrobeLib;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;



public class PatternStorage {
	

	interface PatternSaved {
		public void patternSaved();
	};
	
	public static final String KEY_ID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_PATTERN = "pattern";
	
	
	DatabaseHelper mDbHelper;
	public SQLiteDatabase mDb;
	
	private static final String dbLock = "dbLock1";
	
	private static final String DATABASE_NAME = "patterns.db";
	private static final String TABLE = "patterns";
	private static final int DATABASE_VERSION = 1;
	
	Context mContext;
	
	private static PatternStorage instance = null;

	private PatternStorage(Context context) {
		mContext = context;
		open();
	}
	

	public static PatternStorage getInstance(Context context) {
		synchronized (dbLock) {
			if (instance != null)
				return instance;
			
			instance = new PatternStorage(context);
			return instance;
		}
	}
	
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		private Context mContext;
		
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d("ScheduleStorage", "onCreate");
			synchronized (dbLock) {
				makeTableLow(TABLE, db);
			}
		}
		
		// it is assumed that this routine just deletes it
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("ScheduleStorage", "onUpgrade");
			synchronized (dbLock) {
				db.execSQL("DROP TABLE IF EXISTS '" + TABLE + "'");
				onCreate(db);
			}
			
		}
		
		
	}
	
	private static void makeTableLow(String name, SQLiteDatabase db) {
		db.execSQL("create table '" + name + "' (_id integer primary key autoincrement, " +
				"name string not null, pattern string not null)");
	}
	
	

	
	public void open() {
		synchronized (dbLock) {
			if (mDbHelper == null)
				mDbHelper = new DatabaseHelper(mContext);
			
			mDb = mDbHelper.getWritableDatabase();
		}
		
		
	}
	
	
	
	
	public PatPart[] getPattern(String name) {
		synchronized (dbLock) {
    		Cursor cur = mDb.query(TABLE, new String[] {KEY_PATTERN}, KEY_NAME + "=?", new String[] {name}, null, null, null);
    		
    		if (cur.getCount() == 0) {
    			cur.close();
    			return null;
    		}
			
			cur.moveToFirst();
			PatPart[] toRet = stringToPattern(cur.getString(0));
			cur.close();
			
			return toRet;
		}
	}
	
	
	public String[] getPatterns() {
		synchronized (dbLock) {
    		Cursor cur = mDb.query(TABLE, new String[] {KEY_NAME}, null, null, null, null, KEY_NAME);
    		String[] toRet = new String[cur.getCount()];
    		if (toRet.length == 0) {
    			cur.close();
    			return toRet;
    		}
    		
    		cur.moveToFirst();
    		for (int iii = 0; iii < toRet.length; iii++) {
    			toRet[iii] = cur.getString(0);
    			cur.moveToNext();
    		}
    		cur.close();
    		
    		return toRet;
    	}
		
	}
	
	public void deletePattern(String name) {
		synchronized (dbLock) {
			mDb.delete(TABLE, KEY_NAME + "=?", new String[] {name});
		}
	}
	
	public void addPattern(String name, PatPart[] pattern) {
		synchronized (dbLock) {
			if (getPattern(name) != null) {
				deletePattern(name);
			}
			
			ContentValues cv = new ContentValues();
			
			cv.put(KEY_NAME, name);
			String str = patternToString(pattern);
			
			if (str != null) {
				cv.put(KEY_PATTERN, str);
				
				mDb.insert(TABLE, null, cv);
			}
		}
	}
	

	public void addPattern(String name, String pattern) {
		synchronized (dbLock) {
			if (getPattern(name) != null) {
				deletePattern(name);
			}
			
			ContentValues cv = new ContentValues();
			
			cv.put(KEY_NAME, name);
			String str = pattern;
			
			if (str != null) {
				cv.put(KEY_PATTERN, str);
				
				mDb.insert(TABLE, null, cv);
			}
		}
	}
	
	
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static String patternToString(PatPart[] parts) {
		
		if (Build.VERSION.SDK_INT >= 8) {
			ByteArrayOutputStream baos = null;
			ObjectOutputStream oos = null;
			
			
			try {
				baos = new ByteArrayOutputStream();
				oos = new ObjectOutputStream(new BufferedOutputStream(baos));
			
				oos.writeInt(parts.length);
				oos.writeInt(PatPart.VERSION);
				
				for (PatPart part : parts)
					part.writeExternal(oos);
				oos.flush();
			
				String str = Base64.encodeToString(baos.toByteArray(), 0);
				
				return str;
			
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					if(oos != null)
						oos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
		
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static PatPart[] stringToPattern(String str) {

		if (Build.VERSION.SDK_INT >= 8) {
			
			PatPart[] toRet = null;
			
			
			if (str.length() >= 0) {
			
				ObjectInputStream ois = null;
				Externalizable res = null;
				try {
					ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decode(str, 0)));

					toRet = new PatPart[ois.readInt()];
					
					int version = ois.readInt();
					
					
					for (int iii = 0; iii < toRet.length; iii++) {
						toRet[iii] = new PatPart(ois, version);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				} finally {
					try {
						if(ois != null)
							ois.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
		 
				}
				
				return toRet;
		
				
			}
			
			return null;
			
			
		}
		
		
		return null;
		
		
		
	}
	
	
}
	

