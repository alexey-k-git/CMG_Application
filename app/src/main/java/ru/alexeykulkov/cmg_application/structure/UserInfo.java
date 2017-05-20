package ru.alexeykulkov.cmg_application.structure;


import java.io.Serializable;

// объект пользователя
public class UserInfo implements Serializable{

    private String login;
    private String avatarUrl;
    private int id;

    public UserInfo(int id, String login, String avatarUrl)
    {
        this.id=id;
        this.login = login;
        this.avatarUrl = avatarUrl;

    }

    public int getId()
    {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
