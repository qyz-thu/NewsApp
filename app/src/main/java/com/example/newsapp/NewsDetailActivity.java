package com.example.newsapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsapp.model.News;

import io.realm.Realm;
import io.realm.RealmResults;

public class NewsDetailActivity extends AppCompatActivity {
    TextView title_view;
    TextView content_view;
    ImageView image_view;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_detail_activity);
        Intent intent = getIntent();
        News news=null;
        String newsid="";
        title_view = findViewById(R.id.news_title);
        image_view = findViewById(R.id.news_image);
        content_view = findViewById(R.id.news_content);

        if (intent != null)
        {
            newsid = intent.getStringExtra("id");
        }

        Realm realm = Realm.getDefaultInstance();
        RealmResults<News> results = realm.where(News.class).equalTo("newsID", newsid).findAll();
        if (results.size() != 0)
        {
            news = results.get(0);
        }
        title_view.setText(news.title);
        content_view.setText(news.content);
        // TODO: show image; share link; news recommendation

    }
}
