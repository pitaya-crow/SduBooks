package org.example.sdubooks.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class BaseController {
    @FXML
    protected void handleRankings() {
        System.out.println("点击了排行榜按钮");
    }

    @FXML
    protected void handleProfile() {
        System.out.println("点击了个人中心按钮");
    }

    @FXML
    protected void handleAdminLogin() {
        System.out.println("点击了管理员登录按钮");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/admin-login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) ((Button) fxmlLoader.getNamespace().get("usernameField")).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("管理员登录");
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载管理员登录页面");
            alert.showAndWait();
        }
    }

}
