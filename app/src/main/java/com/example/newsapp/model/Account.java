package com.example.newsapp.model;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Account extends RealmObject {
    @PrimaryKey
    public int id;

    public String name;
    public String avatar;

    public String password;
    public boolean active;

    public Account() {
    }

    public Account(String name, String pw, String avatar)
    {
        this.name = name;
        this.password = pw;
        this.id = name.hashCode();
        this.avatar = avatar;
        this.active = false;
    }

    public Account(String name, String pw, String avatar, boolean on)
    {
        this(name, pw, avatar);
        this.active = on;
    }

}
