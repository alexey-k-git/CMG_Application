package ru.alexeykulkov.cmg_application;

import android.os.Handler;
import android.os.HandlerThread;

// отдельный поток для загрузки изображений в БД SQLite
public class Downloader extends HandlerThread {

    private static final String TAG = "Downloader";
    private Handler requestHanler;

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        requestHanler = new Handler();
    }

    public Downloader() {
        super(TAG);
    }

    public void downloadThis(Runnable  task)
    {
        requestHanler.post(task);
    }
}
