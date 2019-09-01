package com.example.newsapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsapp.model.News;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;

import io.realm.Realm;
import io.realm.RealmResults;

public class NewsDetailActivity extends AppCompatActivity {
    TextView title_view;
    TextView content_view;
    TextView date_view;
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
        date_view = findViewById(R.id.news_date);

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
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        date_view.setText(format.format(news.publishTime));
        final String url = news.images.get(0);
        if (url != null && url.length()>0) {
            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        Bitmap bitmap2 = (Bitmap) msg.obj;
                        image_view.setImageBitmap(bitmap2);
                    }
                }
            };

            new Thread() {
                public void run() {
                    Bitmap bitmap1 = getBitMap(url);
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = bitmap1;
                    handler.sendMessage(msg);
                }
            }.start();

        }

    }
    // TODO: share link; news recommendation; show multiple images

    private Bitmap getBitMap(String url)
    {
        Bitmap bitmap=null;
        URL picUrl=null;
        try {
            picUrl=new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection connection=(HttpURLConnection)picUrl.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream inputStream=connection.getInputStream();
            bitmap= BitmapFactory.decodeStream(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

}
