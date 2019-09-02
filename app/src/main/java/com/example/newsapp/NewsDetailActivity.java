package com.example.newsapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import com.bumptech.glide.Glide;
import com.example.newsapp.model.News;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.SimpleCircleButton;
import com.nightonke.boommenu.BoomMenuButton;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

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
        final News news = realm.where(News.class).equalTo("newsID", newsID).findFirst();
        if (news != null) {
            title_view.setText(news.title);
            content_view.setText(news.content);
            DateFormat format = SimpleDateFormat.getDateTimeInstance();
            date_view.setText(format.format(news.publishTime) + " " + news.publisher);

            if (!news.isRead) {
                realm.beginTransaction();
                news.isRead = true;
                realm.commitTransaction();
                realm.close();
            }

            if (news.images == null || news.images.size() == 0) {
                viewPager.setVisibility(View.GONE);
            }

            viewPager.setAdapter(new NewsImageAdapter(getSupportFragmentManager(), this, news.images));
            BoomMenuButton bmb = findViewById(R.id.boom_menu);

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

            SimpleCircleButton.Builder builderImage = new SimpleCircleButton.Builder().normalImageRes(R.drawable.ic_images)
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            Toast.makeText(NewsDetailActivity.this, R.string.downloading_images, 1000).show();
                            new FetchImagesTask(NewsDetailActivity.this).execute(new ArrayList(news.images));
                        }
                    });
            bmb.addBuilder(builderImage);

            for (int i = 2; i < bmb.getPiecePlaceEnum().pieceNumber(); i++) {
                SimpleCircleButton.Builder builder = new SimpleCircleButton.Builder().normalImageRes(R.drawable.elephant);
                bmb.addBuilder(builder);
            }
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

class FetchImagesTask extends AsyncTask<ArrayList<String>, Integer, ArrayList<Uri>> {
    private Context context;

    FetchImagesTask(Context context) {
        this.context = context;
    }

    @Override
    protected ArrayList<Uri> doInBackground(ArrayList<String>... urls) {
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
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, results);
        context.startActivity(Intent.createChooser(intent, "分享新闻图片"));
    }
}

