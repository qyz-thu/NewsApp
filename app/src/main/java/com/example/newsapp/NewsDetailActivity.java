package com.example.newsapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsapp.model.News;

import java.text.SimpleDateFormat;

import io.realm.Realm;

public class NewsDetailActivity extends AppCompatActivity {
    TextView title_view;
    TextView content_view;
    TextView date_view;
    ImageView image_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.NewsDetailTheme);
        setContentView(R.layout.news_detail_activity);
        Intent intent = getIntent();
        String newsID = "";
        title_view = findViewById(R.id.news_title);
        image_view = findViewById(R.id.news_image);
        content_view = findViewById(R.id.news_content);
        date_view = findViewById(R.id.news_date);

        if (intent != null) {
            newsID = intent.getStringExtra("id");
        }

        Realm realm = Realm.getDefaultInstance();
        News news = realm.where(News.class).equalTo("newsID", newsID).findFirst();
        if (news != null) {
            title_view.setText(news.title);
            content_view.setText(news.content);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
            date_view.setText(format.format(news.publishTime) + news.publisher);
            if (news.images != null && news.images.size() > 0) {
                Glide.with(this).load(news.images.get(0)).into(image_view);
            }

            if (!news.isRead) {
                realm.beginTransaction();
                news.isRead = true;
                realm.commitTransaction();
            }
        }

    }
    // TODO: share link; news recommendation; show multiple images
}
