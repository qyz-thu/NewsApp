package com.example.newsapp;

import android.app.Application;
import android.util.Log;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;

public class NewsApp extends Application {
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
            Realm.deleteRealm(config);
        }

        Log.i("main", "Realm init done");
    }
}
