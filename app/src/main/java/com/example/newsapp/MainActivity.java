package com.example.newsapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.material.navigation.NavigationView;

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
    String category;

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

        Button coloredElephant = findViewById(R.id.colored_elephant);
        coloredElephant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor pref = sharedPreferences.edit();
                pref.putBoolean("darkMode", !sharedPreferences.getBoolean("darkMode", false));
                pref.apply();

                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        TextView titleText = findViewById(R.id.title_text);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        titleText.setTypeface(tf);

        queue = Volley.newRequestQueue(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        final LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setItemPrefetchEnabled(true);
        recyclerView.setLayoutManager(manager);
        RealmResults<News> results = realm.where(News.class).findAllAsync().sort("publishTime", Sort.DESCENDING);
        recyclerView.setAdapter(adapter = new NewsListAdapter(results, this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int last = manager.findLastVisibleItemPosition();

                if (last >= 0 && last < adapter.getItemCount() && (!recyclerView.canScrollVertically(1) || last * 1.1 > manager.getItemCount())) {
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

        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_entertainment:
                        category = "娱乐";
                        break;
                    case R.id.nav_military:
                        category = "军事";
                        break;
                    case R.id.nav_education:
                        category = "教育";
                        break;
                    case R.id.nav_culture:
                        category = "文化";
                        break;
                    case R.id.nav_health:
                        category = "健康";
                        break;
                    case R.id.nav_finance:
                        category = "财经";
                        break;
                    case R.id.nav_sports:
                        category = "体育";
                        break;
                    case R.id.nav_car:
                        category = "汽车";
                        break;
                    case R.id.nav_tech:
                        category = "科技";
                        break;
                    case R.id.nav_society:
                        category = "社会";
                        break;
                    default:
                        category = "";
                        break;
                }
                fetchData(null);
                RealmResults<News> results;
                if (category.length() > 0) {
                    results = realm.where(News.class).equalTo("category", category).findAllAsync().sort("publishTime", Sort.DESCENDING);
                } else {
                    results = realm.where(News.class).findAllAsync().sort("publishTime", Sort.DESCENDING);
                }
                adapter.updateData(results);
                drawerLayout.closeDrawers();
                return true;
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
        String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=50&endDate=%s&words=&categories=%s", format.format(endDate), category);
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
        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
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
