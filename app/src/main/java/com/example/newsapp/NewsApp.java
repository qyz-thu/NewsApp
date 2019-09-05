package com.example.newsapp;

import android.app.Application;
import android.util.Log;

import com.example.newsapp.model.Account;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;

public class NewsApp extends Application {
    private static final String TAG = NewsApp.class.getName();
    static Account currentAccount;

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

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new Account("chenjiajie", "123456", false));
        realm.copyToRealmOrUpdate(new Account("qianyingzhuo", "123456", true));

        currentAccount = realm.where(Account.class).equalTo("active", true).findFirst();
        if (currentAccount == null) {
            currentAccount = realm.where(Account.class).equalTo("name", "chenjiajie").findFirst();
            currentAccount.active = true;
        }
        realm.commitTransaction();

        IConfigurationProvider osmConf = Configuration.getInstance();
        File basePath = new File(getCacheDir().getAbsolutePath(), "osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(osmConf.getOsmdroidBasePath().getAbsolutePath(), "tile");
        osmConf.setOsmdroidTileCache(tileCache);
        osmConf.setUserAgentValue("ElephantNews/1.0");
    }
}
