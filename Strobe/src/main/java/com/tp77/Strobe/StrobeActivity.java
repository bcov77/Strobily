package com.tp77.Strobe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StrobeActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = new Intent(this, com.tp77.StrobeLib.MainActivity.class);
        startActivity(intent);
        finish();
    }
    
}