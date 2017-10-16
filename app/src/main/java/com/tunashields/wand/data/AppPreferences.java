package com.tunashields.wand.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Irvin on 8/30/17.
 */

public class AppPreferences {
    SharedPreferences mPreferences;
    SharedPreferences.Editor mEditor;

    private static final String PREFERENCES_NAME = "WandPreferences";

    private static final String FIRST_TIME_OPEN = "first_time_open";

    @SuppressLint("CommitPrefEdits")
    public AppPreferences(Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();
    }

    public void setFirstTimeOpen(boolean value) {
        mEditor.putBoolean(FIRST_TIME_OPEN, value);
        mEditor.commit();
    }

    public boolean isFirstTimeOpen() {
        return mPreferences.getBoolean(FIRST_TIME_OPEN, true);
    }
}
