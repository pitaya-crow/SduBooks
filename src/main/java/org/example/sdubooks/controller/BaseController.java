package org.example.sdubooks.controller;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import okhttp3.*;
import org.example.sdubooks.model.LoginResponse;

import java.io.IOException;
import java.util.prefs.Preferences;

public class BaseController {

    @FXML
    private HBox navigationBar;

    static final String BASE_URL = "http://localhost:8081/api";
    private static final String LOGOUT_URL = BASE_URL + "/auth/logout";
    // 使用 Java Preferences API 作为全局 Token 存储，避免跨 Controller 传参
    protected static final Preferences prefs = Preferences.userNodeForPackage(BaseController.class);
    protected static final String TOKEN_KEY = "auth_token";

    protected final OkHttpClient httpClient = new OkHttpClient();
    protected final Gson gson = new Gson();

    // ==================== 退出登录逻辑 ====================

    @FXML
    protected void handleLogout() {
        System.out.println("点击了退出登录按钮");

        String token = prefs.get(TOKEN_KEY, null);
        if (token == null || token.isEmpty()) {
            // 本地无 Token，直接跳回登录页
            navigateToLogin(null);
            return;
        }

        // 异步调用退出接口，避免阻塞 UI
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(LOGOUT_URL)
                        .post(RequestBody.create("", MediaType.get("application/json")))
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    // 无论后端返回成功与否，都清除本地 Token 并跳转登录页
                    prefs.remove(TOKEN_KEY);
                    javafx.application.Platform.runLater(() -> navigateToLogin(null));
                }
            } catch (IOException e) {
                e.printStackTrace();
                // 网络异常时同样清除 Token，防止脏数据残留
                prefs.remove(TOKEN_KEY);
                javafx.application.Platform.runLater(() -> navigateToLogin("退出请求失败，已强制退出"));
            }
        }).start();
    }

    // ==================== 页面跳转 ====================
    @FXML
    protected void handleHome() {
        System.out.println("点击了首页按钮");
        navigateTo("/org/example/sdubooks/homepage-view.fxml", "首页", 800, 600);
    }

    @FXML
    protected void handleRankings() {
        System.out.println("点击了排行榜按钮");
        navigateTo("/org/example/sdubooks/ranking.fxml", "排行榜", 450, 650);
    }

    @FXML
    protected void handleProfile() {
        System.out.println("点击了个人中心按钮");
        navigateTo("/org/example/sdubooks/self-center.fxml", "个人中心", 450, 650);
    }

    /**
     * 通用页面跳转方法，彻底替代原先通过 namespace 获取 Stage 的危险写法
     */
    protected void navigateTo(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), width, height);
            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.setTitle(title);
                stage.setMaximized(false);
                stage.setMaximized(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "页面加载失败: " + fxmlPath, Alert.AlertType.ERROR);
        }
    }

    private void navigateToLogin(String warningMsg) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/login-view.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.setTitle("用户登录");
                stage.setMaximized(false);
                stage.sizeToScene();
                if (warningMsg != null) {
                    showAlert("提示", warningMsg, Alert.AlertType.WARNING);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("严重错误", "无法加载登录页面", Alert.AlertType.ERROR);
        }
    }

    // ==================== 通用工具方法 ====================

    /**
     * 安全获取当前 Stage，避免 NullPointerException
     * 子类如有特定 UI 组件可重写此方法提供更可靠的引用
     */
    protected Stage getCurrentStage() {
        if (navigationBar != null && navigationBar.getScene() != null) {
            return (Stage) navigationBar.getScene().getWindow();
        }
        return null;
    }

    protected void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 保存 Token（供 LoginController 登录成功后调用）
     */
    public static void saveToken(String token) {
        prefs.put(TOKEN_KEY, token);
    }

    /**
     * 获取 Token（供其他需要鉴权的 Controller 调用）
     */
    public static String getToken() {
        return prefs.get(TOKEN_KEY, null);
    }
}