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

        // 跳转到排行榜页面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/ranking.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) ((Button) fxmlLoader.getNamespace().get("usernameField")).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("排行榜");
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载排行榜页面");
            alert.showAndWait();
        }
    }

    @FXML
    protected void handleProfile() {
        System.out.println("点击了个人中心按钮");

        // 跳转到个人中心页面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/ranking.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) ((Button) fxmlLoader.getNamespace().get("usernameField")).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("个人中心");
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载个人中心页面");
            alert.showAndWait();
        }
    }

    @FXML
    protected void handleLogout() {
        System.out.println("点击了退出登录按钮");


    }

}
