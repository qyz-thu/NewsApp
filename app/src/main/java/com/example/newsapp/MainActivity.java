package com.example.newsapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.newsapp.model.News;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends Activity {
    NewsListAdapter adapter;
    RequestQueue queue;
    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("darkMode", false)) {
            setTheme(R.style.AppThemeDark);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        realm = Realm.getDefaultInstance();

        TextView titleText = findViewById(R.id.title_text);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        titleText.setTypeface(tf);

        titleText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                SharedPreferences.Editor pref = sharedPreferences.edit();
                pref.putBoolean("darkMode", !sharedPreferences.getBoolean("darkMode", false));
                pref.apply();

                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        queue = Volley.newRequestQueue(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        RealmResults<News> results = realm.where(News.class).findAll().sort("publishTime", Sort.DESCENDING);
        recyclerView.setAdapter(adapter = new NewsListAdapter(results, this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int last = manager.findLastVisibleItemPosition();

                if (!recyclerView.canScrollVertically(1) || last + 5 > manager.getItemCount()) {
                    Date publishTime = adapter.getItem(last).publishTime;
                    publishTime.setTime(publishTime.getTime() - 1);
                    fetchData(publishTime);
                }
            }
        });

        fetchData(null);
    }

    void fetchData(Date endDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss", Locale.CHINA);
        String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=15&endDate=%s&words=&categories=", format.format(endDate != null ? endDate : new Date()));
        Log.d("Main", url);
        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("main", response.toString());
                        JSONArray data = null;
                        try {
                            data = response.getJSONArray("data");
                            for (int i = 0; i < data.length(); i++) {
                                final JSONObject obj = data.getJSONObject(i);
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        News news = new News();
                                        news.assign(obj);
                                        realm.copyToRealmOrUpdate(news);
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d("main", String.format("%d", adapter.getItemCount()));
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("main", "network", error);
                    }
                });
        queue.add(req);
    }

}
