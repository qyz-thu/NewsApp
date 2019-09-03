package com.example.newsapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.newsapp.model.Location;
import com.example.newsapp.model.News;
import com.example.newsapp.model.PairDoubleString;
import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.SimpleCircleButton;
import com.nightonke.boommenu.BoomMenuButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

public class NewsDetailActivity extends AppCompatActivity {
    private static final String TAG = NewsDetailActivity.class.getName();

    TextView title_view;
    TextView content_view;
    TextView date_view;
    ViewPager viewPager;
    MapView mapView;

    News news;

    List<News> allNews;
    RequestQueue queue;

    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.NewsDetailTheme);
        setContentView(R.layout.news_detail_activity);
        Intent intent = getIntent();
        String newsID = "";

        queue = Volley.newRequestQueue(getApplicationContext());

        title_view = findViewById(R.id.news_title);
        content_view = findViewById(R.id.news_content);
        date_view = findViewById(R.id.news_date);
        viewPager = findViewById(R.id.news_images);

        mapView = findViewById(R.id.map_view);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        mapView.setMultiTouchControls(true);

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

        if (intent != null) {
            newsID = intent.getStringExtra("id");
        }

        realm = Realm.getDefaultInstance();
        news = realm.where(News.class).equalTo("newsID", newsID).findFirst();
        if (news != null) {
            title_view.setText(news.title);
            content_view.setText(news.content);
            DateFormat format = SimpleDateFormat.getDateTimeInstance();
            date_view.setText(format.format(news.publishTime) + " " + news.publisher);

            getRecommendation();



            if (!news.isRead) {
                realm.beginTransaction();
                news.isRead = true;
                realm.commitTransaction();
            }

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

            bmb.addBuilder(buildSharingButton(R.drawable.ic_images, FetchImagesTask.Target.SELECT));
            bmb.addBuilder(buildSharingButton(R.drawable.ic_qq, FetchImagesTask.Target.QQ));
            bmb.addBuilder(buildSharingButton(R.drawable.ic_wechat, FetchImagesTask.Target.WECHAT));

            SimpleCircleButton.Builder builderImageStar = new SimpleCircleButton.Builder().normalImageRes(news.isStarred ? R.drawable.ic_star_off : R.drawable.ic_star_on)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            realm.beginTransaction();
                            news.isStarred = !news.isStarred;
                            realm.commitTransaction();
                            Toast.makeText(NewsDetailActivity.this, news.isStarred ? R.string.news_starred_on : R.string.news_starred_off, Toast.LENGTH_LONG).show();

                            BoomButton button = bmb.getBoomButton(4);
                            button.getImageView().setImageDrawable(getDrawable(news.isStarred ? R.drawable.ic_star_off : R.drawable.ic_star_on));
                        }
                    });
            bmb.addBuilder(builderImageStar);

            bmb.addBuilder(buildSharingButton(R.drawable.ic_weibo, FetchImagesTask.Target.WEIBO));

            SimpleCircleButton.Builder builderImageBrowser = new SimpleCircleButton.Builder().normalImageRes(R.drawable.ic_browse)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.url));
                            startActivity(intent);
                        }
                    });
            bmb.addBuilder(builderImageBrowser);

            for (int i = 7; i < bmb.getPiecePlaceEnum().pieceNumber(); i++) {
                SimpleCircleButton.Builder builder = new SimpleCircleButton.Builder().normalImageRes(R.drawable.elephant);
                bmb.addBuilder(builder);
            }

            for (Location location : news.locations) {
                GroundOverlay2 overlay2 = new GroundOverlay2();
                overlay2.setTransparency(0.2f);
                overlay2.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.colored_elephant));
                Double size = 1.0;
                overlay2.setPosition(new GeoPoint(location.lat + size, location.lng - size), new GeoPoint(location.lat - size, location.lng + size));
                mapView.getOverlayManager().add(overlay2);
                Log.d(TAG, String.format("Add overlay at %f %f", location.lat, location.lng));
            }

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
                        AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> task = new FetchImagesTask(target, news.title, NewsDetailActivity.this);
                        task.execute(new ArrayList<>(news.images));
                    }
                });
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


    // TODO: news recommendation

    private void getRecommendation()
    {
        RealmList<PairDoubleString> keywords = news.keywords;
        allNews = new ArrayList<>();
        for (PairDoubleString p: keywords)
        {
            Log.d(TAG, p.name);
            String url = String.format("https://api2.newsminer.net/svc/news/queryNewsList?size=10&startDate=&endDate=&words=%s&categories=", p.name);
            JsonObjectRequest req = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {
                            JSONArray data;
                            try {
                                Log.d("detailed activity", response.toString());
                                data = response.getJSONArray("data");
                                for (int i=0;i<data.length();i++) {
                                    JSONObject obj = data.getJSONObject(i);
                                    News _news = new News();
                                    _news.assign(obj);
                                    allNews.add(_news);
                                    realm.copyToRealmOrUpdate(_news);
                                }

                            }catch (JSONException e)
                            {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "network", error);
                        }
                    });
            queue.add(req);
        }
        for (News n: allNews)
        {
            ;
        }
    }
}

class FetchImagesTask extends AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> {
    private Context context;

    private Target target;
    private String desc;

    FetchImagesTask(Target target, String desc, Context context) {
        this.desc = desc;
        this.context = context;
        this.target = target;
    }

    @Override
    protected ArrayList<Uri> doInBackground(ArrayList<String>... urls) {
        String appId = null;
        switch (target) {
            case WECHAT:
            case MOMENTS:
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
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + url.hashCode() + ".jpg");
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                outputStream.close();
                result.add(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file));
            } catch (Exception err) {
                // ignore
                err.printStackTrace();
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(final ArrayList<Uri> results) {
        if (results == null) {
            // not installed
            Toast.makeText(context, R.string.app_not_installed, Toast.LENGTH_LONG).show();
        } else {
            // ref: https://github.com/YaphetZhao/ShareAnywhere/blob/master/library_shareanywhere/src/main/java/com/yaphetzhao/library_shareanywhere/ShareAnyWhereUtil.java
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, results);
            intent.putExtra("Kdescription", desc);

            switch (target) {
                case WECHAT:
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"));
                    break;
                case MOMENTS:
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI"));
                    break;
                case QQ:
                    intent.setPackage("com.tencent.mobileqq");
                    break;
                case WEIBO:
                    intent.setPackage("com.sina.weibo");
                    break;
            }
            if (target == Target.SELECT) {
                context.startActivity(Intent.createChooser(intent, "分享新闻图片"));
            } else {
                context.startActivity(intent);
            }
        }

    }

    enum Target {
        SELECT,
        WECHAT,
        MOMENTS,
        WEIBO,
        QQ
    }
}

