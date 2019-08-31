package com.example.newsapp.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class News extends RealmObject implements Comparable<News> {
    @PrimaryKey
    public String newsID;

    public String lang;
    public String video;
    public String title;
    public String content;
    public String publisher;
    public String category;

    @Index
    public Date publishTime;
    public Date crawlTime;
    public RealmList<String> images;
    public RealmList<Mention> persons;
    public RealmList<Mention> orgs;
    public RealmList<Location> locations;
    public RealmList<PairDoubleDate> when;
    public RealmList<PairDoubleString> where;
    public RealmList<PairDoubleString> keywords;
    public RealmList<PairDoubleString> who;

    public News() {
    }

    public void assign(JSONObject json) {
        this.lang = json.optString("language");
        this.video = json.optString("video");
        this.title = json.optString("title");
        this.content = json.optString("content");
        this.publisher = json.optString("publisher");
        this.category = json.optString("category");
        this.newsID = json.optString("newsID");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        try {
            this.publishTime = format.parse(json.optString("publishTime"));
        } catch (ParseException e) {
            e.printStackTrace();
            this.publishTime = new Date();
        }
        try {
            this.crawlTime = format.parse(json.optString("crawlTime"));
        } catch (ParseException e) {
            this.crawlTime = new Date();
        }

        this.images = new RealmList<>();
        try {
            String images = json.getString("image");
            images = images.replaceAll("[\\[\\]]", "");
            String[] urls = images.split(",");
            this.images.addAll(Arrays.asList(urls));
        } catch (JSONException e) {
            // ignore
        }

        // TODO: the rest
    }

    @Override
    public int compareTo(News o) {
        return publishTime.compareTo(o.publishTime);
    }


}





