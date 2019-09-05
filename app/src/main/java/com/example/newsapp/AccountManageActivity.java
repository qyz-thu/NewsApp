package com.example.newsapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import static com.example.newsapp.NewsApp.currentAccount;

public class AccountManageActivity extends AppCompatActivity {
    TextView currentTitleView;
    ImageView currentImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_management);

        currentTitleView = findViewById(R.id.account_manage_name);
        currentImageView = findViewById(R.id.account_manage_avatar);

        if (currentAccount != null)
        {
            if (currentAccount.name.equals("chenjiajie"))
                currentImageView.setImageResource(R.drawable.cjj_avatar);
            else if (currentAccount.name.equals("qianyingzhuo"))
                currentImageView.setImageResource(R.drawable.qyz_avatar);
            currentTitleView.setText(currentAccount.name);
        }
    }
}
