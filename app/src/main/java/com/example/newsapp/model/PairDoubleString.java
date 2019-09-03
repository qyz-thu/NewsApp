package com.example.newsapp.model;

import io.realm.RealmObject;

public class PairDoubleString extends RealmObject implements Comparable<PairDoubleString> {
    public Double score;
    public String name;

    public PairDoubleString() {

    }

    public PairDoubleString(Double score, String name) {
        this.score = score;
        this.name = name;
    }

    @Override
    public int compareTo(PairDoubleString p)
    {
        if (score < p.score) return 1;
        return -1;
    }
}
