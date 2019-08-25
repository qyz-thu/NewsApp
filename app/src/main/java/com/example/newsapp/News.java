package com.example.newsapp;

import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class News implements Comparable<News> {
    String lang;
    String video;
    String title;
    String content;
    String newsID;
    String publisher;
    String category;
    Date publishTime;
    Date crawlTime;
    ArrayList<String> images;
    List<Mention> persons;
    List<Mention> orgs;
    List<Location> locations;
    List<Pair<Double, Date>> when;
    List<Pair<Double, String>> where;
    List<Pair<Double, String>> keywords;
    List<Pair<Double, String>> who;

    @Override
    public int compareTo(News o) {
        return publishTime.compareTo(o.publishTime);
    }


    class Mention {
        Integer count;
        String url;
        String name;
    }

    class Location {
        Integer count;
        Double lat;
        Double lng;
        String url;
        String name;
    }


    News(JSONObject json) {
        this.lang = json.optString("language");
        this.video = json.optString("video");
        this.title = json.optString("title");
        this.content = json.optString("content");
        this.newsID = json.optString("newsID");
        this.publisher = json.optString("publisher");
        this.category = json.optString("category");
        Log.d("News", json.toString());
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

        this.images = new ArrayList<>();
        try {
            String images = json.getString("image");
            images = images.replaceAll("[\\[\\]]", "");
            String[] urls = images.split(",");
            this.images = new ArrayList<>(Arrays.asList(urls));
        } catch (JSONException e) {
            // ignore
        }

        // TODO: the rest
    }
}
