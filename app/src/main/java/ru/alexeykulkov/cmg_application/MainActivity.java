package ru.alexeykulkov.cmg_application;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.net.ssl.HttpsURLConnection;

import ru.alexeykulkov.cmg_application.database.UsersLab;
import ru.alexeykulkov.cmg_application.structure.UserInfo;

public class MainActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private LinearLayout progressBar;
    // список с загруженными пользователями
    private ArrayList<UserInfo> users = new ArrayList<>();
    // булевый флаг доступности интернета
    private boolean online;
    // объект для выгрузки изображений из интернета
    private Picasso picasso;
    // потокобезопасный лист с id пользователей для которых уже загружен аватар
    private ConcurrentSkipListSet<Integer> loadedImages;
    // отдельный поток для загрузки изображений в БД SQLite
    private Downloader downloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usersRecyclerView = (RecyclerView) findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        progressBar = (LinearLayout) findViewById(R.id.progressBarLayout);
        online = isOnline();
        picasso = Picasso.with(MainActivity.this);
        downloader = new Downloader();
        downloader.start();
        downloader.getLooper();
        if (savedInstanceState == null) {
            loadedImages = new ConcurrentSkipListSet<Integer>();
            QueryTask queryTask = new QueryTask(false, online);
            queryTask.execute(0);
        } else {
            users = (ArrayList<UserInfo>) savedInstanceState.getSerializable("users");
            loadedImages = (ConcurrentSkipListSet<Integer>) savedInstanceState.getSerializable("loadedImages");
            usersRecyclerView.setAdapter(new UserListAdapter());
            // в случае прерывания первого asyncTask
            if (users.size()==0)
            {
                QueryTask queryTask = new QueryTask(false, online);
                queryTask.execute(0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloader.quit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        online = isOnline();
    }

    //открытие активности c подробностями пользователя
    public void startUserInfoActivity(UserInfo userInfo) {
        Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        intent.putExtra("online", online);
        startActivity(intent);
    }

    //проверка доступа к интернету
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("users", users);
        outState.putSerializable("loadedImages", loadedImages);
    }

    //адаптер для RecyclerView с пользователями
    private class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

        @Override
        public UserListAdapter.UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.user_element, parent, false);
            return new UserListAdapter.UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserListAdapter.UserViewHolder holder, int position) {
            final UserInfo user = users.get(position);
            holder.bindSong(user);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startUserInfoActivity(user);
                }
            });
            if (position == users.size() - 1) {
                Toast.makeText(MainActivity.this, "Подгрузка данных", Toast.LENGTH_SHORT).show();
                QueryTask queryTask = new QueryTask(true, online);
                queryTask.execute(user.getId());
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {

            private ImageView imageViewHolder;
            private TextView textViewHolder;

            public UserViewHolder(View itemView) {
                super(itemView);
                imageViewHolder = (ImageView) itemView.findViewById(R.id.userAvatarImageView);
                textViewHolder = (TextView) itemView.findViewById(R.id.userAvatarTexView);
            }

            public void bindSong(final UserInfo user) {
                textViewHolder.setText(user.getLogin());
                if (online) {
                    picasso.load(user.getAvatarUrl()).placeholder(R.drawable.please_wait).error(R.drawable.download_error).fit().into(imageViewHolder);
                    downloader.downloadThis(new Runnable() {
                        public void run() {
                            if (!loadedImages.contains(user.getId())) {
                                final Bitmap bitmap;
                                try {
                                    bitmap = picasso.load(user.getAvatarUrl()).get();
                                    if (Thread.interrupted()) return;
                                    UsersLab.get(getApplicationContext()).putUserBitmap(user.getId(), bitmap);
                                    loadedImages.add(user.getId());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    Bitmap bitmap = UsersLab.get(getApplicationContext()).getUserBitmap(user.getId());
                    if (bitmap == null) {
                        imageViewHolder.setImageResource(R.drawable.no_image);
                    } else {
                        imageViewHolder.setImageBitmap(bitmap);
                    }

                }
            }
        }
    }

    //asyncTask для загрузки/дозагрузки списка пользоватлей
    class QueryTask extends AsyncTask<Integer, Void, List<UserInfo>> {

        // флаг ошибки
        boolean error;
        // флаг повторного запуска
        boolean secondTime;
        // флаг наличия интернета
        boolean online;

        public QueryTask(boolean secondTime, boolean online) {
            this.secondTime = secondTime;
            this.online = online;
        }

        @Override
        protected void onPreExecute() {
            if (!secondTime) {
                progressBar.setVisibility(View.VISIBLE);
                usersRecyclerView.setVisibility(View.GONE);
            }
        }

        @Override
        protected List<UserInfo> doInBackground(Integer... id) {
            List<UserInfo> users = null;
            Context context = getApplicationContext();
            if (online) {
                try {
                    String resultOfQuery = makeRequestToGithub(id[0]);
                    users = parseJSONResponse(resultOfQuery);
                    UsersLab.get(context).updateUsers(users);
                } catch (Exception e) {
                    error = true;
                }
            } else {
                users = UsersLab.get(context).getUsers(id[0]);
            }
            return users;
        }

        @Override
        protected void onPostExecute(List<UserInfo> result) {
            super.onPostExecute(result);
            if (!secondTime) {
                usersRecyclerView.setAdapter(new UserListAdapter());
                progressBar.setVisibility(View.GONE);
                usersRecyclerView.setVisibility(View.VISIBLE);
                if (!online && users.size()==0)
                {
                    Toast.makeText(MainActivity.this, "Нет предварительно загруженных данных\n Перезапустите приложение с включенным интернетом", Toast.LENGTH_SHORT).show();
                }
            }
            if (error) {
                Toast.makeText(MainActivity.this, "Произошла ошибка во время запроса", Toast.LENGTH_SHORT).show();
                return;
            }
            if (result == null || result.size() == 0) {
                Toast.makeText(MainActivity.this, "Нечего подгружать", Toast.LENGTH_SHORT).show();
            } else {
                users.addAll(result);
                usersRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }


        //преобразование JSON объекта в список объектов
        private List<UserInfo> parseJSONResponse(String jsonResponse) throws JSONException {
            List<UserInfo> users = new ArrayList<UserInfo>();
            JSONArray array;
            array = new JSONArray(jsonResponse);
            Integer resultCount = array.length();
            if (resultCount > 0) {
                int id;
                String login;
                String avatar_url;
                for (int i = 0; i < array.length(); i++) {
                    id = array.getJSONObject(i).getInt("id");
                    login = array.getJSONObject(i).getString("login");
                    avatar_url = array.getJSONObject(i).getString("avatar_url");
                    users.add(new UserInfo(id, login, avatar_url));
                }
            }
            return users;
        }

        //GET запрос к GitHub для получения JSON объекта с пользователями
        private String makeRequestToGithub(Integer lastId) {
            StringBuilder builder = new StringBuilder();
            HttpsURLConnection connection = null;
            BufferedReader in = null;
            try {
                URL url = new URL("https://api.github.com/users?since=" + lastId);
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
