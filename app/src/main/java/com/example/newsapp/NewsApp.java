package com.example.newsapp;

import android.app.Application;
import android.util.Log;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;

public class NewsApp extends Application {
    private static final String TAG = NewsApp.class.getName();

    public NewsApp() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);

        // if migration is needed, simply clear the db
        RealmConfiguration config = Realm.getDefaultConfiguration();
        try {
            Realm.getInstance(config);
        } catch (RealmMigrationNeededException e){
            Log.w(TAG, "Cleaning realm db");
            Realm.deleteRealm(config);
        }

        Log.i(TAG, "Realm init done");

        IConfigurationProvider osmConf = Configuration.getInstance();
        File basePath = new File(getCacheDir().getAbsolutePath(), "osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(osmConf.getOsmdroidBasePath().getAbsolutePath(), "tile");
        osmConf.setOsmdroidTileCache(tileCache);
        osmConf.setUserAgentValue("ElephantNews/1.0");
    }
}
