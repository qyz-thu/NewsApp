package com.example.newsapp;

import android.Manifest;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.AnyRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    static int currentAccountId;

    public NewsApp() {
    }

    // https://stackoverflow.com/questions/6602417/get-the-uri-of-an-image-stored-in-drawable
    static String getUriForResource(Context context, @AnyRes int resId) {
        Resources res = context.getResources();
        return String.format("%s://%s/%s/%s", ContentResolver.SCHEME_ANDROID_RESOURCE, res.getResourcePackageName(resId), res.getResourceTypeName(resId), res.getResourceEntryName(resId));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);

        // if migration is needed, simply clear the db
        RealmConfiguration config = Realm.getDefaultConfiguration();
        try {
            Realm.getInstance(config);
        } catch (RealmMigrationNeededException e) {
            Log.w(TAG, "Cleaning realm db");
            Realm.deleteRealm(config);
        }

        Log.i(TAG, "Realm init done");

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        if (realm.where(Account.class).equalTo("name", "chenjiajie").findFirst() == null)  {
            realm.copyToRealmOrUpdate(new Account("chenjiajie", "123456", getUriForResource(this, R.drawable.cjj_avatar), false));
        }
        if (realm.where(Account.class).equalTo("name", "qianyingzhuo").findFirst() == null)  {
            realm.copyToRealmOrUpdate(new Account("qianyingzhuo", "123456",getUriForResource(this, R.drawable.qyz_avatar), false));
        }

        currentAccount = realm.where(Account.class).equalTo("active", true).findFirst();
        if (currentAccount == null) {
            currentAccount = realm.where(Account.class).equalTo("name", "qianyingzhuo").findFirst();
            currentAccount.active = true;
        }
        currentAccountId = currentAccount.id;
        Log.d(TAG, "Account id is now " + currentAccountId);
        realm.commitTransaction();

        IConfigurationProvider osmConf = Configuration.getInstance();
        File basePath = new File(getCacheDir().getAbsolutePath(), "osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(osmConf.getOsmdroidBasePath().getAbsolutePath(), "tile");
        osmConf.setOsmdroidTileCache(tileCache);
        osmConf.setUserAgentValue("ElephantNews/1.0");
    }
}
