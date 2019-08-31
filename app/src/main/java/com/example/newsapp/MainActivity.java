package com.example.newsapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends Activity {
    NewsListAdapter adapter;
    RequestQueue queue;
    Realm realm;
    SwipeRefreshLayout swipeRefreshLayout;
    Date lastFetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // dark mode
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
        recyclerView.setItemViewCacheSize(20);
        final LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        RealmResults<News> results = realm.where(News.class).findAllAsync().sort("publishTime", Sort.DESCENDING);
        recyclerView.setAdapter(adapter = new NewsListAdapter(results, this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int last = manager.findLastVisibleItemPosition();

                if (!recyclerView.canScrollVertically(1) || last * 1.1 > manager.getItemCount()) {
                    Date publishTime = adapter.getItem(last).publishTime;
                    publishTime.setTime(publishTime.getTime() - 1);
                    fetchData(publishTime);
                }
            }
        });

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchData(null);
            }
        });

        fetchData(null);
    }

    void fetchData(Date endDate) {
        if (endDate == null) {
            endDate = new Date();
        }

        if (lastFetch != null && endDate.getTime() == lastFetch.getTime()) {
            return;
        }

        lastFetch = endDate;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss", Locale.CHINA);
        String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=15&endDate=%s&words=&categories=", format.format(endDate));
        Log.d("Main", url);
        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        new FetchDataTask(swipeRefreshLayout).execute(response);
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

class FetchDataTask extends AsyncTask<JSONObject, Integer, List<News>> {
    private WeakReference<SwipeRefreshLayout> swipeRefreshLayout;

    FetchDataTask(SwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = new WeakReference<>(swipeRefreshLayout);
    }

    @Override
    protected List<News> doInBackground(JSONObject... jsonObjects) {
        final JSONObject response = jsonObjects[0];
        List<News> result = new ArrayList<>();
        try {
            JSONArray data = response.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                final JSONObject obj = data.getJSONObject(i);
                News news = new News();
                news.assign(obj);
                result.add(news);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(final List<News> allNews) {
        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (News news : allNews) {
                    realm.copyToRealmOrUpdate(news);
                }
            }
        });

        SwipeRefreshLayout layout = swipeRefreshLayout.get();
        if (layout != null) {
            layout.setRefreshing(false);
        }
    }
}
