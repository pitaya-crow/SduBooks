package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.sdubooks.model.BorrowRecord;
import org.example.sdubooks.model.BookReview;
import org.example.sdubooks.model.UserStats;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class SelfController extends BaseController {

    @FXML private Label usernameLabel;
    @FXML private Label joinDateLabel;
    @FXML private Label totalBorrowsLabel;
    @FXML private Label currentBorrowsLabel;
    @FXML private Button btnBorrowRecords;
    @FXML private Button btnReviews;
    @FXML private VBox contentArea;

    private String currentTab = "borrows";

    private static final String USER_BASE_URL = "http://localhost:8081/api/user";
    private static final String STATS_URL = USER_BASE_URL + "/info";
    private static final String BORROWS_URL = USER_BASE_URL + "/borrows";
    private static final String REVIEWS_URL = USER_BASE_URL + "/reviews";

    @FXML
    public void initialize() {
        loadUserStats();
        loadBorrowRecords();
    }

    private String extractData(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return "{}";
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json != null && json.has("data") && !json.get("data").isJsonNull()) {
                return json.get("data").toString();
            }
        } catch (Exception ignored) {}
        return responseBody;
    }

    @FXML
    private void handleSwitchTab(javafx.event.ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        String tab = (String) clickedBtn.getUserData();

        if (currentTab.equals(tab)) {
            return;
        }

        currentTab = tab;
        updateButtonStyles(clickedBtn);

        if ("borrows".equals(tab)) {
            loadBorrowRecords();
        } else {
            loadReviews();
        }
    }

    private void updateButtonStyles(Button activeBtn) {
        String activeStyle = "-fx-background-color: #ffffff; -fx-background-radius: 25; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 10 30; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-font-size: 16px; -fx-text-fill: #64748b; -fx-padding: 10 30; -fx-cursor: hand;";

        btnBorrowRecords.setStyle(inactiveStyle);
        btnReviews.setStyle(inactiveStyle);

        activeBtn.setStyle(activeStyle);
    }

    private UserStats currentUserStats;

    private Long getCurrentUserId() {
        return BaseController.getUserId();
    }

    private void loadUserStats() {
        String token = getToken();
        Long userId = getCurrentUserId();
        if (userId == null) return;

        Request request = new Request.Builder()
                .url(STATS_URL + "?userId=" + userId)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 用户信息响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    UserStats stats = gson.fromJson(extractData(respBody), UserStats.class);
                    if (stats != null) {
                        currentUserStats = stats;
                        Platform.runLater(() -> {
                            usernameLabel.setText(stats.getUserName() != null ? stats.getUserName() : "");
                            joinDateLabel.setText("加入时间：" + (stats.getCreateTime() != null ? stats.getCreateTime() : "未知"));
                            totalBorrowsLabel.setText(String.valueOf(stats.getTotalBorrows() != null ? stats.getTotalBorrows() : 0));
                            currentBorrowsLabel.setText(String.valueOf(stats.getCurrentBorrows() != null ? stats.getCurrentBorrows() : 0));
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "加载用户信息失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    @FXML
    private void handleEditProfile() {
        if (currentUserStats == null) {
            showAlert("提示", "用户信息未加载，请稍后重试", Alert.AlertType.WARNING);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("编辑个人资料");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(400);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new javafx.geometry.Insets(20));

        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setMinWidth(60);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #e2e8f0; -fx-padding: 8;";
        String labelStyle = "-fx-font-size: 13px; -fx-text-fill: #475569; -fx-padding: 8 0;";

        TextField usernameField = new TextField(currentUserStats.getUserName() != null ? currentUserStats.getUserName() : "");
        usernameField.setStyle(fieldStyle);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("不修改请留空");
        passwordField.setStyle(fieldStyle);

        int row = 0;
        Label lbl1 = new Label("用户名:");
        lbl1.setStyle(labelStyle);
        grid.add(lbl1, 0, row);
        grid.add(usernameField, 1, row++);

        Label lbl3 = new Label("新密码:");
        lbl3.setStyle(labelStyle);
        grid.add(lbl3, 0, row);
        grid.add(passwordField, 1, row++);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String newUsername = usernameField.getText().trim();
                String newPassword = passwordField.getText();

                if (newUsername.isEmpty()) {
                    showAlert("提示", "用户名不能为空", Alert.AlertType.WARNING);
                    return null;
                }

                updateProfile(newUsername, newPassword);
            }
            return btn;
        });

        dialog.showAndWait();
    }

    private void updateProfile(String username, String password) {
        String token = getToken();
        Long userId = getCurrentUserId();
        if (token == null || userId == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("personId", userId.intValue());
        body.addProperty("userName", username);
        if (password != null && !password.isEmpty()) {
            body.addProperty("password", password);
        }

        String json = body.toString();
        System.out.println("[DEBUG] 更新用户信息: " + json);

        RequestBody reqBody = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(USER_BASE_URL + "/info")
                .header("Authorization", "Bearer " + token)
                .put(reqBody)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 更新用户信息响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", "个人资料已更新", Alert.AlertType.INFORMATION);
                        loadUserStats(); // 刷新数据
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("失败", "更新失败: " + respBody, Alert.AlertType.ERROR)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络异常: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void loadBorrowRecords() {
        String token = getToken();
        Long userId = getCurrentUserId();
        if (userId == null) return;

        Request request = new Request.Builder()
                .url(BORROWS_URL + "?userId=" + userId)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 借阅记录响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    List<BorrowRecord> records = gson.fromJson(
                            extractData(respBody),
                            new TypeToken<List<BorrowRecord>>(){}.getType()
                    );
                    Platform.runLater(() -> renderBorrowRecords(records != null ? records : new java.util.ArrayList<>()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "加载借阅记录失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void renderBorrowRecords(List<BorrowRecord> records) {
        contentArea.getChildren().clear();

        for (BorrowRecord record : records) {
            contentArea.getChildren().add(createBorrowRecordItem(record));
        }
    }

    private VBox createBorrowRecordItem(BorrowRecord record) {
        VBox item = new VBox(15);
        item.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        HBox content = new HBox(20);
        content.setStyle("-fx-alignment: CENTER_LEFT;");

        // 封面
        VBox coverBox = new VBox();
        coverBox.setStyle("-fx-min-width: 100px; -fx-min-height: 130px; " +
                "-fx-background-color: #e2e8f0; -fx-background-radius: 10; " +
                "-fx-alignment: CENTER;");
        Label coverLabel = new Label("");
        coverLabel.setStyle("-fx-font-size: 40px;");
        coverBox.getChildren().add(coverLabel);

        // 书籍信息
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-alignment: CENTER_LEFT;");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(record.getBookTitle() != null ? record.getBookTitle() : "图书");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // 日期信息
        String borrowDateStr = record.getBorrowedAt() != null ? record.getBorrowedAt().toLocalDate().toString() : "";
        String dueDateStr = record.getDueAt() != null ? record.getDueAt().toLocalDate().toString() : "";
        Label dateLabel = new Label("借阅: " + borrowDateStr + "  应还: " + dueDateStr);
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // 状态标签
        Label statusLabel = new Label();
        boolean returned = record.isReturned();
        if (returned) {
            statusLabel.setText("已归还");
            statusLabel.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 15; " +
                    "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            // 检查是否逾期
            boolean overdue = false;
            if (record.getDueAt() != null && java.time.LocalDateTime.now().isAfter(record.getDueAt())) {
                overdue = true;
            }
            if (overdue) {
                statusLabel.setText("已逾期");
                statusLabel.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 15; " +
                        "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("借阅中");
                statusLabel.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 15; " +
                        "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        }

        infoBox.getChildren().addAll(titleLabel, dateLabel, statusLabel);

        // 右侧操作区
        VBox actionBox = new VBox(10);
        actionBox.setStyle("-fx-alignment: CENTER_RIGHT;");

        if (!returned) {
            // 检查逾期
            boolean overdue = false;
            long overdueDays = 0;
            if (record.getDueAt() != null && LocalDateTime.now().isAfter(record.getDueAt())) {
                overdue = true;
                overdueDays = ChronoUnit.DAYS.between(record.getDueAt(), LocalDateTime.now());
            }

            if (overdue) {
                Label overdueInfo = new Label("逾期 " + overdueDays + " 天");
                overdueInfo.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                actionBox.getChildren().add(overdueInfo);
            }

            Button returnBtn = new Button(overdue ? "立即归还" : "归还图书");
            returnBtn.setStyle(overdue ?
                    "-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:10;-fx-padding:8 20;" :
                    "-fx-background-color:white;-fx-border-color:#cbd5e1;-fx-border-radius:10;-fx-padding:8 20;");
            returnBtn.setOnAction(e -> handleReturnBook(record));
            actionBox.getChildren().add(returnBtn);
        } else {
            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #22c55e;");
            actionBox.getChildren().add(checkIcon);
            if (record.getReturnedAt() != null) {
                Label returnDateLabel = new Label("归还: " + record.getReturnedAt().toLocalDate());
                returnDateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
                actionBox.getChildren().add(returnDateLabel);
            }
        }

        content.getChildren().addAll(coverBox, infoBox, actionBox);
        item.getChildren().add(content);

        return item;
    }

    private void handleReturnBook(BorrowRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认归还");
        confirm.setHeaderText(null);
        confirm.setContentText("确定要归还《" + record.getBookTitle() + "》吗？");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performReturnBook(record);
            }
        });
    }

    private void performReturnBook(BorrowRecord record) {
        String token = getToken();
        String url = BASE_URL + "/borrow/return";

        JsonObject body = new JsonObject();
        body.addProperty("id", record.getId());

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", "归还图书成功", Alert.AlertType.INFORMATION);
                        loadBorrowRecords();
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("错误", "归还图书失败", Alert.AlertType.ERROR)
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败", Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void loadReviews() {
        String token = getToken();
        Long userId = getCurrentUserId();
        if (userId == null) return;

        Request request = new Request.Builder()
                .url(REVIEWS_URL + "?userId=" + userId)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 书评响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    List<BookReview> reviews = gson.fromJson(
                            extractData(respBody),
                            new TypeToken<List<BookReview>>(){}.getType()
                    );
                    Platform.runLater(() -> renderReviews(reviews != null ? reviews : new java.util.ArrayList<>()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "加载书评失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void renderReviews(List<BookReview> reviews) {
        contentArea.getChildren().clear();

        for (BookReview review : reviews) {
            contentArea.getChildren().add(createReviewItem(review));
        }
    }

    private VBox createReviewItem(BookReview review) {
        VBox item = new VBox(15);
        item.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        // 头部信息
        HBox header = new HBox();
        header.setStyle("-fx-alignment: CENTER_LEFT;");

        Label titleLabel = new Label("评论了图书");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(review.getCreateTime() != null ? review.getCreateTime().toLocalDate().toString() : "");
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        header.getChildren().addAll(titleLabel, spacer, dateLabel);

        // 图书链接
        Hyperlink bookLink = new Hyperlink("查看图书详情");
        bookLink.setStyle("-fx-font-size: 14px;");
        bookLink.setOnAction(e -> viewBookDetail(review.getBookId() != null ? review.getBookId().longValue() : null));

        // 评分
        HBox ratingBox = new HBox(5);
        ratingBox.setStyle("-fx-alignment: CENTER_LEFT;");

        int rating = review.getRating() != null ? review.getRating().intValue() : 0;
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < rating ? "★" : "☆");
            star.setStyle("-fx-font-size: 20px; -fx-text-fill: " + (i < rating ? "#fbbf24" : "#cbd5e1") + ";");
            ratingBox.getChildren().add(star);
        }

        Label ratingText = new Label(rating + " 分");
        ratingText.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        ratingBox.getChildren().add(ratingText);

        // 评论内容
        Label contentLabel = new Label(review.getContent() != null ? review.getContent() : "");
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-line-spacing: 5;");
        contentLabel.setWrapText(true);

        item.getChildren().addAll(header, bookLink, ratingBox, contentLabel);

        return item;
    }

    private void viewBookDetail(Long bookId) {
        System.out.println("查看图书详情，图书ID: " + bookId);
        BookDetailController.setSelectedBookId(bookId);
        loadPageInShell("/org/example/sdubooks/book-detail.fxml");
    }

    @Override
    protected Stage getCurrentStage() {
        if (contentArea != null && contentArea.getScene() != null) {
            return (Stage) contentArea.getScene().getWindow();
        }
        return null;
    }
}
