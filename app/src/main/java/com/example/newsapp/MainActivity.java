package com.example.newsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
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
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();

    RequestQueue queue;
    Realm realm;
    NewsListAdapter adapter;
    SwipeRefreshLayout swipeRefreshLayout;
    NavigationView navigationView;
    SharedPreferences sharedPreferences;
    Button shiftMode;
    boolean darkMode;
    Date lastFetch;
    String category = "";
    boolean onlyStarred = false;
    String searchKeyword = "";
    List<Category> allCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // dark mode
        darkMode = sharedPreferences.getBoolean("darkMode", false);
        if (darkMode) {
            Log.d(TAG, "dark mode enabled");
            setTheme(R.style.AppThemeDark);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        allCategories = new ArrayList<>();
        allCategories.add(new Category(R.id.nav_entertainment, "entertainment", "娱乐"));
        allCategories.add(new Category(R.id.nav_military, "military", "军事"));
        allCategories.add(new Category(R.id.nav_education, "education", "教育"));
        allCategories.add(new Category(R.id.nav_culture, "culture", "文化"));
        allCategories.add(new Category(R.id.nav_health, "health", "健康"));
        allCategories.add(new Category(R.id.nav_finance, "finance", "财经"));
        allCategories.add(new Category(R.id.nav_sports, "sports", "体育"));
        allCategories.add(new Category(R.id.nav_car, "car", "汽车"));
        allCategories.add(new Category(R.id.nav_tech, "tech", "科技"));
        allCategories.add(new Category(R.id.nav_society, "society", "社会"));

        realm = Realm.getDefaultInstance();

        shiftMode = findViewById(R.id.shift_mode);
        shiftMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor pref = sharedPreferences.edit();
                pref.putBoolean("darkMode", !darkMode);
                pref.apply();

                reload();
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
        adapter.setOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                News news = adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, NewsDetailActivity.class);
                intent.putExtra("id", news == null ? "" : news.newsID);
                startActivity(intent);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int last = manager.findLastVisibleItemPosition();

                if (last >= 0 && last < adapter.getItemCount() && (!recyclerView.canScrollVertically(1) || last * 1.1 > manager.getItemCount()) && !onlyStarred) {
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

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                onlyStarred = item.getItemId() == R.id.nav_stars;
                searchKeyword = "";
                category = "";

                if (item.getItemId() == R.id.nav_pref) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.nav_search) {
                    LinearLayout layout = new LinearLayout(MainActivity.this);
                    layout.setPadding(50, 10, 50, 20);
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    layout.addView(input, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.search).setView(layout)
                            .setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    searchKeyword = input.getText().toString();
                                    updateData();
                                }
                            }).setCancelable(false).show();
                } else {
                    for (Category cat : allCategories) {
                        if (item.getItemId() == cat.navigationId) {
                            category = cat.chineseName;
                            break;
                        }
                    }
                }


                updateData();
                drawerLayout.closeDrawers();
                return true;
            }
        });

        updateData();
    }

    private void updateData() {
        RealmQuery<News> query;
        RealmResults<News> results;
        query = realm.where(News.class);
        if (category.length() > 0) {
            query = query.equalTo("category", category);
        } else if (onlyStarred) {
            query = query.equalTo("isStarred", true);
        } else if (searchKeyword.length() > 0) {
            query = query.equalTo("keywords.name", searchKeyword);

        }
        swipeRefreshLayout.setEnabled(!onlyStarred);
        if (query.count() < 50) {
            fetchData(null);
        }
        results = query.findAllAsync().sort("publishTime", Sort.DESCENDING);
        adapter.updateData(results);
    }

    @Override
    public void onResume() {
        super.onResume();

        Set<String> enabledCategories = sharedPreferences.getStringSet("categories", null);
        Menu menu = navigationView.getMenu();

        for (Category cat : allCategories) {
            menu.findItem(cat.navigationId).setVisible(enabledCategories == null || enabledCategories.contains(cat.name));
        }

        boolean newDarkMode = sharedPreferences.getBoolean("darkMode", false);
        shiftMode.setBackgroundResource(newDarkMode ? R.drawable.night : R.drawable.day);

        // pref changed
        if (darkMode != newDarkMode) {
            darkMode = newDarkMode;
            reload();
        }
    }

    void reload() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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
        String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=50&endDate=%s&words=%s&categories=%s", format.format(endDate), searchKeyword, category);
        Log.d(TAG, url);
        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        new FetchDataTask(swipeRefreshLayout).execute(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "network", error);
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

class Category {
    int navigationId;
    String name;
    String chineseName;

    Category(int navigationId, String name, String chineseName) {
        this.navigationId = navigationId;
        this.name = name;
        this.chineseName = chineseName;
    }
}
