package com.example.newsapp;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    NewsListAdapter adapter;
    List<News> allNews;
    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        TextView titletext = findViewById(R.id.title_text);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Lato-Regular.ttf");
        titletext.setTypeface(tf);

        allNews = new ArrayList<>();
        queue = Volley.newRequestQueue(this);

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter = new NewsListAdapter(this));

        fetchData();
    }

    void fetchData() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss", Locale.CHINA);
        String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=15&startDate=2019-07-01&endDate=%s&words=&categories=", format.format(new Date()));
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
                        allNews.add(new News(data.getJSONObject(i)));
                    }
                    Collections.sort(allNews);
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
