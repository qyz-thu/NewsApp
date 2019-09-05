package com.example.newsapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsapp.model.Account;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.example.newsapp.NewsApp.currentAccount;
import static com.example.newsapp.NewsApp.currentAccountId;

public class AccountManageActivity extends AppCompatActivity {
    static String TAG = AccountManageActivity.class.getName();

    TextView currentTitleView;
    ImageView currentImageView;
    ImageView editProfileView;
    ImageView plusView;
    ImageView changeView;
    TextView starNumberView;
    TextView readNumberView;


    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_management);

        currentTitleView = findViewById(R.id.account_manage_name);
        currentImageView = findViewById(R.id.account_manage_avatar);
        starNumberView = findViewById(R.id.star_news_number);
        readNumberView = findViewById(R.id.read_news_number);

        realm = Realm.getDefaultInstance();

        int star_number = currentAccount.starred_news.size();
        int read_number = currentAccount.read_news.size();
        String text = starNumberView.getText().toString() + star_number;
        starNumberView.setText(text);
        text = readNumberView.getText().toString() + read_number;
        readNumberView.setText(text);

        if (currentAccount.id == "chenjiajie".hashCode())
            currentImageView.setImageResource(R.drawable.cjj_avatar);
        else if (currentAccount.id == "qianyingzhuo".hashCode())
            currentImageView.setImageResource(R.drawable.qyz_avatar);
        else currentImageView.setImageResource(R.drawable.default_avatar);
        currentTitleView.setText(currentAccount.name);

        setEditProfileView();
        setAddAccountView();
        setSwitchAccountView();

    }

    private void setEditProfileView() {
        editProfileView = findViewById(R.id.edit_profile);
        editProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "edit profile", Toast.LENGTH_LONG).show();

                final AlertDialog.Builder builder = new AlertDialog.Builder(AccountManageActivity.this);
                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(AccountManageActivity.this, R.layout.edit_profile, null);
                dialog.setView(dialogView);
                dialog.show();

                final ImageView avatarView = dialogView.findViewById(R.id.edit_avatar);
                if (currentAccount.id == "chenjiajie".hashCode())
                    avatarView.setImageResource(R.drawable.cjj_avatar);
                else if (currentAccount.id == "qianyingzhuo".hashCode())
                    avatarView.setImageResource(R.drawable.qyz_avatar);
                else avatarView.setImageResource(R.drawable.default_avatar);

                final EditText editPassword = dialogView.findViewById(R.id.password_entry);
                final EditText editUsername = dialogView.findViewById(R.id.username_entry);
                final EditText editNewPassword = dialogView.findViewById(R.id.new_password_entry);
                editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                editNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                editUsername.setHint(currentAccount.name);

                Button cancelButton = dialogView.findViewById(R.id.button_cancel);
                Button confirmButton = dialogView.findViewById(R.id.button_confirm);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String pw = editPassword.getText().toString();
                        if (!pw.equals(currentAccount.password))
                            Toast.makeText(AccountManageActivity.this, "Incorrect password!", Toast.LENGTH_LONG).show();
                        else {
                            boolean legal = true;
                            String new_username = editUsername.getText().toString();
                            Pattern pattern = Pattern.compile("[^a-zA-Z0-9_\\-]");
                            Matcher matcher = pattern.matcher(new_username);
                            if (matcher.find())
                                Toast.makeText(AccountManageActivity.this,
                                        "Illegal characters in new username!", Toast.LENGTH_LONG).show();
                            else {
                                realm.beginTransaction();
                                // check if username duplicate
                                if (!new_username.equals("")) {
                                    int hc = new_username.hashCode();
                                    RealmResults<Account> res = realm.where(Account.class).equalTo("name", new_username).notEqualTo("id", currentAccount.id).findAll();
                                    if (res.size() != 0) {
                                        Toast.makeText(AccountManageActivity.this, "Username already exists!", Toast.LENGTH_SHORT).show();
                                        legal = false;
                                    } else currentAccount.name = new_username;
                                }

                                String new_password = editNewPassword.getText().toString();
                                if (!new_password.equals(""))
                                    currentAccount.password = new_password;
                                realm.commitTransaction();
                                if (!legal) return;
                                dialog.dismiss();
                                finish();
                                Intent intent = new Intent(AccountManageActivity.this, AccountManageActivity.class);
                                startActivity(intent);

                            }
                        }
                    }
                });
            }
        });
    }

    private void setAddAccountView() {
        plusView = findViewById(R.id.plus_button);
        plusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "add an account", Toast.LENGTH_LONG).show();
                // TODO
                final AlertDialog.Builder builder = new AlertDialog.Builder(AccountManageActivity.this);
                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(AccountManageActivity.this, R.layout.add_account, null);
                dialog.setView(dialogView);
                dialog.show();

                final EditText newUsername = dialogView.findViewById(R.id.new_account_name);
                final EditText newPassword = dialogView.findViewById(R.id.new_account_password);
                final EditText confirmPassword = dialogView.findViewById(R.id.confirm_new_account_password);
                newPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

                Button cancelButton = dialogView.findViewById(R.id.add_button_cancel);
                Button confirmButton = dialogView.findViewById(R.id.add_button_confirm);

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String username = newUsername.getText().toString();
                        String password = newPassword.getText().toString();
                        String confirm_password = confirmPassword.getText().toString();

                        if (!password.equals(confirm_password)) {
                            Toast.makeText(AccountManageActivity.this, "Passwords are not the same!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        RealmResults<Account> res = realm.where(Account.class).equalTo("name", username).findAll();
                        if (res.size() != 0) {
                            Toast.makeText(AccountManageActivity.this, "Username already exists!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Account newAccount = new Account(username, password, true);
                        int hc = username.hashCode();
                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(newAccount);
                        currentAccount.active = false;
                        currentAccount = realm.where(Account.class).equalTo("id", hc).findFirst();
                        currentAccountId = currentAccount.id;
                        Log.d(TAG, "Account id is now " + currentAccountId);
                        realm.commitTransaction();
                        dialog.dismiss();
                        finish();
                        Intent intent = new Intent(AccountManageActivity.this, AccountManageActivity.class);
                        startActivity(intent);
                    }
                });

            }
        });
    }

    private void setSwitchAccountView() {
        changeView = findViewById(R.id.change_button);
        changeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "switch an account", Toast.LENGTH_LONG).show();
                // TODO
                final AlertDialog.Builder builder = new AlertDialog.Builder(AccountManageActivity.this);
                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(AccountManageActivity.this, R.layout.switch_account, null);
                dialog.setView(dialogView);
                dialog.show();

                final EditText username = dialogView.findViewById(R.id.switch_username_entry);
                final EditText password = dialogView.findViewById(R.id.switch_password_entry);
                password.setTransformationMethod(PasswordTransformationMethod.getInstance());

                Button cancelButton = dialogView.findViewById(R.id.switch_button_cancel);
                Button confirmButton = dialogView.findViewById(R.id.switch_button_confirm);

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String name = username.getText().toString();
                        String pw = password.getText().toString();
                        Account newAccount = realm.where(Account.class).equalTo("name", name).findFirst();
                        if (newAccount == null) {
                            Toast.makeText(AccountManageActivity.this, "Account doesn't exist!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!pw.equals(newAccount.password)) {
                            Toast.makeText(AccountManageActivity.this, "Incorrect password!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        realm.beginTransaction();
                        currentAccount.active = false;
                        currentAccount = realm.where(Account.class).equalTo("name", name).findFirst();
                        currentAccount.active = true;
                        currentAccountId = currentAccount.id;
                        Log.d(TAG, "Account id is now " + currentAccountId);
                        realm.commitTransaction();
                        dialog.dismiss();
                        finish();
                        Intent intent = new Intent(AccountManageActivity.this, AccountManageActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });
    }
}
