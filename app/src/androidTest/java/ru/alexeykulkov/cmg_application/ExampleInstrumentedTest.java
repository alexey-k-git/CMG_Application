package ru.alexeykulkov.cmg_application;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import ru.alexeykulkov.cmg_application.database.DataBase;
import ru.alexeykulkov.cmg_application.database.DataBaseScheme;
import ru.alexeykulkov.cmg_application.database.UsersLab;
import ru.alexeykulkov.cmg_application.structure.UserInfo;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {

        Context appContext = InstrumentationRegistry.getTargetContext();
        SQLiteDatabase database = new DataBase(appContext).getWritableDatabase();
        List<UserInfo> list = getDataBase(database);
        for(UserInfo user: list)
        {
            System.out.println(user.getId() +  "  " +  user.getLogin());
        }
    }


    private List<UserInfo> getDataBase(SQLiteDatabase database)
    {
        List<UserInfo> list = new ArrayList<>();
        String[] columns = new String[]{DataBaseScheme.UsersTable.Cols.LOGIN, DataBaseScheme.UsersTable.Cols.AVATAR_URL, DataBaseScheme.UsersTable.Cols.ID};
        Cursor cursor = database.query(
                DataBaseScheme.UsersTable.NAME,
                columns,
                null,
                null,
                null,
                null,
                null
        );
        UserCursorWrapper wraper =  new UserCursorWrapper(cursor);
        if (wraper.getCount()>0) {
            wraper.moveToFirst();
            list.add(wraper.getUser());
            while (wraper.moveToNext())
            {
                list.add(wraper.getUser());
            }
        }
        return list;

    }

    private class UserCursorWrapper extends CursorWrapper {

        public UserCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        public UserInfo getUser() {
            String login = getString(getColumnIndex(DataBaseScheme.UsersTable.Cols.LOGIN));
            String avatar_url = getString(getColumnIndex(DataBaseScheme.UsersTable.Cols.AVATAR_URL));
            int id = getInt(getColumnIndex(DataBaseScheme.UsersTable.Cols.ID));
            UserInfo user = new UserInfo(id, login, avatar_url);
            return user;
        }
    }

}


