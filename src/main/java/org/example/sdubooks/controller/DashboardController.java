package org.example.sdubooks.controller;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
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

    private static final String BASE_URL = "http://10.27.241.94:8081/api/admin";
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
                DashboardStats stats = gson.fromJson(response.body().string(), DashboardStats.class);
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
                List<RecentBorrow> borrows = gson.fromJson(
                        response.body().string(),
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
            HBox activityItem = new HBox(10);
            activityItem.setStyle("-fx-padding: 12px 0; -fx-border-bottom: 1px solid #f0f0f0;");

            Circle statusDot = new Circle(8);
            statusDot.setFill(borrow.getBookName().contains("借阅") ?
                    javafx.scene.paint.Color.web("#4CAF50") :
                    javafx.scene.paint.Color.web("#FF9800"));

            Text activityText = new Text(
                    borrow.getBookName() + " " +
                            borrow.getUserName() + " " +
                            (borrow.getBookName().contains("借阅") ? "借阅了" : "归还了")
            );
            activityText.setStyle("-fx-font-size: 14px; -fx-fill: #333;");

            Text timeText = new Text(
                    borrow.getTimestamp().format(formatter) + "前"
            );
            timeText.setStyle("-fx-font-size: 12px; -fx-fill: #999; -fx-text-alignment: right;");

            HBox.setHgrow(activityText, Priority.ALWAYS);
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
                List<HotBook> books = gson.fromJson(
                        response.body().string(),
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
            HBox bookItem = new HBox(10);
            bookItem.setStyle("-fx-padding: 8px 0;");

            Label rank = new Label(String.valueOf(i + 1));
            rank.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #5a3cff; -fx-min-width: 24px;");

            VBox content = new VBox(4);
            content.setStyle("-fx-alignment: CENTER_LEFT;");

            Label title = new Label(book.getTitle());
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            Label author = new Label(book.getAuthor());
            author.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            content.getChildren().addAll(title, author);

            Label count = new Label(book.getBorrowCount() + "次");
            count.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");

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
                List<ActiveUser> users = gson.fromJson(
                        response.body().string(),
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
            HBox userItem = new HBox(10);
            userItem.setStyle("-fx-padding: 8px 0;");

            Label rank = new Label(String.valueOf(i + 1));
            rank.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #5a3cff; -fx-min-width: 24px;");

            VBox content = new VBox(4);
            content.setStyle("-fx-alignment: CENTER_LEFT;");

            Label name = new Label(user.getName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            Label email = new Label(user.getEmail());
            email.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            content.getChildren().addAll(name, email);

            Label count = new Label(user.getBorrowCount() + "本");
            count.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");

            userItem.getChildren().addAll(rank, content, count);
            activeUsersBox.getChildren().add(userItem);
        }
    }
}