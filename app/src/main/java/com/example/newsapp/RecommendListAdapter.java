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
import java.util.List;

public class RecommendListAdapter extends RecyclerView.Adapter<RecommendListAdapter.ViewHolder> {
    List<News> data;
    NewsDetailActivity parent;
    private OnItemClickListener onItemClickListener;

    RecommendListAdapter(List<News> data, NewsDetailActivity parent) {
        this.data = data;
        this.parent = parent;
        setHasStableIds(true);
    }

    void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public RecommendListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_card, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendListAdapter.ViewHolder holder, final int position) {
        News news = data.get(position);
        holder.titleView.setText(news.title);
        boolean isRead = news.isRead.contains(NewsApp.currentAccount);
        holder.titleView.setTypeface(Typeface.DEFAULT, isRead ? Typeface.NORMAL : Typeface.BOLD);
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
                onItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        News item = data.get(position);
        if (item != null && item.newsID != null) {
            return item.newsID.hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int a);
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
