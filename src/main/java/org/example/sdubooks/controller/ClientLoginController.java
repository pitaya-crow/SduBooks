package org.example.sdubooks.controller;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.example.sdubooks.model.LoginResponse;

import okhttp3.*; // 引入 OkHttp

import java.io.IOException;

public class ClientLoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    // 后端接口地址
    private static final String LOGIN_URL = "http://localhost:8081/api/login";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @FXML
    protected void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 前端基础校验
        if (username.trim().isEmpty()) {
            showAlert("提示", "请输入用户名", Alert.AlertType.WARNING);
            return;
        }
        if (password.isEmpty()) {
            showAlert("提示", "请输入密码", Alert.AlertType.WARNING);
            return;
        }

        // 发送网络请求 (在新线程中执行，防止卡死界面)
        new Thread(() -> {
            try {
                // 构建请求 JSON: {"username": "...", "password": "..."}
                String jsonInput = gson.toJson(new UserRequest(username, password));

                // 构建 POST 请求
                RequestBody body = RequestBody.create(jsonInput, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(LOGIN_URL)
                        .post(body)
                        .build();

                // 执行请求
                try (Response response = client.newCall(request).execute()) {
                    if (response.body() != null) {
                        String responseBody = response.body().string();
                        LoginResponse loginResult = gson.fromJson(responseBody, LoginResponse.class);

                        // 切换回 JavaFX 主线程更新 UI
                        javafx.application.Platform.runLater(() -> {
                            if (loginResult.getCode() == 200) {
                                // 登录成功，跳转页面
                                navigateToHomepage();
                            } else {
                                // 登录失败，显示后端返回的错误信息
                                showAlert("登录失败", loginResult.getMessage(), Alert.AlertType.ERROR);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                // 网络异常处理
                javafx.application.Platform.runLater(() ->
                        showAlert("网络错误", "无法连接到服务器，请检查后端是否启动", Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    // 用于发送请求的临时类
    static class UserRequest {
        public String userName;//这里要和后端的请求参数保持一致
        public String password;
        public UserRequest(String u, String p) { this.userName = u; this.password = p; }
    }

    private void navigateToHomepage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/homepage-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("图书管理系统");
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "页面加载失败", Alert.AlertType.ERROR);
        }
    }

    @FXML
    protected void handleBackToAdmin() {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/admin-login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("管理员登录");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "无法加载管理员登录页面", Alert.AlertType.ERROR);
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
