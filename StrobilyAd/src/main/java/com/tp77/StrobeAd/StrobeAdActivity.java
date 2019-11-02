package com.tp77.StrobeAd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

import com.tp77.StrobeLib.StrobeLibActivity;

public class StrobeAdActivity extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = new Intent(this, com.tp77.StrobeLib.MainActivity.class);
        startActivity(intent);
        finish();
    }
}