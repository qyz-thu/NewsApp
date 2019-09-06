package com.example.newsapp;

import android.os.AsyncTask;

import com.example.newsapp.model.News;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class FetchDataTask extends AsyncTask<JSONObject, Integer, List<JSONObject>> {
    private Runnable callback;

    FetchDataTask(Runnable callback) {
        this.callback = callback;
    }

    @Override
    protected List<JSONObject> doInBackground(JSONObject... jsonObjects) {
        final JSONObject response = jsonObjects[0];
        List<JSONObject> result = new ArrayList<>();
        try {
            JSONArray data = response.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                result.add(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(final List<JSONObject> allNews) {
        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (JSONObject obj : allNews) {
                    News news = new News();
                    news.assign(obj);
                    realm.copyToRealmOrUpdate(news);
                }
            }
        });

        if (callback != null) {
            callback.run();
        }
    }
}
