package com.example.newsapp.model;

import io.realm.RealmObject;

public class Mention extends RealmObject {
    Integer count;
    String url;
    String name;

    public Mention() {

    }
}
