package com.example.newsapp.model;

import io.realm.RealmObject;

public class PairDoubleString extends RealmObject {
    public Double score;
    public String name;

    public PairDoubleString() {

    }

    public PairDoubleString(Double score, String name) {
        this.score = score;
        this.name = name;
    }
}
