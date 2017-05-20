package ru.alexeykulkov.cmg_application.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ru.alexeykulkov.cmg_application.structure.ExtendedUserInfo;
import ru.alexeykulkov.cmg_application.structure.UserInfo;

import ru.alexeykulkov.cmg_application.database.DataBaseScheme.*;

// Класс для работы с базой данных
public class UsersLab {

    // кол-во пользователей которые будут подгружаться в оффлайн режиме
    final private int COUNT_FOR_VIEW = 15;

    private static UsersLab usersLab;
    private SQLiteDatabase database;

    private UsersLab(Context context) {
        database = new DataBase(context).getWritableDatabase();
    }

    public static UsersLab get(Context context) {
        if (usersLab == null) {
            usersLab = new UsersLab(context);
        }
        return usersLab;
    }

    // получение пользователей из БД начиная с определенного id
    public List<UserInfo> getUsers(int id) {
        UserCursorWrapper cursor = query(id);
        List<UserInfo> list = new ArrayList<UserInfo>();
        try {
            if (cursor.getCount() == 0) {
                return list;
            }
            cursor.moveToFirst();
            list.add(cursor.getUser());
            int i = 1;
            while (cursor.moveToNext() && i < COUNT_FOR_VIEW) {
                list.add(cursor.getUser());
                i++;
            }
            return list;

        } finally {
            cursor.close();
        }
    }

    // получение курсора в удобном виде для работы
    private UserCursorWrapper query(int id) {
        String[] columns = new String[]{UsersTable.Cols.LOGIN, UsersTable.Cols.AVATAR_URL, UsersTable.Cols.ID};
        String selection = UsersTable.Cols.ID + ">" + id;
        Cursor cursor = database.query(
                UsersTable.NAME,
                columns,
                selection,
                null,
                null,
                null,
                null
        );
        return new UserCursorWrapper(cursor);
    }

    // занесение в БД аватара пользователя
    synchronized public void putUserBitmap(int id, Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] array = stream.toByteArray();
        ContentValues values = new ContentValues();
        values.put(UsersTable.Cols.AVATAR, array);
        database.update(UsersTable.NAME, values, "id = " + id, null);
    }

    // получение аватара из БД
    public Bitmap getUserBitmap(int id) {
        Cursor cursor = database.query(
                UsersTable.NAME,
                new String[]{UsersTable.Cols.AVATAR},
                UsersTable.Cols.ID + "=" + id,
                null,
                null,
                null,
                null
        );
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            byte[] array = cursor.getBlob(0);
            if (array == null) return null;
            return BitmapFactory.decodeByteArray(array, 0, array.length);
        } else {
            return null;
        }
    }

    /*
    Обновление пользователей в БД.
    Если пользоватлей нет метод REPLACE сделает новые записи новых,
    если есть - заменит старые записи на новые.
     */
    public void updateUsers(List<UserInfo> users) {
        StringBuilder builder = new StringBuilder();
        UserInfo user;
        int lastIndex = users.size() - 1;
        for (int i = 0; i < lastIndex; i++) {
            user = users.get(i);
            builder.append("(" + user.getId() + ", \'" + user.getLogin() + "\',\'" + user.getAvatarUrl() + "\'),");
        }
        user = users.get(lastIndex);
        builder.append("(" + user.getId() + ", \'" + user.getLogin() + "\',\'" + user.getAvatarUrl() + "\');");
        String query = String.format("REPLACE INTO %s (%s,%s,%s) VALUES %s",
                UsersTable.NAME, UsersTable.Cols.ID, UsersTable.Cols.LOGIN, UsersTable.Cols.AVATAR_URL,
                builder.toString());
        database.execSQL(query);
    }

    // вствка/замена записи с расширенной информацией о пользователе в БД
    public void updateExtendedUser(ExtendedUserInfo extendedUser) {
        ContentValues values = getContentValues(extendedUser);
        database.replace(ExtendedUsersTable.NAME, null, values);
    }

    // получение расширенной информации о пользователе
    public ExtendedUserInfo getExtendedUserInfo(String login) {
        ExtendedUserInfo extendedUserInfo = null;
        String[] columns = new String[]{ExtendedUsersTable.Cols.LOGIN, ExtendedUsersTable.Cols.NAME,
                ExtendedUsersTable.Cols.COMPANY, ExtendedUsersTable.Cols.BIO,
                ExtendedUsersTable.Cols.EMAIL};
        Cursor cursor = database.query(
                ExtendedUsersTable.NAME,
                columns,
                ExtendedUsersTable.Cols.LOGIN + "=" + "'" + login + "'",
                null,
                null,
                null,
                null
        );
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            extendedUserInfo = new ExtendedUserCursorWrapper(cursor).getExtendedUser();
        }
        return extendedUserInfo;
    }

    // метод формирования объекта ContentValues для UserInfo
    private ContentValues getContentValues(UserInfo user) {
        ContentValues values = new ContentValues();
        values.put(UsersTable.Cols.LOGIN, user.getLogin());
        values.put(UsersTable.Cols.AVATAR_URL, user.getAvatarUrl());
        values.put(UsersTable.Cols.ID, user.getId());
        return values;
    }

    // метод формирования объекта ContentValues для ExtendedUserInfo
    private ContentValues getContentValues(ExtendedUserInfo user) {
        ContentValues values = new ContentValues();
        values.put(ExtendedUsersTable.Cols.LOGIN, user.getLogin());
        values.put(ExtendedUsersTable.Cols.NAME, user.getName());
        values.put(ExtendedUsersTable.Cols.COMPANY, user.getCompany());
        values.put(ExtendedUsersTable.Cols.BIO, user.getBio());
        values.put(ExtendedUsersTable.Cols.EMAIL, user.getEmail());
        return values;
    }

    // обёртка для курсора
    private class UserCursorWrapper extends CursorWrapper {

        public UserCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        public UserInfo getUser() {
            String login = getString(getColumnIndex(UsersTable.Cols.LOGIN));
            String avatar_url = getString(getColumnIndex(UsersTable.Cols.AVATAR_URL));
            int id = getInt(getColumnIndex(UsersTable.Cols.ID));
            UserInfo user = new UserInfo(id, login, avatar_url);
            return user;
        }
    }

    // обёртка для курсора
    private class ExtendedUserCursorWrapper extends CursorWrapper {

        public ExtendedUserCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        public ExtendedUserInfo getExtendedUser() {

            String login = getString(getColumnIndex(ExtendedUsersTable.Cols.LOGIN));
            String name = getString(getColumnIndex(ExtendedUsersTable.Cols.NAME));
            String company = getString(getColumnIndex(ExtendedUsersTable.Cols.COMPANY));
            String bio = getString(getColumnIndex(ExtendedUsersTable.Cols.BIO));
            String email = getString(getColumnIndex(ExtendedUsersTable.Cols.EMAIL));
            ExtendedUserInfo user = new ExtendedUserInfo(login, name, company, bio, email);
            return user;
        }
    }
}
