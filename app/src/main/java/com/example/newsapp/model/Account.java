package com.example.newsapp.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Account extends RealmObject {
    @PrimaryKey
    public int id;

    public String name;

    public String password;
    public boolean active;

    public Account() {
    }

    public Account(String name, String pw)
    {
        this.name = name;
        this.password = pw;
        this.id = name.hashCode();
        this.active = false;
    }

    public Account(String name, String pw, boolean on)
    {
        this(name, pw);
        this.active = on;
    }

}
