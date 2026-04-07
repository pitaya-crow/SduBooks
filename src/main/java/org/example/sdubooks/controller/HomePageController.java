package org.example.sdubooks.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
//因为那些页面还没有写，现在只是让控制台输出你点了什么按钮
public class HomePageController {
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

    @FXML
    protected void handleCategoryClick(ActionEvent event) {
        Button button = (Button) event.getSource();
        String category = (String) button.getUserData();
        System.out.println("点击了图书分类按钮: " + category);
    }

    @FXML
    protected void handleViewRankings() {
        System.out.println("点击了查看排行榜按钮");
    }
    //跳转到图书详情的还没写
}
