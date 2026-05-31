package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.sdubooks.model.*;
import okhttp3.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController extends BaseController {

    // ===== 统计卡片 =====
    @FXML private Label totalBooksLabel;
    @FXML private Label borrowedBooksLabel;
    @FXML private Label registeredUsersLabel;
    @FXML private Label overdueBooksLabel;

    // ===== 最近活动 =====
    @FXML private VBox recentActivitiesBox;

    // ===== 热门图书 =====
    @FXML private VBox hotBooksBox;

    // ===== 活跃用户 =====
    @FXML private VBox activeUsersBox;

    private static final String BASE_URL = "http://localhost:8081/api/admin";
    // 接口路径（保持与文档一致）
    private static final String STATS_URL = BASE_URL + "/dashboard/stats";
    private static final String RECENT_URL = BASE_URL + "/dashboard/recent";
    private static final String HOT_BOOKS_URL = BASE_URL + "/dashboard/hot-books";
    private static final String ACTIVE_USERS_URL = BASE_URL + "/dashboard/active-users";

    @FXML
    public void initialize() {
        // 加载所有数据
        loadDashboardData();
    }

    private void loadDashboardData() {
        // 同时发起4个请求（使用线程池优化）
        new Thread(() -> {
            try {
                loadStats();
                loadRecentBorrows();
                loadHotBooks();
                loadActiveUsers();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("数据加载错误", "无法获取仪表板数据，请检查网络连接", Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    /**
     * 从包装响应中提取 data 字段的 JSON 字符串
     * 后端返回 {"code":200, "data":{...}} 格式
     */
    private String extractData(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json.has("data")) {
                return json.get("data").toString();
            }
        } catch (Exception ignored) {}
        return responseBody; // 如果不是包装格式，直接返回原内容
    }

    // 加载统计卡片数据
    private void loadStats() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(STATS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String respBody = response.body().string();
                DashboardStats stats = gson.fromJson(extractData(respBody), DashboardStats.class);
                Platform.runLater(() -> {
                    totalBooksLabel.setText(String.valueOf(stats.getTotalBooks()));
                    borrowedBooksLabel.setText(String.valueOf(stats.getBorrowedBooks()));
                    registeredUsersLabel.setText(String.valueOf(stats.getRegisteredUsers()));
                    overdueBooksLabel.setText(String.valueOf(stats.getOverdueBooks()));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载最近活动
    private void loadRecentBorrows() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(RECENT_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String respBody = response.body().string();
                List<RecentBorrow> borrows = gson.fromJson(
                        extractData(respBody),
                        new TypeToken<List<RecentBorrow>>(){}.getType()
                );
                Platform.runLater(() -> renderRecentActivities(borrows));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderRecentActivities(List<RecentBorrow> borrows) {
        recentActivitiesBox.getChildren().clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (RecentBorrow borrow : borrows) {
            HBox activityItem = new HBox(12);
            activityItem.setStyle("-fx-padding: 10 0; -fx-alignment: CENTER_LEFT; -fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            Circle statusDot = new Circle(5);
            statusDot.setFill(javafx.scene.paint.Color.web("#22c55e"));

            Label activityText = new Label(
                    borrow.getUserName() + " " +
                            (borrow.getBookName().contains("借阅") ? "借阅了" : "归还了") + " " +
                            borrow.getBookName()
            );
            activityText.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
            HBox.setHgrow(activityText, Priority.ALWAYS);

            Label timeText = new Label(borrow.getTimestamp().format(formatter));
            timeText.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

            activityItem.getChildren().addAll(statusDot, activityText, timeText);
            recentActivitiesBox.getChildren().add(activityItem);
        }
    }

    // 加载热门图书
    private void loadHotBooks() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(HOT_BOOKS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String respBody = response.body().string();
                List<HotBook> books = gson.fromJson(
                        extractData(respBody),
                        new TypeToken<List<HotBook>>(){}.getType()
                );
                Platform.runLater(() -> renderHotBooks(books));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderHotBooks(List<HotBook> books) {
        hotBooksBox.getChildren().clear();
        for (int i = 0; i < Math.min(books.size(), 5); i++) {
            HotBook book = books.get(i);
            HBox bookItem = new HBox(12);
            bookItem.setStyle("-fx-padding: 10 8; -fx-alignment: CENTER_LEFT; -fx-background-color: #f8fafc; -fx-background-radius: 8;");

            Label rank = new Label(String.valueOf(i + 1));
            String rankColor = i == 0 ? "#f59e0b" : (i == 1 ? "#94a3b8" : (i == 2 ? "#cd7f32" : "#cbd5e1"));
            rank.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: " + rankColor + "; -fx-min-width: 28px; -fx-alignment: CENTER;");

            VBox content = new VBox(2);
            content.setStyle("-fx-alignment: CENTER_LEFT;");
            HBox.setHgrow(content, Priority.ALWAYS);

            Label title = new Label(book.getTitle());
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label author = new Label(book.getAuthor());
            author.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            content.getChildren().addAll(title, author);

            Label count = new Label(book.getBorrowCount() + " 次");
            count.setStyle("-fx-font-size: 14px; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

            bookItem.getChildren().addAll(rank, content, count);
            hotBooksBox.getChildren().add(bookItem);
        }
    }

    // 加载活跃用户
    private void loadActiveUsers() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(ACTIVE_USERS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String respBody = response.body().string();
                List<ActiveUser> users = gson.fromJson(
                        extractData(respBody),
                        new TypeToken<List<ActiveUser>>(){}.getType()
                );
                Platform.runLater(() -> renderActiveUsers(users));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderActiveUsers(List<ActiveUser> users) {
        activeUsersBox.getChildren().clear();
        for (int i = 0; i < Math.min(users.size(), 4); i++) {
            ActiveUser user = users.get(i);
            HBox userItem = new HBox(12);
            userItem.setStyle("-fx-padding: 10 8; -fx-alignment: CENTER_LEFT; -fx-background-color: #f8fafc; -fx-background-radius: 8;");

            // 头像圆圈
            javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
            avatar.setStyle("-fx-background-color: #ede9fe; -fx-background-radius: 16; -fx-min-width: 32; -fx-max-width: 32; -fx-min-height: 32; -fx-max-height: 32;");
            Label avatarText = new Label(user.getName() != null && !user.getName().isEmpty() ?
                    user.getName().substring(0, 1).toUpperCase() : "?");
            avatarText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");
            avatar.getChildren().add(avatarText);

            VBox content = new VBox(2);
            content.setStyle("-fx-alignment: CENTER_LEFT;");
            HBox.setHgrow(content, Priority.ALWAYS);

            Label name = new Label(user.getName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label email = new Label(user.getEmail());
            email.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            content.getChildren().addAll(name, email);

            Label count = new Label(user.getBorrowCount() + " 本");
            count.setStyle("-fx-font-size: 14px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");

            userItem.getChildren().addAll(avatar, content, count);
            activeUsersBox.getChildren().add(userItem);
        }
    }
}