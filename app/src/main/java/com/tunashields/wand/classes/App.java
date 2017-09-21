package com.tunashields.wand.classes;

import android.app.Application;

import com.tunashields.wand.data.Database;

/**
 * Created by Irvin on 9/21/17.
 */

public class App extends Application {
    public static Database mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        mDatabase = new Database(this);
        mDatabase.open();
    }

    @Override
    public void onTerminate() {
        mDatabase.close();
        super.onTerminate();
    }
}
