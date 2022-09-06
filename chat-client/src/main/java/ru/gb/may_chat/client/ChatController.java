package ru.gb.may_chat.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.gb.may_chat.client.history.HistoryController;
import ru.gb.may_chat.client.net.MessageProcessor;
import ru.gb.may_chat.client.net.NetworkService;
import ru.gb.may_chat.enums.Command;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.AUTH_MESSAGE;
import static ru.gb.may_chat.enums.Command.BROADCAST_MESSAGE;
import static ru.gb.may_chat.enums.Command.CHANGE_NICK;
import static ru.gb.may_chat.enums.Command.PRIVATE_MESSAGE;

public class ChatController implements Initializable, MessageProcessor {

    private static final String BROADCAST_CONTACT = "ALL";
    @FXML
    private VBox changeNickPanel;

    @FXML
    private TextField newNickField;

    @FXML
    private VBox changePasswordPanel;

    @FXML
    private PasswordField oldPassField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private VBox loginPanel;

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox mainPanel;

    @FXML
    private TextArea chatArea;

    @FXML
    private ListView<String> contacts;

    @FXML
    private TextField inputField;

    @FXML
    private Button btnSend;

    private NetworkService networkService;

    private String user;

    private HistoryController historyController;

    public void mockAction(ActionEvent actionEvent) {
        System.out.println("mock");
    }

    public NetworkService getNetworkService() {
        return networkService;
    }


    public void closeApplication(ActionEvent actionEvent) {
        closeSession();


    }

    public void sendMessage(ActionEvent actionEvent) {
        try {
            String text = inputField.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            String recipient = contacts.getSelectionModel().getSelectedItem();
            if (recipient.equals(BROADCAST_CONTACT)) {
                networkService.sendMessage(BROADCAST_MESSAGE.getCommand() + REGEX + text);
                historyController.writeBroadcastmsg(text, user);
            } else {
                networkService.sendMessage(PRIVATE_MESSAGE.getCommand() + REGEX + recipient + REGEX + text);
                historyController.writePrivatemsg(text, user, recipient);
            }
            inputField.clear();

        } catch (IOException e) {
            showError("Network error");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(
                Alert.AlertType.ERROR,
                message,
                ButtonType.CLOSE
        );
        alert.showAndWait();
    }

    private void showCloseWarning() {
        Alert alert = new Alert(
                Alert.AlertType.WARNING,
                "Session closed, please restart the client",
                ButtonType.CLOSE
        );
        alert.showAndWait();
        closeSession();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        networkService = new NetworkService(this);
    }

    @Override
    public void processMessage(String message) {
        System.out.println("processing message");
        Platform.runLater(() -> parseMessage(message));
    }

    private void parseMessage(String message)  {
        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

            switch (command) {
                case AUTH_OK -> authOk(split);
                case ERROR_MESSAGE -> showError(split[1]);
                case LIST_USERS -> parseUsers(split);
                case CHANGE_NICK_OK -> handleChangeNick(split[1]);
                case CLOSE_SESSION_MESSAGE -> showCloseWarning(); // Сообщение о закрытии
                default -> {chatArea.appendText(split[1] + System.lineSeparator());
                    try {
                        historyController.writeInputmsg(split[1]);
                    } catch (IOException e) {
                        showError("Message logging is disabled!");
                    }
                }

            }
    }

    private void handleChangeNick(String newNick) {
        String oldnickname = user;
        historyController.changeFilename(oldnickname, newNick);
        user = newNick;
        returnToChat(null);
    }

    private void closeSession() {
        try {
            networkService.shutdown();
            Platform.exit();
            historyController.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
   }



    private void parseUsers(String[] split) {
        List<String> contact = new ArrayList<>(Arrays.asList(split));
        contact.set(0, BROADCAST_CONTACT);
        contacts.setItems(FXCollections.observableList(contact));
        contacts.getSelectionModel().selectFirst();
    }

    private void authOk(String[] split) {
        System.out.println("Auth ok");
        user = split[1];
        loginPanel.setVisible(false);
        mainPanel.setVisible(true);
        System.out.println("The user variable = " + user);
        historyController = new HistoryController(user);

        try {
            chatArea.appendText(historyController.getlasthistory(100));
        } catch (IOException e) {
            showError("Chathistory is not available.");
        }

    }

    public void sendChangeNick(ActionEvent actionEvent) {
        String newNick = newNickField.getText();

        if (newNick.isBlank()) {
            return;
        }
        try {
            networkService.sendMessage(CHANGE_NICK.getCommand() + REGEX + newNick);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Network error");
        }
    }

    public void returnToChat(ActionEvent actionEvent) {
         mainPanel.setVisible(true);
         changeNickPanel.setVisible(false);
    }

    public void sendChangePass(ActionEvent actionEvent) {
//TODO
    }

    public void sendAuth(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login.isBlank() || password.isBlank()) {
            return;
        }
        String msg = AUTH_MESSAGE.getCommand() + REGEX + login + REGEX + password;
        try {
            if (!networkService.isConnected()) {
                networkService.connect();
            }

            networkService.sendMessage(msg);
        } catch (IOException e) {
            showError("Network error");
        }
    }

    public void showChangeNickPanel(ActionEvent actionEvent) {
        changeNickPanel.setVisible(true);
        mainPanel.setVisible(false);
    }
}
