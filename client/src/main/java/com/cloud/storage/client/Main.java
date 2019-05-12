package com.cloud.storage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        primaryStage.setTitle("Cloud Storage");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        Network.getInstance().close();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
