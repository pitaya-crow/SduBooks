package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    private List<RecentBorrow> allRecentBorrows = new java.util.ArrayList<>();
    private int recentPage = 0;
    private static final int RECENT_PAGE_SIZE = 3;

    // ===== 热门图书 =====
    @FXML private VBox hotBooksBox;

    // ===== 活跃用户 =====
    @FXML private VBox activeUsersBox;

    private static final String BASE_URL = "http://localhost:8081/api/admin";
    // 接口路径（保持与文档一致）
    private static final String STATS_URL = BASE_URL + "/dashboard/stats";
    private static final String RECENT_URL = BASE_URL + "/dashboard/recent";
    private static final String HOT_BOOKS_URL = BASE_URL + "/dashboard/hot-books";
    private static final String ACTIVE_USERS_URL = BASE_URL + "/dashboards/active-users";

    @FXML
    public void initialize() {
        // 加载所有数据
        loadDashboardData();
    }

    private void loadDashboardData() {
        // 每个请求独立 try-catch，一个失败不影响其他
        new Thread(() -> {
            try { loadStats(); } catch (Exception e) { System.err.println("[DEBUG] loadStats失败: " + e.getMessage()); }
            try { loadRecentBorrows(); } catch (Exception e) { System.err.println("[DEBUG] loadRecentBorrows失败: " + e.getMessage()); }
            try { loadHotBooks(); } catch (Exception e) { System.err.println("[DEBUG] loadHotBooks失败: " + e.getMessage()); }
            try { loadActiveUsers(); } catch (Exception e) { System.err.println("[DEBUG] loadActiveUsers失败: " + e.getMessage()); }
        }).start();
    }

    /**
     * 从包装响应中提取 data 字段的 JSON 字符串
     * 后端返回 {"code":200, "data":{...}} 格式
     */
    private String extractData(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return "{}";
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json != null && json.has("data") && !json.get("data").isJsonNull()) {
                return json.get("data").toString();
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] extractData解析失败: " + e.getMessage());
        }
        return responseBody;
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
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[DEBUG] 仪表板stats响应: code=" + response.code() + " body=" + respBody);
            if (response.isSuccessful()) {
                DashboardStats stats = gson.fromJson(extractData(respBody), DashboardStats.class);
                if (stats != null) {
                    Platform.runLater(() -> {
                        totalBooksLabel.setText(String.valueOf(stats.getBookCount()));
                        borrowedBooksLabel.setText(String.valueOf(stats.getBorrowRecordCount()));
                        registeredUsersLabel.setText(String.valueOf(stats.getUserCount()));
                        overdueBooksLabel.setText(String.valueOf(stats.getOverdueCount()));
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] loadStats异常: " + e.getMessage());
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
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[DEBUG] 仪表板recent响应: code=" + response.code() + " body=" + respBody);
            if (response.isSuccessful()) {
                List<RecentBorrow> borrows = gson.fromJson(
                        extractData(respBody),
                        new TypeToken<List<RecentBorrow>>(){}.getType()
                );
                if (borrows == null) borrows = new java.util.ArrayList<>();
                final List<RecentBorrow> finalBorrows = borrows;
                Platform.runLater(() -> {
                    allRecentBorrows = finalBorrows;
                    recentPage = 0;
                    renderRecentActivities();
                });
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] loadRecentBorrows异常: " + e.getMessage());
        }
    }

    private void renderRecentActivities() {
        recentActivitiesBox.getChildren().clear();
        if (allRecentBorrows == null || allRecentBorrows.isEmpty()) {
            recentActivitiesBox.getChildren().add(new Label("暂无活动记录"));
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        int totalSize = allRecentBorrows.size();
        int totalPages = Math.max((int) Math.ceil((double) totalSize / RECENT_PAGE_SIZE), 1);
        if (recentPage >= totalPages) recentPage = totalPages - 1;
        if (recentPage < 0) recentPage = 0;

        int from = recentPage * RECENT_PAGE_SIZE;
        int to = Math.min(from + RECENT_PAGE_SIZE, totalSize);
        List<RecentBorrow> pageItems = allRecentBorrows.subList(from, to);

        for (RecentBorrow borrow : pageItems) {
            HBox activityItem = new HBox(12);
            activityItem.setStyle("-fx-padding: 10 0; -fx-alignment: CENTER_LEFT; -fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            Circle statusDot = new Circle(5);
            statusDot.setFill(javafx.scene.paint.Color.web("#22c55e"));

            String bookName = borrow.getBookName() != null ? borrow.getBookName() : "";
            String userName = borrow.getUserName() != null ? borrow.getUserName() : "";
            Label activityText = new Label(userName + " 借阅了 " + bookName);
            activityText.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
            HBox.setHgrow(activityText, Priority.ALWAYS);

            Label timeText = new Label(borrow.getTimestamp() != null ? borrow.getTimestamp().format(formatter) : "");
            timeText.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

            activityItem.getChildren().addAll(statusDot, activityText, timeText);
            recentActivitiesBox.getChildren().add(activityItem);
        }

        // 分页按钮
        if (totalPages > 1) {
            HBox pagingBox = new HBox(10);
            pagingBox.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 8 0 0 0;");

            Button prevBtn = new Button("◀");
            prevBtn.setDisable(recentPage == 0);
            prevBtn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6; -fx-cursor: hand;");
            prevBtn.setOnAction(e -> { recentPage--; renderRecentActivities(); });

            Label pageLabel = new Label((recentPage + 1) + "/" + totalPages);
            pageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Button nextBtn = new Button("▶");
            nextBtn.setDisable(recentPage >= totalPages - 1);
            nextBtn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6; -fx-cursor: hand;");
            nextBtn.setOnAction(e -> { recentPage++; renderRecentActivities(); });

            pagingBox.getChildren().addAll(prevBtn, pageLabel, nextBtn);
            recentActivitiesBox.getChildren().add(pagingBox);
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
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[DEBUG] 仪表板hotBooks响应: code=" + response.code() + " body=" + respBody);
            if (response.isSuccessful()) {
                List<HotBook> books = gson.fromJson(
                        extractData(respBody),
                        new TypeToken<List<HotBook>>(){}.getType()
                );
                Platform.runLater(() -> renderHotBooks(books));
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] loadHotBooks异常: " + e.getMessage());
        }
    }

    private void renderHotBooks(List<HotBook> books) {
        hotBooksBox.getChildren().clear();
        if (books == null) return;
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
            String respBody = response.body() != null ? response.body().string() : "";
            System.out.println("[DEBUG] 仪表板activeUsers响应: code=" + response.code() + " body=" + respBody);
            if (response.isSuccessful()) {
                List<ActiveUser> users = gson.fromJson(
                        extractData(respBody),
                        new TypeToken<List<ActiveUser>>(){}.getType()
                );
                Platform.runLater(() -> renderActiveUsers(users));
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] loadActiveUsers异常: " + e.getMessage());
        }
    }

    private void renderActiveUsers(List<ActiveUser> users) {
        activeUsersBox.getChildren().clear();
        if (users == null || users.isEmpty()) {
            activeUsersBox.getChildren().add(new Label("暂无数据"));
            return;
        }
        for (int i = 0; i < Math.min(users.size(), 4); i++) {
            ActiveUser user = users.get(i);
            HBox userItem = new HBox(12);
            userItem.setStyle("-fx-padding: 10 8; -fx-alignment: CENTER_LEFT; -fx-background-color: #f8fafc; -fx-background-radius: 8;");

            // 头像圆圈
            javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
            avatar.setStyle("-fx-background-color: #ede9fe; -fx-background-radius: 16; -fx-min-width: 32; -fx-max-width: 32; -fx-min-height: 32; -fx-max-height: 32;");
            String userName = user.getName();
            Label avatarText = new Label(userName != null && !userName.isEmpty() ?
                    userName.substring(0, 1).toUpperCase() : "?");
            avatarText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");
            avatar.getChildren().add(avatarText);

            VBox content = new VBox(2);
            content.setStyle("-fx-alignment: CENTER_LEFT;");
            HBox.setHgrow(content, Priority.ALWAYS);

            Label name = new Label(userName);
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            String role = (user.getUserTypeId() != null && user.getUserTypeId() == 2) ? "管理员" : "普通用户";
            Label roleLabel = new Label(role);
            roleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            content.getChildren().addAll(name, roleLabel);

            userItem.getChildren().addAll(avatar, content);
            activeUsersBox.getChildren().add(userItem);
        }
    }
}