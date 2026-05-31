package org.example.sdubooks.controller;

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
import java.time.temporal.ChronoUnit;
import java.util.List;

public class SelfController extends BaseController {

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
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

    private void loadUserStats() {
        String token = getToken();
        Request request = new Request.Builder()
                .url(STATS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    UserStats stats = gson.fromJson(response.body().string(), UserStats.class);
                    Platform.runLater(() -> {
                        usernameLabel.setText(stats.getUsername());
                        emailLabel.setText(stats.getEmail());
                        joinDateLabel.setText("加入时间：" + stats.getJoinDate());
                        totalBorrowsLabel.setText(String.valueOf(stats.getTotalBorrows()));
                        currentBorrowsLabel.setText(String.valueOf(stats.getCurrentBorrows()));
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "加载用户信息失败", Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void loadBorrowRecords() {
        String token = getToken();
        Request request = new Request.Builder()
                .url(BORROWS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BorrowRecord> records = gson.fromJson(
                            response.body().string(),
                            new TypeToken<List<BorrowRecord>>(){}.getType()
                    );
                    Platform.runLater(() -> renderBorrowRecords(records));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "加载借阅记录失败", Alert.AlertType.ERROR)
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

        Label titleLabel = new Label(record.getBookTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // 状态标签
        Label statusLabel = new Label();
        String status = record.getStatus();
        if ("BORROWING".equals(status) || "借阅中".equals(status)) {
            statusLabel.setText("借阅中");
            statusLabel.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 15; " +
                    "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        } else if ("RETURNED".equals(status) || "已归还".equals(status)) {
            statusLabel.setText("已归还");
            statusLabel.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 15; " +
                    "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        } else if ("OVERDUE".equals(status) || "已逾期".equals(status)) {
            statusLabel.setText("已逾期");
            statusLabel.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 15; " +
                    "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        // 日期信息
        HBox dateBox = new HBox(30);
        dateBox.setStyle("-fx-alignment: CENTER_LEFT;");

        Label borrowDateLabel = new Label("借阅日期：" + record.getBorrowDate());
        borrowDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        Label dueDateLabel = new Label("应还日期：" + record.getDueDate());
        dueDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        if ("OVERDUE".equals(status) || "已逾期".equals(status)) {
            dueDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }

        dateBox.getChildren().addAll(borrowDateLabel, dueDateLabel);

        infoBox.getChildren().addAll(titleLabel, statusLabel, dateBox);

        // 右侧操作区
        VBox actionBox = new VBox(10);
        actionBox.setStyle("-fx-alignment: CENTER_RIGHT;");

        if ("BORROWING".equals(status) || "借阅中".equals(status)) {
            Button returnBtn = new Button("归还图书");
            returnBtn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10; " +
                    "-fx-padding: 8 20; -fx-font-size: 14px; -fx-cursor: hand;");
            returnBtn.setOnAction(e -> handleReturnBook(record));
            actionBox.getChildren().add(returnBtn);
        } else if ("RETURNED".equals(status) || "已归还".equals(status)) {
            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #22c55e;");
            actionBox.getChildren().add(checkIcon);

            if (record.getReturnDate() != null) {
                Label returnDateLabel = new Label("归还日期：" + record.getReturnDate());
                returnDateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
                actionBox.getChildren().add(returnDateLabel);
            }
        } else if ("OVERDUE".equals(status) || "已逾期".equals(status)) {
            Label alertIcon = new Label("⚠");
            alertIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #ef4444;");
            actionBox.getChildren().add(alertIcon);

            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
            Label overdueLabel = new Label("已逾期 " + overdueDays + " 天");
            overdueLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");

            Button urgentReturnBtn = new Button("立即归还");
            urgentReturnBtn.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 10; " +
                    "-fx-padding: 8 20; -fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            urgentReturnBtn.setOnAction(e -> handleReturnBook(record));

            actionBox.getChildren().addAll(overdueLabel, urgentReturnBtn);
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
        String url = "http://10.27.241.94:8081/api/borrow/return/" + record.getBookId();

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create("", MediaType.get("application/json")))
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
        Request request = new Request.Builder()
                .url(REVIEWS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BookReview> reviews = gson.fromJson(
                            response.body().string(),
                            new TypeToken<List<BookReview>>(){}.getType()
                    );
                    Platform.runLater(() -> renderReviews(reviews));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "加载书评失败", Alert.AlertType.ERROR)
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

        Label dateLabel = new Label(review.getReviewDate().toString());
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        header.getChildren().addAll(titleLabel, spacer, dateLabel);

        // 图书链接
        Hyperlink bookLink = new Hyperlink("查看图书详情");
        bookLink.setStyle("-fx-font-size: 14px;");
        bookLink.setOnAction(e -> viewBookDetail(review.getBookId()));

        // 评分
        HBox ratingBox = new HBox(5);
        ratingBox.setStyle("-fx-alignment: CENTER_LEFT;");

        int rating = review.getRating().intValue();
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < rating ? "★" : "☆");
            star.setStyle("-fx-font-size: 20px; -fx-text-fill: " + (i < rating ? "#fbbf24" : "#cbd5e1") + ";");
            ratingBox.getChildren().add(star);
        }

        Label ratingText = new Label(rating + " 分");
        ratingText.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        ratingBox.getChildren().add(ratingText);

        // 评论内容
        Label contentLabel = new Label(review.getContent());
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-line-spacing: 5;");
        contentLabel.setWrapText(true);

        item.getChildren().addAll(header, bookLink, ratingBox, contentLabel);

        return item;
    }

    private void viewBookDetail(Long bookId) {
        System.out.println("查看图书详情，图书ID: " + bookId);
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/org/example/sdubooks/book-detail.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());

            BookDetailController controller = loader.getController();
            controller.setBookId(bookId);

            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.setTitle("图书详情");
                stage.setMaximized(false);
                stage.setMaximized(true);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "无法加载图书详情页", javafx.scene.control.Alert.AlertType.ERROR);
        }
    }

    @Override
    protected Stage getCurrentStage() {
        if (contentArea != null && contentArea.getScene() != null) {
            return (Stage) contentArea.getScene().getWindow();
        }
        return null;
    }
}
