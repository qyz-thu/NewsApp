package com.example.newsapp;

import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.example.newsapp.model.News;

import java.util.Date;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class NewsListAdapter extends RealmRecyclerViewAdapter<News, NewsListAdapter.ViewHolder> {

    MainActivity parent;
    private OnItemClickListener onItemClickListener;

    NewsListAdapter(RealmResults<News> list, MainActivity parent) {
        super(list, true, true);
        setHasStableIds(true);
        this.parent = parent;
    }

    void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public NewsListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_card, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsListAdapter.ViewHolder holder, final int position) {
        final News news = getItem(position);
        holder.titleView.setText(news.title);
        holder.titleView.setTypeface(Typeface.DEFAULT, news.isRead ? Typeface.NORMAL : Typeface.BOLD);
        if (!news.images.isEmpty()) {
            String url = news.images.get(0);
            if (url != null && url.length() > 0) {
                CircularProgressDrawable drawable = new CircularProgressDrawable(parent);
                drawable.setStrokeWidth(5);
                drawable.setCenterRadius(30);
                drawable.start();
                Glide.with(parent).load(news.images.get(0)).placeholder(drawable).centerInside().into(holder.imageView);
            } else
                Glide.with(parent).load(R.drawable.elephant).centerInside().into(holder.imageView);
        } else {
            Glide.with(parent).load(R.drawable.elephant).centerInside().into(holder.imageView);
        }

        holder.timeView.setText(DateUtils.getRelativeTimeSpanString(news.publishTime.getTime(), new Date().getTime(), DateUtils.HOUR_IN_MILLIS));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onItemClick(news.newsID);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        News item = this.getItem(position);
        if (item != null && item.newsID != null) {
            return item.newsID.hashCode();
        } else {
            return 0;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(String newsID);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        ImageView imageView;
        TextView titleView;
        TextView timeView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageView = itemView.findViewById(R.id.imageView);
            titleView = itemView.findViewById(R.id.titleView);
            timeView = itemView.findViewById(R.id.timeView);
        }
    }
}
