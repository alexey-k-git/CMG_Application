package ru.alexeykulkov.cmg_application;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import ru.alexeykulkov.cmg_application.database.UsersLab;
import ru.alexeykulkov.cmg_application.structure.ExtendedUserInfo;
import ru.alexeykulkov.cmg_application.structure.UserInfo;

// Активность с подробной информацией о пользователе
public class UserInfoActivity extends AppCompatActivity {

    private TextView loginTextView;
    private TextView bioTextView;
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView companyTextView;
    private ImageView avatarImageView;
    // объект текущего пользователя
    private UserInfo user;
    // булевый флаг доступности интернета
    private boolean online;
    // объект для выгрузки изображений из интернета
    private Picasso picasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        loginTextView = (TextView) findViewById(R.id.loginTextView);
        bioTextView = (TextView) findViewById(R.id.bioTextView);
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        emailTextView = (TextView) findViewById(R.id.emailTextView);
        companyTextView = (TextView) findViewById(R.id.companyTextView);
        avatarImageView = (ImageView) findViewById(R.id.avatarImageView);
        if (savedInstanceState == null) {
            picasso = Picasso.with(UserInfoActivity.this);
            Intent intent = getIntent();
            user = (UserInfo) intent.getSerializableExtra("userInfo");
            online = intent.getBooleanExtra("online", false);
            loginTextView.setText(user.getLogin());
            if (online) {
                picasso.load(user.getAvatarUrl()).placeholder(R.drawable.please_wait).error(R.drawable.download_error).into(avatarImageView);
                QueryTask queryTask = new QueryTask();
                queryTask.execute(user.getLogin());
            } else {
                ExtendedUserInfo extendedUserInfo = UsersLab.get(this).getExtendedUserInfo(user.getLogin());
                fillFields(extendedUserInfo);
                Bitmap bitmap = UsersLab.get(getApplicationContext()).getUserBitmap(user.getId());
                if (bitmap == null) {
                    avatarImageView.setImageResource(R.drawable.no_image);
                } else {
                    avatarImageView.setImageBitmap(bitmap);
                }
            }
        } else {
            avatarImageView.setImageBitmap((Bitmap) savedInstanceState.getParcelable("avatar"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("avatar", ((BitmapDrawable) avatarImageView.getDrawable()).getBitmap());
    }

    // заполнение текстовых полей по данным объекта
    private void fillFields(ExtendedUserInfo extendedUserInfo) {
        if (extendedUserInfo == null) {
            return;
        }
        if (extendedUserInfo.getName() != "null") {
            nameTextView.setText(extendedUserInfo.getName());
        }
        if (extendedUserInfo.getCompany() != "null") {
            companyTextView.setText(extendedUserInfo.getCompany());
        }
        if (extendedUserInfo.getBio() != "null") {
            bioTextView.setText(extendedUserInfo.getBio());
        }
        if (extendedUserInfo.getEmail() != "null") {
            emailTextView.setText(extendedUserInfo.getEmail());
        }
    }

    //asyncTask для загрузки подробной информации о пользоватле
    class QueryTask extends AsyncTask<String, Void, ExtendedUserInfo> {

        // флаг ошибки
        boolean error;

        @Override
        protected ExtendedUserInfo doInBackground(String... login) {
            ExtendedUserInfo extendedUserInfo = null;
            try {
                String resultOfQuery = makeRequestToGithub(login[0]);
                extendedUserInfo = parseJSONResponse(resultOfQuery);
                UsersLab.get(UserInfoActivity.this).updateExtendedUser(extendedUserInfo);
            } catch (Exception e) {
                error = true;
            }
            return extendedUserInfo;
        }

        @Override
        protected void onPostExecute(ExtendedUserInfo result) {
            super.onPostExecute(result);
            if (error) {
                Toast.makeText(UserInfoActivity.this, "Произошла ошибка во время запроса", Toast.LENGTH_SHORT).show();
            }
            if (result != null) {
                fillFields(result);
            }
        }

        //преобразование JSON объекта в объект ExtendedUserInfo
        private ExtendedUserInfo parseJSONResponse(String jsonResponse) throws JSONException {
            ExtendedUserInfo extendedUserInfo = null;
            JSONObject jsonObject = new JSONObject(jsonResponse);
            extendedUserInfo =
                    new ExtendedUserInfo(jsonObject.getString("login"), jsonObject.getString("name"),
                            jsonObject.getString("company"), jsonObject.getString("bio"), jsonObject.getString("email"));
            return extendedUserInfo;
        }

        //GET запрос к GitHub для получения JSON объекта текущего пользователя
        private String makeRequestToGithub(String login) {
            StringBuilder builder = new StringBuilder();
            HttpsURLConnection connection = null;
            BufferedReader in = null;
            try {
                URL url = new URL("https://api.github.com/users/" + login);
                connection = (HttpsURLConnection) url.openConnection();
                if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    String line;
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((line = in.readLine()) != null) {
                        builder.append(line);
                    }
                }
            } catch (IOException e) {
                return null;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
            return builder.toString();
        }

    }

}
