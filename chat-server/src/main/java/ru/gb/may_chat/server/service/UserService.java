package ru.gb.may_chat.server.service;

import ru.gb.may_chat.server.model.User;

import java.sql.SQLException;

public interface UserService {
    void start();
    void stop() throws SQLException;
    String authenticate(String login, String password);
    String changeNick(String oldNick, String newNick);
    User createUser(String login, String password, String nick);
    void deleteUser(String login, String password);
    void changePassword(String login, String oldPassword, String newPassword);
}
