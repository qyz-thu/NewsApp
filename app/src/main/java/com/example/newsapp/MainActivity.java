package com.example.newsapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    String msg = "Android: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.text1);
        textView.setText(R.string.rejection);
        Log.d(msg, "The onCreate() event.");

        IntentFilter filter = new IntentFilter("com.newsapp.CUSTOM_INTENT");
        BroadcastReceiver receiver = new MyReceiver();
        registerReceiver(receiver, filter);
    }

    public void broadcastIntent(View view){
        Toast.makeText(this, "button pushed.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.setAction("com.newsapp.CUSTOM_INTENT");
        sendBroadcast(intent);
    }

    public void startService(View view)
    {
        startService(new Intent(getBaseContext(), MyService.class));
    }

    public void stopService(View view)
    {
        stopService(new Intent(getBaseContext(), MyService.class));
    }
}
