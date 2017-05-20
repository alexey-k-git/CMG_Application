package ru.alexeykulkov.cmg_application.database;

// Класс описывающий схему Базы данных
public class DataBaseScheme {

    public static final class UsersTable {
        public static final String NAME = "USERS";

        public static final class Cols {
            public static final String ID = "ID";
            public static final String LOGIN = "LOGIN";
            public static final String AVATAR = "AVATAR";
            public static final String AVATAR_URL = "AVATAR_URL";
        }
    }

    public static final class ExtendedUsersTable {
        public static final String NAME = "EXTENDED_USERS";

        public static final class Cols {
            public static final String LOGIN = "LOGIN";
            public static final String NAME = "NAME";
            public static final String COMPANY = "COMPANY";
            public static final String BIO = "BIO";
            public static final String EMAIL = "EMAIL";
        }
    }

}
