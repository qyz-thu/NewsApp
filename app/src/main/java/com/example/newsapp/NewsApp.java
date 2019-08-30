package com.example.newsapp;

import android.app.Application;
import android.util.Log;

import io.realm.Realm;

public class NewsApp extends Application {
    public NewsApp() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);

        Log.i("main", "Realm inited");
    }
}
