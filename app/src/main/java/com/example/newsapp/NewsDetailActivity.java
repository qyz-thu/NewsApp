package com.example.newsapp;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.newsapp.model.Account;
import com.example.newsapp.model.Location;
import com.example.newsapp.model.News;
import com.example.newsapp.model.PairDoubleString;
import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.SimpleCircleButton;
import com.nightonke.boommenu.BoomMenuButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.GroundOverlay2;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class NewsDetailActivity extends AppCompatActivity {
    private static final String TAG = NewsDetailActivity.class.getName();

    TextView title_view;
    TextView contentTextView;
    TextView date_view;
    ViewPager viewPager;
    MapView mapView;
    TextView emptyRecommendTextView;
    TextView mapTitle;
    VideoView videoView;
    LinearLayout contentView;

    News news;
    String newsID;

    List<News> recommendNews;
    RealmResults<News> newsRealmResults;
    RequestQueue queue;
    RecommendListAdapter adapter;

    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.NewsDetailTheme);
        setContentView(R.layout.news_detail_activity);
        Intent intent = getIntent();

        queue = Volley.newRequestQueue(getApplicationContext());
        recommendNews = new ArrayList<>();

        title_view = findViewById(R.id.news_title);
        contentTextView = findViewById(R.id.news_content);
        date_view = findViewById(R.id.news_date);
        viewPager = findViewById(R.id.news_images);
        emptyRecommendTextView = findViewById(R.id.recommendation_empty);
        mapTitle = findViewById(R.id.map_title);
        videoView = findViewById(R.id.video_view);
        contentView = findViewById(R.id.content_view);
        mapView = findViewById(R.id.map_view);

        if (intent != null) {
            newsID = intent.getStringExtra("id");
        }

        realm = Realm.getDefaultInstance();
        news = realm.where(News.class).equalTo("newsID", newsID).findFirst();
        if (news != null) {
            title_view.setText(news.title);
            contentTextView.setText(news.content);
            DateFormat format = SimpleDateFormat.getDateTimeInstance();
            date_view.setText(format.format(news.publishTime) + " " + news.publisher);

            getRecommendation();

            if (news.images == null || news.images.size() == 0) {
                viewPager.setVisibility(View.GONE);
            }

            viewPager.setAdapter(new NewsImageAdapter(getSupportFragmentManager(), this, news.images));
            final BoomMenuButton bmb = findViewById(R.id.boom_menu);

            SimpleCircleButton.Builder builderText = new SimpleCircleButton.Builder().normalImageRes(R.drawable.ic_text)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, news.content);
                            startActivity(Intent.createChooser(intent, "分享新闻内容"));
                        }
                    });
            bmb.addBuilder(builderText);

            SimpleCircleButton.Builder builderImageText = new SimpleCircleButton.Builder().normalImageRes(R.drawable.ic_text2)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            View view = contentView;

                            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                                    view.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.WHITE);
                            view.draw(canvas);

                            AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> task = new FetchImagesTask(bitmap, FetchImagesTask.Target.SELECT, news.title, NewsDetailActivity.this);
                            task.execute(new ArrayList<String>());
                        }
                    });
            bmb.addBuilder(builderImageText);

            SimpleCircleButton.Builder builderImageTextWithImages = new SimpleCircleButton.Builder().normalImageRes(R.drawable.ic_archive)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            Toast.makeText(NewsDetailActivity.this, R.string.downloading_images, Toast.LENGTH_LONG).show();
                            View view = contentView;

                            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                                    view.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.WHITE);
                            view.draw(canvas);

                            AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> task = new FetchImagesTask(bitmap, FetchImagesTask.Target.SELECT, news.title, NewsDetailActivity.this);
                            task.execute(new ArrayList<>(news.images));
                        }
                    });
            bmb.addBuilder(builderImageTextWithImages);

            bmb.addBuilder(buildSharingButton(R.drawable.ic_images, FetchImagesTask.Target.SELECT));

            boolean isStarred = news.isStarred.contains(NewsApp.currentAccount);
            SimpleCircleButton.Builder builderImageStar = new SimpleCircleButton.Builder().normalImageRes(isStarred ? R.drawable.ic_star_off : R.drawable.ic_star_on)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            boolean isStarred = news.isStarred.contains(NewsApp.currentAccount);
                            realm.beginTransaction();
                            if (isStarred) {
                                news.isStarred.remove(NewsApp.currentAccount);
                            } else {
                                news.isStarred.add(NewsApp.currentAccount);
                            }
                            realm.commitTransaction();
                            Toast.makeText(NewsDetailActivity.this, isStarred ? R.string.news_starred_off : R.string.news_starred_on, Toast.LENGTH_LONG).show();

                            BoomButton button = bmb.getBoomButton(4);
                            button.getImageView().setImageDrawable(getDrawable(isStarred ? R.drawable.ic_star_off : R.drawable.ic_star_on));
                        }
                    });
            bmb.addBuilder(builderImageStar);

            SimpleCircleButton.Builder builderImageBrowser = new SimpleCircleButton.Builder().normalImageRes(R.drawable.ic_browse)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.url));
                            startActivity(intent);
                        }
                    });
            bmb.addBuilder(builderImageBrowser);

            bmb.addBuilder(buildSharingButton(R.drawable.ic_qq, FetchImagesTask.Target.QQ));
            bmb.addBuilder(buildSharingButton(R.drawable.ic_wechat, FetchImagesTask.Target.WECHAT));
            bmb.addBuilder(buildSharingButton(R.drawable.ic_weibo, FetchImagesTask.Target.WEIBO));


            mapTitle.setVisibility(news.locations.size() > 0 ? View.VISIBLE : View.GONE);


            if (news.video != null && news.video.length() > 0) {
                Log.d(TAG, "Loading video of url " + news.video);
                videoView.setVideoURI(Uri.parse(news.video));
                videoView.start();
                // looping
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(true);
                    }
                });
            } else {
                videoView.setVisibility(View.GONE);
            }

            RecyclerView recyclerView = findViewById(R.id.recommendations);
            recyclerView.setHasFixedSize(true);
            final LinearLayoutManager manager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter = new RecommendListAdapter(recommendNews, this));
            adapter.setOnItemClickListener(new RecommendListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    News news = adapter.data.get(position);
                    Intent intent = new Intent(NewsDetailActivity.this, NewsDetailActivity.class);
                    intent.putExtra("id", news == null ? "" : news.newsID);
                    startActivity(intent);
                }
            });


            // delay running these

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    boolean isRead = news.isRead.contains(NewsApp.currentAccount);
                    if (!isRead) {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                News n = realm.where(News.class).equalTo("newsID", newsID).findFirst();
                                Account current = realm.where(Account.class).equalTo("id", NewsApp.currentAccountId).findFirst();
                                n.isRead.add(current);
                                n.firstReadTime = new Date();
                            }
                        });
                    }
                }
            });

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getRecommendation();
                }
            });

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mapView.setTileSource(TileSourceFactory.MAPNIK);
                    mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
                    mapView.setMultiTouchControls(true);
                    mapView.setVisibility(news.locations.size() > 0 ? View.VISIBLE : View.GONE);

                    // ref: https://stackoverflow.com/questions/6210895/listview-inside-scrollview-is-not-scrolling-on-android
                    mapView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            int action = motionEvent.getAction();
                            switch (action) {
                                case MotionEvent.ACTION_DOWN:
                                    // Disallow ScrollView to intercept touch events.
                                    view.getParent().requestDisallowInterceptTouchEvent(true);
                                    break;

                                case MotionEvent.ACTION_UP:
                                    // Allow ScrollView to intercept touch events.
                                    view.getParent().requestDisallowInterceptTouchEvent(false);
                                    break;
                            }

                            return false;
                        }
                    });

                    IMapController controller = mapView.getController();
                    controller.setZoom(4);
                    controller.setCenter(new GeoPoint(48.39479, 129.49519));

                    Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.colored_elephant);
                    for (Location location : news.locations) {
                        GroundOverlay2 overlay2 = new GroundOverlay2();
                        overlay2.setTransparency(0.2f);
                        overlay2.setImage(image);
                        Double size = 1.0;
                        overlay2.setPosition(new GeoPoint(location.lat + size, location.lng - size), new GeoPoint(location.lat - size, location.lng + size));
                        mapView.getOverlayManager().add(overlay2);
                        //Log.d(TAG, String.format("Add overlay at %f %f", location.lat, location.lng));
                    }
                }
            });
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private SimpleCircleButton.Builder buildSharingButton(int drawable, final FetchImagesTask.Target target) {
        return new SimpleCircleButton.Builder().normalImageRes(drawable)
                .listener(new OnBMClickListener() {
                    @Override
                    public void onBoomButtonClick(int index) {
                        Toast.makeText(NewsDetailActivity.this, R.string.downloading_images, Toast.LENGTH_LONG).show();
                        View view = contentView;

                        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                                view.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(Color.WHITE);
                        view.draw(canvas);

                        AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> task = new FetchImagesTask(bitmap, target, news.title, NewsDetailActivity.this);
                        task.execute(new ArrayList<>(news.images));
                    }
                });
    }

    private void getRecommendation() {
        RealmQuery<News> query = Realm.getDefaultInstance().where(News.class);
        TreeSet<PairDoubleString> keywords = new TreeSet<>(news.keywords);
        query = query.beginGroup();

        int queryCount = 0;
        for (PairDoubleString p : keywords) {
            if (p.score < 0.3) break;
            //Log.d(TAG, p.name);

            // kinda slow..
            /*
            String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=10&startDate=&endDate=&words=%s&categories=", p.name);
            JsonObjectRequest req = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            new FetchDataTask(null).execute(response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(NewsDetailActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "network", error);
                        }
                    });
            queue.add(req);
            */

            if (queryCount > 0) query = query.or();
            query = query.equalTo("keywords.name", p.name);
            queryCount++;
            if (queryCount > 5)
                break;
        }

        if (news.category != null) {
            if (queryCount > 0) {
                query = query.or();
                queryCount++;
            }
            query = query.equalTo("category", news.category);
        }

        if (news.publisher != null) {
            if (queryCount > 0) {
                query = query.or();
                queryCount++;
            }
            query = query.equalTo("publisher", news.publisher);
        }

        query = query.endGroup();

        query = query.notEqualTo("newsID", news.newsID).sort("publishTime", Sort.DESCENDING).limit(20);
        newsRealmResults = query.findAllAsync();
        newsRealmResults.addChangeListener(new RealmChangeListener<RealmResults<News>>() {
            @Override
            public void onChange(RealmResults<News> res) {
                Log.d(TAG, "Realm got " + res.size());
                TreeMap<Double, News> map = new TreeMap<>();
                for (News n : res) {
                    double score = 0;
                    for (PairDoubleString myKeyword : news.keywords) {
                        for (PairDoubleString theirKeyword : n.keywords) {
                            if (myKeyword.name.equals(theirKeyword.name)) {
                                score += myKeyword.score * theirKeyword.score;
                            }
                        }
                    }
                    map.put(score, n);
                }

                recommendNews.clear();
                int i = 0;
                for (Double score : map.descendingKeySet()) {
                    if (++i > 3) {
                        break;
                    }
                    Log.d(TAG, String.format("Add recommend news with score %f", score));
                    recommendNews.add(map.get(score));
                }

                emptyRecommendTextView.setVisibility(recommendNews.size() > 0 ? View.GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });

    }

    // TODO: news recommendation

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
}

class FetchImagesTask extends AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> {
    private static String TAG = FetchImagesTask.class.getName();
    private Context context;

    private Target target;
    private String desc;
    private Bitmap bitmap;

    FetchImagesTask(Bitmap bitmap, Target target, String desc, Context context) {
        this.bitmap = bitmap;
        this.desc = desc;
        this.context = context;
        this.target = target;
    }

    @Override
    protected ArrayList<Uri> doInBackground(ArrayList<String>... urls) {
        String appId = null;
        switch (target) {
            case WECHAT:
                appId = "com.tencent.mm";
                break;
            case QQ:
                appId = "com.tencent.mobileqq";
                break;
            case WEIBO:
                appId = "com.sina.weibo";
                break;
        }

        if (appId != null) {
            try {
                context.getPackageManager().getPackageInfo(appId, PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException err) {
                // app not installed
                return null;
            }
        }

        ArrayList<Uri> result = new ArrayList<>();


        for (final String url : urls[0]) {
            try {
                Bitmap bitmap = Glide.with(context).asBitmap().load(url).submit().get();
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + url.hashCode() + ".png");
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                outputStream.close();
                result.add(getImageContentUri(context, file));
            } catch (Exception err) {
                // ignore
                err.printStackTrace();
            }
        }

        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_capture_" + new Date().getTime() + ".png");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 30, outputStream);
            outputStream.close();
            result.add(getImageContentUri(context, file));
        } catch (Exception err) {
            // ignore
            err.printStackTrace();
        }
        return result;
    }

    static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        Uri uri = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/images/media");
                uri = Uri.withAppendedPath(baseUri, "" + id);
            }

            cursor.close();
        }

        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }

    @Override
    protected void onPostExecute(final ArrayList<Uri> results) {
        if (results == null) {
            // not installed
            Toast.makeText(context, R.string.app_not_installed, Toast.LENGTH_LONG).show();
        } else {
            // ref: https://github.com/YaphetZhao/ShareAnywhere/blob/master/library_shareanywhere/src/main/java/com/yaphetzhao/library_shareanywhere/ShareAnyWhereUtil.java
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/png");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, results);
            intent.putExtra("Kdescription", desc);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Log.d(TAG, "Sharing " + results.toString());

            String packageName = null;
            switch (target) {
                case WECHAT:
                    intent.setPackage("com.tencent.mm");
                    packageName = "com.tencent.mm";
                    break;
                case QQ:
                    intent.setPackage("com.tencent.mobileqq");
                    packageName = "com.tencent.mobileqq";
                    break;
                case WEIBO:
                    intent.setPackage("com.sina.weibo");
                    packageName = "com.tencent.weibo";
                    break;
            }

            if (packageName != null) {
                for (Uri uri : results) {
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }

            if (target == Target.SELECT) {
                context.startActivity(Intent.createChooser(intent, "分享新闻全文与图片"));
            } else {
                context.startActivity(intent);
            }
        }

    }

    enum Target {
        SELECT,
        WECHAT,
        WEIBO,
        QQ
    }
}

