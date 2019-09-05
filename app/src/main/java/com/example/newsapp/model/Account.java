package com.example.newsapp.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Account extends RealmObject {
    @PrimaryKey
    public int id;

    public String name;

    public String password;
    public RealmList<String> read_news;
    public RealmList<String> starred_news;
    public boolean active;

    public Account() {
    }

    public Account(String name, String pw)
    {
        this.name = name;
        this.password = pw;
        this.id = name.hashCode();
        this.read_news = new RealmList<>();
        this.starred_news = new RealmList<>();
        this.active = false;
    }

    public Account(String name, String pw, boolean on)
    {
        this(name, pw);
        this.active = on;
    }

}
