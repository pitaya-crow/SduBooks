package org.example.sdubooks.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    protected void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            showAlert("提示", "请输入用户名", Alert.AlertType.WARNING);
            return;
        }

        if (password == null || password.isEmpty()) {
            showAlert("提示", "请输入密码", Alert.AlertType.WARNING);
            return;
        }

        if ("user".equals(username) && "user".equals(password)) {//检查用户名和密码是否正确
            showAlert("登录成功", "登录成功！", Alert.AlertType.INFORMATION);
        } else {
            showAlert("登录错误", "用户名或密码错误", Alert.AlertType.ERROR);
        }
    }

    @FXML
    protected void handleBackToAdmin() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/client-login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("用户端登录");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "无法加载客户端登录页面", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
