package com.example.newsapp.model;

import io.realm.RealmObject;

public class Location extends RealmObject {
    Integer count;
    Double lat;
    Double lng;
    String url;
    String name;

    public Location() {

    }
}
