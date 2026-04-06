package org.example.sdubooks;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("admin-login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 450, 650);
        stage.setTitle("登录");
        stage.setScene(scene);
        stage.show();
    }
}
