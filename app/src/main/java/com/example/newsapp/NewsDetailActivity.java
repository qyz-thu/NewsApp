package com.example.newsapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.newsapp.model.News;

import java.text.SimpleDateFormat;

import io.realm.Realm;
import io.realm.RealmList;

public class NewsDetailActivity extends AppCompatActivity {
    private static final String TAG = NewsDetailActivity.class.getName();

    TextView title_view;
    TextView content_view;
    TextView date_view;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.NewsDetailTheme);
        setContentView(R.layout.news_detail_activity);
        Intent intent = getIntent();
        String newsID = "";
        title_view = findViewById(R.id.news_title);
        content_view = findViewById(R.id.news_content);
        date_view = findViewById(R.id.news_date);
        viewPager = findViewById(R.id.news_images);

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

            if (!news.isRead) {
                realm.beginTransaction();
                news.isRead = true;
                realm.commitTransaction();
            }

            if (news.images == null || news.images.size() == 0) {
                viewPager.setVisibility(View.GONE);
            }

            viewPager.setAdapter(new NewsImageAdapter(getSupportFragmentManager(), this, news.images));
        }
    }

    private class NewsImageAdapter extends FragmentPagerAdapter {
        RealmList<String> images;
        Context context;

        NewsImageAdapter(FragmentManager fragmentManager, Context context, RealmList<String> images) {
            super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.context = context;
            this.images = images;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new NewsImageFragment();
            Bundle args = new Bundle();
            args.putString("url", images.get(position));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return "图片 " + (position + 1);
        }
    }


    // TODO: share link; news recommendation; show multiple images
}
