package com.example.newsapp.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class News extends RealmObject implements Comparable<News>, Serializable {
    @PrimaryKey
    public String newsID;

    public String lang;
    public String video;
    public String title;
    public String content;
    public String publisher;
    public String category;
    public String url;

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

    public boolean isRead;
    public boolean isStarred;
    public Date firstReadTime;

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
        this.video = json.optString("video");
        this.url = json.optString("url");
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
            String images = json.getString("image").trim();
            images = images.replaceAll("[\\[\\]]", "");
            String[] urls = images.split(",");
            for (String url : urls) {
                String trimmed = url.trim();
                if (trimmed.length() > 0) {
                    this.images.add(trimmed);
                }
            }
        } catch (JSONException e) {
            // ignore
        }

        this.keywords = new RealmList<>();
        try {
            JSONArray keywords = json.getJSONArray("keywords");
            for (int i = 0; i < keywords.length(); i++) {
                JSONObject object = keywords.getJSONObject(i);
                Double score = object.optDouble("score");
                String name = object.optString("word");
                this.keywords.add(new PairDoubleString(score, name));
            }
        } catch (JSONException e) {
            // ignore
        }

        this.locations = new RealmList<>();
        try {
            JSONArray keywords = json.getJSONArray("locations");
            for (int i = 0; i < keywords.length(); i++) {
                JSONObject object = keywords.getJSONObject(i);
                this.locations.add(new Location(object.optInt("count"), object.optDouble("lat"), object.optDouble("lng"), object.optString("linkedURL"), object.optString("mention")));
            }
        } catch (JSONException e) {
            // ignore
        }

        News prev = Realm.getDefaultInstance().where(News.class).equalTo("newsID", this.newsID).findFirst();
        this.isRead = prev != null && prev.isRead;
        this.isStarred = prev != null && prev.isStarred;
        if (prev != null) {
            this.firstReadTime = prev.firstReadTime;
        }

        // TODO: the rest
    }

    @Override
    public int compareTo(News o) {
        return publishTime.compareTo(o.publishTime);
    }


}





