package ru.gb.may_chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gb.may_chat.enums.Command;
import ru.gb.may_chat.server.error.NickAlreadyIsBusyException;
import ru.gb.may_chat.server.error.WrongCredentialsException;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.AUTH_MESSAGE;
import static ru.gb.may_chat.enums.Command.AUTH_OK;
import static ru.gb.may_chat.enums.Command.CHANGE_NICK_OK;
import static ru.gb.may_chat.enums.Command.ERROR_MESSAGE;

public class Handler {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread handlerThread;
    private Server server;
    private String user;
    private boolean isAuthorized;
    private Logger logger = LogManager.getLogger(Handler.class);



    public Handler(Socket socket, Server server) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.isAuthorized = false;
            logger.info(String.format("Handler %s is created", this.getHandler()));
        } catch (IOException e) {
            //System.err.println("Connection problems with user: " + user);
            logger.error(String.format("Connection problems with user:  %s",user));
        }
    }

    public Runnable handle() {
        handlerThread = new Thread(() -> {
            authorize();
            if (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                //System.out.println("Auth done");
                logger.info(String.format("User %s is authorused.",user));
            }
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {
                    String message = in.readUTF();
                    parseMessage(message);
                } catch (IOException e) {
                    logger.info(String.format("Connection broken with client: %s", user));
                    //System.out.println("Connection broken with client: " + user);
                    server.removeHandler(this);
                    Thread.currentThread().interrupt();
                }
            }
        });


        return handlerThread;
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

        switch (command) {
            case BROADCAST_MESSAGE -> server.broadcast(user, split[1]);
            case PRIVATE_MESSAGE -> server.privateMessage(user, split[1], split[2]);
            case CHANGE_NICK -> changeNick(split[1]);
            default -> logger.warn(String.format("Unknown message: %s",message));
        }
    }

    private void changeNick(String newNick) {
       try {
          server.getUserService().changeNick(user, newNick);
          user = newNick;
          server.updateHandlerUsername();
          send(CHANGE_NICK_OK.getCommand() + REGEX + newNick);
        } catch (NickAlreadyIsBusyException e) {
           send(ERROR_MESSAGE.getCommand() + REGEX + "This nickname already in use");
       }
    }

    private void authorize() {
        //System.out.println("Authorizing");
        logger.info(String.format("Handler {%s} authorizing has started",user));
        Thread daemon = new Thread(new DaemonThread());
        daemon.setDaemon(true);
        daemon.start();
        logger.info(String.format("%s Daemon is started.", this));
        try {
            while (!socket.isClosed()) {
                String msg = in.readUTF();
                if (msg.startsWith(AUTH_MESSAGE.getCommand())) {
                    String[] parsed = msg.split(REGEX);
                    String response = "";
                    String nickname = null;

                    try {
                        nickname = server.getUserService().authenticate(parsed[1], parsed[2]);
                    } catch (WrongCredentialsException e) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + e.getMessage();
                        logger.info(String.format("Wrong credentials: %s",parsed[1]));
                        //System.out.println("Wrong credentials: " + parsed[1]);
                    }
                    
                    if (server.isUserAlreadyOnline(nickname)) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + "This client already connected";
                        //System.out.println("Already connected");
                        logger.info(String.format("User %s is already connected",user));
                    }

                    
                    if (!response.equals("")) {
                        send(response);
                    } else {
                        this.user = nickname;
                        logger.info(String.format("User %s has successfully authorised.", user));
                        send(AUTH_OK.getCommand() + REGEX + nickname);
                        server.addHandler(this);
                        this.isAuthorized = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            //System.out.println("authorize");
            logger.warn("authorize");
        }
    }

    public void send(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(String.format("Error with message sending: %s", msg), this::getHandler);
            //System.out.println("send + IOException");
        }
    }

    public Thread getHandlerThread() {
        return handlerThread;
    }

    public Handler getHandler() {return this;}

    public String getUser() {
        return user;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    private void shutDown() {
        try {
            server.finalMessage(getHandler()); //Передает сообщение о разрыве соединения клиенту
            //System.out.println("finalMessage");//Log
            logger.info(String.format("Shutdown message for client: ",user));
            in.close();
            out.close();
            if (socket != null && !socket.isClosed()) {socket.close();}
            //System.out.println("The socket is closed");
            logger.info(String.format("Socket is closed fo user - %s",user));
            if(!handlerThread.isInterrupted()) {handlerThread.interrupt();}
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(String.format("Something wrong with the shutdown of [%s]",user));
            //System.out.println("Something wrong with the shutdown");

        }

    }

    class DaemonThread implements Runnable { //параллельный поток следит за временем активности сессии и закрывает если есть первышение
        private final int secondsOfWaiting = 120;
        @Override
        public void run() {
            logger.info("Daemon thread starts working", Handler.this::getHandler);
            awaitingOfAuthorizing(secondsOfWaiting);
            logger.info("awaitingOfAuthorization is complete", Handler.this::getHandler);
            //System.out.println("awaitingOfAuthorization is complete");//Log

        }

        private void awaitingOfAuthorizing(int seconds) {
            int counter = 0;
            while (counter < seconds) {
                if (isAuthorized()) {
                    return;
                }
                try {
                    Thread.sleep(1000);
                    if (counter % 10 == 0) {System.out.println("Estimated " + (secondsOfWaiting - counter ) + " seconds.");}
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    //System.out.println("DaemonThread exception");
                    logger.error("DaemonThread InterruptedException", Handler.this::getHandler);
                }
                counter++;
            }
            shutDown();
        }
    }
}
