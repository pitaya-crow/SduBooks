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
    private static final String BASE_URL = "http://10.27.241.94:8081/api/auth";
    private static final String LOGIN_URL = BASE_URL + "/login";
    private static final String REGISTER_URL = BASE_URL + "/register";
    private static final String LOGOUT_URL = BASE_URL + "/logout";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    //登录
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

        // 登录 发送网络请求 (在新线程中执行，防止卡死界面)
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
                    handleResponse(response, loginResult -> {
                        if (loginResult.getCode() == 200) {
                            navigateToHomepage();
                        } else {
                            showAlert("登录失败", loginResult.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }
            } catch (IOException e) {
                handleNetworkError(e);
            }
        }).start();
    }

    // 处理响应结果
    private void handleResponse(Response response, ResponseHandler handler) throws IOException{
        if (response.body() != null) {
            String responseBody = response.body().string();
            LoginResponse result = gson.fromJson(responseBody, LoginResponse.class);

            javafx.application.Platform.runLater(() -> handler.onSuccess(result));
        }
    }

    // 处理网络错误
    private void handleNetworkError(IOException e) {
        e.printStackTrace();
        javafx.application.Platform.runLater(() ->
                showAlert("网络错误", "无法连接到服务器，请检查后端是否启动", Alert.AlertType.ERROR)
        );
    }

    //注册
    @FXML
    protected void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 简单的前端校验
        if (username.trim().isEmpty() || password.isEmpty()) {
            showAlert("提示", "注册时用户名和密码不能为空", Alert.AlertType.WARNING);
            return;
        }

        new Thread(() -> {
            try {
                // 构建 JSON: {"userName": "...", "password": "..."}
                String jsonInput = gson.toJson(new UserRequest(username, password));
                RequestBody body = RequestBody.create(jsonInput, MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(REGISTER_URL) // 使用注册URL
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    handleResponse(response, registerResult -> {
                        if (registerResult.getCode() == 200) {
                            showAlert("注册成功", "账号注册成功，请登录", Alert.AlertType.INFORMATION);

                        } else {
                            showAlert("注册失败", registerResult.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }
            } catch (IOException e) {
                handleNetworkError(e);
            }
        }).start();
    }

    // 退出登录
    @FXML
    protected void handleLogout() {
        new Thread(() -> {
            try {
                // 如果后端不需要 Body，可以传空的 JSON 或者直接 POST
                RequestBody body = RequestBody.create("", MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(LOGOUT_URL)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    handleResponse(response, logoutResult -> {
                        if (logoutResult.getCode() == 200) {
                            showAlert("已退出", "成功退出登录", Alert.AlertType.INFORMATION);
                            // 退出后返回登录页
                            navigateToLogin();
                        } else {
                            showAlert("退出失败", logoutResult.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }
            } catch (IOException e) {
                handleNetworkError(e);
            }
        }).start();
    }

    private void navigateToLogin() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/client-login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 625);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("图书管理系统");
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "页面加载失败", Alert.AlertType.ERROR);
        }
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

//    @FXML
//    protected void handleBackToAdmin() {
//
//        try {
//            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/admin-login-view.fxml"));
//            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
//            Stage stage = (Stage) usernameField.getScene().getWindow();
//            stage.setScene(scene);
//            stage.setTitle("管理员登录");
//        } catch (IOException e) {
//            e.printStackTrace();
//            showAlert("错误", "无法加载管理员登录页面", Alert.AlertType.ERROR);
//        }
//    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface ResponseHandler {
        void onSuccess(LoginResponse result);
    }
}
