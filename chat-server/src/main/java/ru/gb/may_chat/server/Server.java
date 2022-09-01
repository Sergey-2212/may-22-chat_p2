package ru.gb.may_chat.server;

import ru.gb.may_chat.props.PropertyReader;
import ru.gb.may_chat.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.*;

public class Server {
    private final int port;
    private List<Handler> handlers;
    private UserService userService;

    private Executor thpool = Executors.newFixedThreadPool(2);

    public Server(UserService userService) {
        this.userService = userService;
        this.handlers = new ArrayList<>();
        port = PropertyReader.getInstance().getPort();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server start!");
            userService.start();
            while (true) {
                System.out.println("Waiting for connection......");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                Handler handler = new Handler(socket, this);
                thpool.execute(handler.handle());

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void broadcast(String from, String message) {
        String msg = BROADCAST_MESSAGE.getCommand() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }

    public void privateMessage(String from, String to, String message) {
        String msg = PRIVATE_MESSAGE.getCommand() + REGEX + String.format("[%s]=>[%s]: %s", from, to, message);
        for (Handler handler : handlers) {
            if (handler.getUser().equals(from) || handler.getUser().equals(to)) {
                handler.send(msg);
            }
        }
    }

    public void finalMessage (Handler handler) {
        String msg = CLOSE_SESSION_MESSAGE.getCommand() + REGEX;
        handler.send(msg);
    }

    public UserService getUserService() {
        return userService;
    }
    
    public synchronized boolean isUserAlreadyOnline(String nick) {
        for (Handler handler : handlers) {
            if (handler.getUser().equals(nick)) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized void addHandler(Handler handler) {
        this.handlers.add(handler);
        sendContacts();
    }

    public synchronized void removeHandler(Handler handler) {
        this.handlers.remove(handler);
        sendContacts();
    }

    public synchronized void updateHandlerUsername() {
        sendContacts();
    }

    private void shutdown(){
        try {
            userService.stop();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendContacts() {
       String contacts = handlers.stream()
                .map(Handler::getUser)
                .collect(Collectors.joining(REGEX));
       String msg = LIST_USERS.getCommand() + REGEX + contacts;

        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }
}
