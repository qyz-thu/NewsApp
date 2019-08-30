package com.example.newsapp;

import android.text.Layout;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

public class NewsListAdapter extends RecyclerView.Adapter<NewsListAdapter.ViewHolder> {

    MainActivity parent;

    NewsListAdapter(MainActivity parent) {
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
        News news = parent.allNews.get(position);
        holder.titleView.setText(news.title);
        if (!news.images.isEmpty()) {
            Log.d("Adapter", news.images.get(0));
            if (!news.images.get(0).equals(""))
                Glide.with(parent).load(news.images.get(0)).centerInside().into(holder.imageView);
            else
            {
//                ImageView view = parent.findViewById(R.id.imageView);
//                view.setImageResource(R.drawable.elephant);
                int resourceId = R.drawable.elephant;
                Glide.with(parent).load(resourceId).centerInside().into(holder.imageView);
            }
        }
        holder.timeView.setText(DateUtils.getRelativeTimeSpanString(news.publishTime.getTime()));
    }

    @Override
    public int getItemCount() {
        return parent.allNews.size();
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
