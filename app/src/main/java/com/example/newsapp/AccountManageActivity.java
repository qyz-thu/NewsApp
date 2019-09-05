package com.example.newsapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.newsapp.model.Account;
import com.example.newsapp.model.News;
import com.example.newsapp.model.PairDoubleString;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
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
    ImageView avatarView;

    String newAvatar;
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

        long star_number = realm.where(News.class).equalTo("isStarred.id", currentAccountId).count();
        long read_number = realm.where(News.class).equalTo("isRead.id", currentAccountId).count();
        starNumberView.setText(String.format(getString(R.string.star_number), star_number));
        readNumberView.setText(String.format(getString(R.string.history_number), read_number));

        currentImageView.setImageURI(Uri.parse(currentAccount.avatar));
        currentTitleView.setText(currentAccount.name);

        setEditProfileView();
        setAddAccountView();
        setSwitchAccountView();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // select avatar
            // TODO: move it to background thread
            Uri selected = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selected);

                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "avatar_image_" + new Date().toString() + ".png");
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                outputStream.close();
                Uri newUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
                newAvatar = newUri.toString();
                avatarView.setImageURI(newUri);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    private void setEditProfileView() {
        editProfileView = findViewById(R.id.edit_profile);
        editProfileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AccountManageActivity.this, "edit profile", Toast.LENGTH_LONG).show();
                newAvatar = null;

                final AlertDialog.Builder builder = new AlertDialog.Builder(AccountManageActivity.this);
                final AlertDialog dialog = builder.create();
                View dialogView = View.inflate(AccountManageActivity.this, R.layout.edit_profile, null);
                dialog.setView(dialogView);
                dialog.show();

                avatarView = dialogView.findViewById(R.id.edit_avatar);
                avatarView.setImageURI(Uri.parse(currentAccount.avatar));

                avatarView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, 0);
                    }
                });

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

                                if (newAvatar != null) {
                                    currentAccount.avatar = newAvatar;
                                }
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
                        Account newAccount = new Account(username, password, NewsApp.getUriForResource(AccountManageActivity.this, R.drawable.default_avatar), true);
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
