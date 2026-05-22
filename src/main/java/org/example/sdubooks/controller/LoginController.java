package org.example.sdubooks.controller;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import okhttp3.*;
import org.example.sdubooks.model.LoginResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginController {

    // ==================== UI 组件绑定 ====================
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label titleLabel;
    @FXML private Label subTitleLabel;
    @FXML private Button registerBtn;
    @FXML private Hyperlink switchLink;

    // ==================== 状态与配置 ====================
    // true: 管理员模式 | false: 普通用户模式
    private boolean isAdminMode = false;

    private static final String BASE_URL = "http://10.27.241.94:8081/api/auth";
    private static final String LOGIN_URL = BASE_URL + "/login";
    private static final String REGISTER_URL = BASE_URL + "/register";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    // ==================== 初始化与模式切换 ====================

    /**
     * FXML 加载完成后自动调用，默认初始化为普通用户模式
     */
    @FXML
    public void initialize() {
        setAdminMode(false);
    }

    /**
     * 设置当前登录模式，并同步更新所有差异化 UI 状态
     */
    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;

        // 切换模式时清空输入框，防止串号
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();

        // 更新差异化 UI 组件
        if (titleLabel != null) {
            if (isAdmin) {
                titleLabel.setText("管理员登录");
                subTitleLabel.setText("图书管理系统后台");
                registerBtn.setVisible(false);
                registerBtn.setManaged(false);   // 释放布局空间，避免空白断层
                switchLink.setText("返回用户端登录");
            } else {
                titleLabel.setText("用户登录");
                subTitleLabel.setText("图书管理系统");
                registerBtn.setVisible(true);
                registerBtn.setManaged(true);
                switchLink.setText("切换到管理员登录");
            }
        }
    }

    /**
     * 底部超链接点击事件：在两种模式间无缝切换
     */
    @FXML
    protected void toggleLoginMode() {
        setAdminMode(!this.isAdminMode);
    }

    // ==================== 核心业务逻辑 ====================

    /**
     * 统一登录入口：前端校验后根据模式走同一套网络请求
     */
    @FXML
    protected void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            showAlert("提示", "请输入用户名", Alert.AlertType.WARNING);
            return;
        }
        if (password == null || password.isEmpty()) {
            showAlert("提示", "请输入密码", Alert.AlertType.WARNING);
            return;
        }

        // 构建符合后端约定的请求体 Map<String, String>
        Map<String, String> loginData = new HashMap<>();
        loginData.put("userName", username);
        loginData.put("password", password);

        executePostRequest(LOGIN_URL, loginData, result -> {
            if (result.getCode() == 200) {
                // 登录成功，保存 token 到本地
                if (result.getData() != null && result.getData().getToken() != null) {
                    BaseController.saveToken(result.getData().getToken());
                } else {
                    // 防御性处理：后端返回200但未携带Token时给出警告
                    showAlert("警告", "登录成功但未获取到凭证，部分功能可能受限", Alert.AlertType.WARNING);
                }

                // 登录成功，根据后端返回的 role 字段动态跳转
                String role = result.getData() != null ? result.getData().getRole() : null;
                if ("ADMIN".equals(role)) {
                    navigateTo("/org/example/sdubooks/admin-homepage-view.fxml", "管理员后台", false);
                } else {
                    navigateTo("/org/example/sdubooks/homepage-view.fxml", "图书管理系统", true);
                }
            } else {
                showAlert("登录失败", result.getMsg(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * 注册入口：仅在普通用户模式下生效
     */
    @FXML
    protected void handleRegister() {
        if (isAdminMode) {
            showAlert("提示", "管理员界面不提供注册功能", Alert.AlertType.WARNING);
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password == null || password.isEmpty()) {
            showAlert("提示", "用户名和密码不能为空", Alert.AlertType.WARNING);
            return;
        }

        Map<String, String> registerData = new HashMap<>();
        registerData.put("userName", username);
        registerData.put("password", password);

        executePostRequest(REGISTER_URL, registerData, result -> {
            if (result.getCode() == 200) {
                showAlert("注册成功", "账号创建成功，请登录", Alert.AlertType.INFORMATION);
            } else {
                showAlert("注册失败", result.getMsg(), Alert.AlertType.ERROR);
            }
        });
    }

    // ==================== 网络请求封装 ====================

    /**
     * 通用异步 POST 请求方法，避免登录和注册重复编写线程与 OkHttp 代码
     */
    private void executePostRequest(String url, Map<String, String> data, ResponseHandler handler) {
        new Thread(() -> {
            try {
                String jsonBody = gson.toJson(data);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder().url(url).post(body).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.body() != null) {
                        String responseBody = response.body().string();
                        LoginResponse result = gson.fromJson(responseBody, LoginResponse.class);
                        // 切回 JavaFX 主线程更新 UI
                        javafx.application.Platform.runLater(() -> handler.onSuccess(result));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        showAlert("网络错误", "无法连接到服务器，请检查后端是否启动", Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    // ==================== 通用工具方法 ====================

    /**
     * 统一页面跳转方法
     * @param fxmlPath  目标 FXML 路径
     * @param title     窗口标题
     * @param maximize  是否最大化窗口
     */
    private void navigateTo(String fxmlPath, String title, boolean maximize) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(maximize);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "页面加载失败: " + fxmlPath, Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 响应处理函数式接口，配合 Lambda 简化回调逻辑
     */
    @FunctionalInterface
    private interface ResponseHandler {
        void onSuccess(LoginResponse result);
    }
}