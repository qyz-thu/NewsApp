package com.example.newsapp.model;

import io.realm.RealmObject;

public class PairDoubleString extends RealmObject {
    Double score;
    String name;

    public PairDoubleString() {

    }

    public PairDoubleString(Double score, String name) {
        this.score = score;
        this.name = name;
    }
}
