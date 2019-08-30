package com.example.newsapp;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.newsapp.model.News;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class NewsListAdapter extends RealmRecyclerViewAdapter<News, NewsListAdapter.ViewHolder> {

    MainActivity parent;

    NewsListAdapter(RealmResults<News> list, MainActivity parent) {
        super(list, true, true);
        this.parent = parent;
    }

    @NonNull
    @Override
    public NewsListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_card, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsListAdapter.ViewHolder holder, int position) {
        News news = getItem(position);
        holder.titleView.setText(news.title);
        if (!news.images.isEmpty()) {
            if (!news.images.get(0).equals(""))
                Glide.with(parent).load(news.images.get(0)).centerInside().into(holder.imageView);
            else {
                int resourceId = R.drawable.elephant;
                Glide.with(parent).load(resourceId).centerInside().into(holder.imageView);
            }
        }
        holder.timeView.setText(DateUtils.getRelativeTimeSpanString(news.publishTime.getTime()));
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        ImageView imageView;
        TextView titleView;
        TextView timeView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageView = itemView.findViewById(R.id.imageView);
            titleView = itemView.findViewById(R.id.titleView);
            timeView = itemView.findViewById(R.id.timeView);
        }
    }
}
