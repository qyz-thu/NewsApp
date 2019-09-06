package com.example.newsapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.example.newsapp.model.PairDoubleString;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.example.newsapp.NewsApp.currentAccount;

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

    CurrentView currentView = CurrentView.HOME;
    String searchKeyword = "";
    List<Category> allCategories;
    RealmResults<News> results;

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

        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 0);
            }
        }

        realm = Realm.getDefaultInstance();


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
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Lato-Italic.ttf");
        titleText.setTypeface(tf);

        queue = Volley.newRequestQueue(getApplicationContext());

        final RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        final LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setItemPrefetchEnabled(true);
        recyclerView.setLayoutManager(manager);
        results = realm.where(News.class).findAllAsync().sort("publishTime", Sort.DESCENDING);
        recyclerView.setAdapter(adapter = new NewsListAdapter(results, this));
        adapter.setOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String newsID) {
                Intent intent = new Intent(MainActivity.this, NewsDetailActivity.class);
                intent.putExtra("id", newsID);
                startActivity(intent);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int last = manager.findLastVisibleItemPosition();

                if (last >= 0 && last < adapter.getItemCount() && (!recyclerView.canScrollVertically(1) || last * 1.1 > manager.getItemCount()) &&
                        (currentView == CurrentView.HOME || currentView == CurrentView.CATEGORY || currentView == CurrentView.SEARCH)) {
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
        View headView = navigationView.getHeaderView(0);
        TextView account_name = headView.findViewById(R.id.account_name_view);
        account_name.setText(String.format(getString(R.string.my_account), currentAccount.name));

        ImageView account_avatar = headView.findViewById(R.id.account_avatar_view);
        account_avatar.setImageURI(Uri.parse(currentAccount.avatar));
        account_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "you have clicked the avatar.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, AccountManageActivity.class);
                startActivity(intent);
            }
        });


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                searchKeyword = "";
                category = "";

                if (item.getItemId() == R.id.nav_pref) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.nav_search) {
                    currentView = CurrentView.SEARCH;
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
                } else if (item.getItemId() == R.id.nav_stars) {
                    currentView = CurrentView.STARRED;
                } else if (item.getItemId() == R.id.nav_history) {
                    currentView = CurrentView.HISTORY;
                } else if (item.getItemId() == R.id.nav_recommend) {
                    currentView = CurrentView.RECOMMEND;
                } else {
                    for (Category cat : allCategories) {
                        if (item.getItemId() == cat.navigationId) {
                            currentView = CurrentView.CATEGORY;
                            category = cat.chineseName;
                            break;
                        }
                    }
                }
                Log.d(TAG, "Switch to " + currentView.toString());


                updateData();
                drawerLayout.closeDrawers();
                return true;
            }
        });


        updateData();
    }

    private void updateData() {
        RealmQuery<News> query;
        query = realm.where(News.class);
        if (category.length() > 0) {
            query = query.equalTo("category", category);
        } else if (currentView == CurrentView.STARRED) {
            query = query.equalTo("isStarred.id", NewsApp.currentAccountId);
        } else if (currentView == CurrentView.HISTORY) {
            query = query.equalTo("isRead.id", NewsApp.currentAccountId);
        } else if (currentView == CurrentView.RECOMMEND) {
            // find history first
            query = query.equalTo("isRead.id", NewsApp.currentAccountId);
        } else if (searchKeyword.length() > 0) {
            query = query.equalTo("keywords.name", searchKeyword);

        }
        swipeRefreshLayout.setEnabled(currentView == CurrentView.HOME || currentView == CurrentView.CATEGORY || currentView == CurrentView.SEARCH);
        if (query.count() < 50 && (currentView == CurrentView.HOME || currentView == CurrentView.CATEGORY || currentView == CurrentView.SEARCH)) {
            fetchData(null);
        }

        if (currentView == CurrentView.HISTORY || currentView == CurrentView.RECOMMEND) {
            results = query.findAllAsync().sort("firstReadTime", Sort.DESCENDING);
        } else {
            results = query.findAllAsync().sort("publishTime", Sort.DESCENDING);
        }

        if (currentView == CurrentView.RECOMMEND) {
            Log.d(TAG, "Updating recommend news");
            ArrayList<String> keywords = new ArrayList<>();
            int count = 0;
            for (News n : results) {
                for (PairDoubleString keyword : n.keywords) {
                    if (keyword.score > 0.5) {
                        keywords.add(keyword.name);
                        count ++;
                    }
                }
                if (count > 10) {
                    break;
                }
            }

            query = realm.where(News.class);
            query.beginGroup();
            for (int i = 0;i < keywords.size();i++) {
                if (i > 0) {
                    query = query.or();
                }
                query = query.equalTo("keywords.name", keywords.get(i));
            }
            query = query.endGroup();
            query = query.notEqualTo("isRead.id", NewsApp.currentAccountId);
            results = query.findAllAsync().sort("publishTime", Sort.DESCENDING);
        }

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

        updateData();
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
        swipeRefreshLayout.setRefreshing(true);
        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        new FetchDataTask(new Runnable() {
                            @Override
                            public void run() {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }).execute(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
                        swipeRefreshLayout.setRefreshing(false);
                        Log.d(TAG, "network", error);
                    }
                });
        queue.add(req);
    }

    enum CurrentView {
        HOME,
        CATEGORY,
        STARRED,
        HISTORY,
        SEARCH,
        RECOMMEND
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
