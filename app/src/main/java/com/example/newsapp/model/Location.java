package com.example.newsapp.model;

import io.realm.RealmObject;

public class Location extends RealmObject {
    public Integer count;
    public Double lat;
    public Double lng;
    public String url;
    public String name;

    public Location() {

    }

    public Location(Integer count, Double lat, Double lng, String url, String name) {
        this.count = count;
        this.lat = lat;
        this.lng = lng;
        this.url = url;
        this.name = name;
    }
}
