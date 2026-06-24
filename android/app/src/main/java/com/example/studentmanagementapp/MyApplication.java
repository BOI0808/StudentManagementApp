package com.example.studentmanagementapp;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getAppContext() {
        if (instance == null) {
            return null;
        }
        return instance.getApplicationContext();
    }
}
