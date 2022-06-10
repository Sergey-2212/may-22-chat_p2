package ru.gb.may_chat.client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import javax.imageio.IIOException;
import java.io.IOException;
import java.util.Date;

public class Application extends javafx.application.Application {

    public static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("/ChatWindow.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        primaryStage.setScene(scene);
        primaryStage.setTitle("May chat");
        primaryStage.show();
        //ChatController controller = loader.getController();
        //Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(120), x-> {
        //primaryStage.fireEvent(new WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        // }));
        //timeline.setCycleCount(1);
        //timeline.play();

    }
}
