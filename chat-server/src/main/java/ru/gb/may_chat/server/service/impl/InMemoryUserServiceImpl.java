package ru.gb.may_chat.server.service.impl;

import ru.gb.may_chat.server.error.NickAlreadyIsBusyException;
import ru.gb.may_chat.server.error.UserNotFoundException;
import ru.gb.may_chat.server.error.WrongCredentialsException;
import ru.gb.may_chat.server.model.User;
import ru.gb.may_chat.server.service.UserService;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InMemoryUserServiceImpl implements UserService {

    private Connection connection;
    private Statement statement;
    //private List<User> users;

//    public InMemoryUserServiceImpl() {
//        this.users = new ArrayList<>();
//    }

    @Override
    public void start() {
        try {
            connect();

        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);

        }
//        users.addAll(List.of(
//                new User("log1", "pass1", "nick1"),
//                new User("log2", "pass2", "nick2"),
//                new User("log3", "pass3", "nick3"),
//                new User("log4", "pass4", "nick4"),
//                new User("log5", "pass5", "nick5")
//        ));
        System.out.println("User service started");
    }

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\Professional\\Documents\\Программирование\\GeekBrains\\1Четверть\\may-22-chat_p2\\ChatDB.db");
        statement = connection.createStatement();
    }

    private void disconnect () throws SQLException {
        if(!connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    public void stop() throws SQLException {
        disconnect();
        System.out.println("User service stopped");
    }

//O    @Override
//    public String authenticate(String login, String password) {
//        System.out.println("Auth log: " + login + " pass: " + password);
//        for (User user : users) {
//            if (login.equals(user.getLogin()) && password.equals(user.getPassword())) {
////            if (Objects.equals(login, user.getLogin()) && Objects.equals(password, user.getPassword())) {
//                return user.getNick();
//            }
//        }
//
//        throw new WrongCredentialsException("Wrong login or password");
//    }
    @Override
    public String authenticate(String login, String password) {
        try {
            ResultSet result = statement.executeQuery(
                    String.format("SELECT nickname FROM chatusers WHERE login = '%s' and pass = '%s'", login, password));
            String nick = result.getString(1);
                result.close();
            return nick;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WrongCredentialsException("Wrong login or password!");
        }
    }

//            @Override
//    public String changeNick(String oldNick, String newNick) {
//        if (isNickBusy(newNick)) {
//            throw new NickAlreadyIsBusyException();
//        }
//        User user = findUserByNickname(oldNick);
//        user.setNick(newNick);
//        return newNick;
//    }
   @Override
    public String changeNick(String oldNick, String newNick) {
       if (isNickBusy(newNick)) {
            throw new NickAlreadyIsBusyException();
       }
       try {
           statement.execute(String.format(
                   "UPDATE chatusers SET nickname = '%s' WHERE nickname = '%s'", newNick,oldNick));
           return statement.executeQuery(String.format(
                   "SELECT nickname FROM chatusers WHERE nickname = '%s'",newNick)).getString(1);
       } catch (SQLException e) {
           e.printStackTrace();
       }
       return null;
   }


//        private User findUserByNickname(String nickname) {
//        for (User user : users) {
//            if (user.getNick().equals(nickname)) {
//                return user;
//            }
//        }
//        throw new UserNotFoundException();
//    }
//    private User findUserByLogin(String login) {
//        for (User user : users) {
//            if (user.getLogin().equals(login)) {
//                return user;
//            }
//        }
//        throw new UserNotFoundException();
//    }

//    private boolean isNickBusy(String newNick) {
//        for (User user : users) {
//            if (user.getNick().equals(newNick)) {
//                return true;
//            }
//        }
//        return false;
//    }

    private boolean isNickBusy (String newNick) {
        try {
            ResultSet result = statement.executeQuery(String.format
                    ("SELECT nickname FROM chatusers WHERE nickname = '%s'", newNick));
            String reqestresult = result.getString(1);
            boolean compareResult = reqestresult.equals(newNick);
            result.close();
            return compareResult;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("isNickBusy made exception.");
        }
        return false;
    }

    @Override
    public User createUser(String login, String password, String nick) {
        return null; //TODO
    }

    @Override
    public void deleteUser(String login, String password) {
        //@TODO
    }

    @Override
    public void changePassword(String login, String oldPassword, String newPassword) {
        try {
            ResultSet result = statement.executeQuery(String.format("SELECT login FROM chatusers WHERE login = '%s' and pass = '%s'", login, oldPassword));
            if (result.getString(1).equals(login)) {
                statement.execute(String.format("UPDATE chatusers SET pass = '%s' WHERE pass = '%s' and login = '%s'", newPassword, oldPassword, login));//@TODO
            }
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WrongCredentialsException("Wrong login or password");
        }
    }
}
