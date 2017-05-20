package ru.alexeykulkov.cmg_application.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.alexeykulkov.cmg_application.database.DataBaseScheme.*;

// База данных с в двумя таблицами
public class DataBase extends SQLiteOpenHelper {

    private static final String DB_NAME = "cmg";
    private static final int DB_VERSION = 1;

    public DataBase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase dataBase) {
        createTable(dataBase);
    }

    public void createTable(SQLiteDatabase dataBase) {
        dataBase.execSQL("create table " + UsersTable.NAME + "(" +
                UsersTable.Cols.ID + " INTEGER PRIMARY KEY, " +
                UsersTable.Cols.LOGIN + " TEXT, " +
                UsersTable.Cols.AVATAR_URL + " TEXT, " +
                UsersTable.Cols.AVATAR + " BLOB " +
                ")"
        );

        dataBase.execSQL("create table " + ExtendedUsersTable.NAME + "(" +
                ExtendedUsersTable.Cols.LOGIN + " TEXT PRIMARY KEY, " +
                ExtendedUsersTable.Cols.NAME + " TEXT, " +
                ExtendedUsersTable.Cols.COMPANY + " TEXT, " +
                ExtendedUsersTable.Cols.BIO + " TEXT, " +
                ExtendedUsersTable.Cols.EMAIL + " TEXT " +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase dataBase, int oldVersion, int newVersion) {
        // нет обновлению
    }

}