package ru.alexeykulkov.cmg_application.structure;

// объект с расширенной информацией о пользователе
public class ExtendedUserInfo {

    private String login;
    private String name;
    private String company;
    private String bio;
    private String email;

    public ExtendedUserInfo(String login, String name, String company, String bio, String email) {
        this.login = login;
        this.name = name;
        this.company = company;
        this.bio = bio;
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getCompany() {
        return company;
    }

    public String getBio() {
        return bio;
    }

    public String getEmail() {
        return email;
    }
}
