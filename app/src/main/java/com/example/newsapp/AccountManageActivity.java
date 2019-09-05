package com.example.newsapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static com.example.newsapp.NewsApp.currentAccount;

public class AccountManageActivity extends AppCompatActivity {
    TextView currentTitleView;
    ImageView currentImageView;
    ImageView editProfileView;
    ImageView plusView;
    ImageView changeView;


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

        editProfileView = findViewById(R.id.edit_profile);
        editProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "edit profile", Toast.LENGTH_LONG).show();

                final AlertDialog.Builder builder = new AlertDialog.Builder(AccountManageActivity.this);
                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(AccountManageActivity.this, R.layout.edit_profile,null);
                dialog.setView(dialogView);
                dialog.show();

                EditText editUsername = dialogView.findViewById(R.id.username_entry);
                editUsername.setHint(currentAccount.name);

                Button cancelButton = dialogView.findViewById(R.id.button_cancel);
                Button confirmButton = dialogView.findViewById(R.id.button_confirm);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO
                        dialog.dismiss();
                    }
                });
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO
                        dialog.dismiss();
                    }
                });


            }
        });

        plusView = findViewById(R.id.plus_button);
        changeView = findViewById(R.id.change_button);
        plusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "add an account", Toast.LENGTH_LONG).show();
                // TODO
            }
        });
        changeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "switch an account", Toast.LENGTH_LONG).show();

                // TODO
            }
        });
    }
}
