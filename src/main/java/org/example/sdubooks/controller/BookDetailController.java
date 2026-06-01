package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.sdubooks.model.Book;
import org.example.sdubooks.model.BookReview;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class BookDetailController extends BaseController {

    @FXML private Button btnBack;
    @FXML private VBox coverBox;
    @FXML private Label categoryLabel;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label ratingLabel;
    @FXML private Label borrowCountLabel;
    @FXML private Label availableLabel;
    @FXML private Label isbnLabel;
    @FXML private Label publisherLabel;
    @FXML private Label publishDateLabel;
    @FXML private Label totalLabel;
    @FXML private Button btnBorrow;
    @FXML private Button btnWriteReview;
    @FXML private Label descriptionLabel;
    @FXML private Label reviewCountLabel;
    @FXML private VBox reviewsContainer;

    private Long bookId;
    private Book currentBook;

    // 静态 bookId，供其他控制器在加载页面前设置
    private static Long pendingBookId;

    public static void setSelectedBookId(Long id) {
        pendingBookId = id;
    }

    private static final String ADMIN_BASE_URL = "http://localhost:8081/api/admin";
    private static final String BOOK_DETAIL_URL = ADMIN_BASE_URL + "/books";
    private static final String REVIEW_BASE_URL = BASE_URL+"/review";
    private static final String BORROW_URL = BASE_URL+"/borrow";

    @FXML
    public void initialize() {
        btnBack.setVisible(false);
        // 如果有通过静态方法设置的 bookId，自动加载
        if (pendingBookId != null) {
            this.bookId = pendingBookId;
            pendingBookId = null;
            loadBookDetail();
        }
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
        loadBookDetail();
    }

    private void loadBookDetail() {
        if (bookId == null) {
            showAlert("错误", "图书ID不能为空", Alert.AlertType.ERROR);
            return;
        }

        String token = getToken();
        Request request = new Request.Builder()
                .url(BOOK_DETAIL_URL + "/" + bookId)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 图书详情响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    // 提取 data 字段
                    String dataStr = respBody;
                    try {
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.has("data") && !json.get("data").isJsonNull()) {
                            dataStr = json.get("data").toString();
                        }
                    } catch (Exception ignored) {}
                    currentBook = gson.fromJson(dataStr, Book.class);
                    if (currentBook != null) {
                        Platform.runLater(() -> {
                            displayBookDetail(currentBook);
                            loadReviews();
                            btnBack.setVisible(true);
                        });
                    }
                } else {
                    Platform.runLater(() ->
                            showAlert("错误", "加载图书详情失败 (HTTP " + response.code() + ")", Alert.AlertType.ERROR)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void displayBookDetail(Book book) {
        categoryLabel.setText(book.getCategory() != null ? book.getCategory() : "");
        titleLabel.setText(book.getTitle() != null ? book.getTitle() : "");
        authorLabel.setText(book.getAuthor() != null ? book.getAuthor() : "");
        ratingLabel.setText(book.getRating() != null ? String.valueOf(book.getRating()) : "0");
        borrowCountLabel.setText("借阅 " + (book.getBorrowCount() != null ? book.getBorrowCount() : 0) + " 次");
        availableLabel.setText("可借 " + (book.getAvailable() != null ? book.getAvailable() : 0) + "/" + (book.getTotal() != null ? book.getTotal() : 0));
        isbnLabel.setText(book.getIsbn() != null ? book.getIsbn() : "");
        publisherLabel.setText(book.getPublisher() != null ? book.getPublisher() : "");
        publishDateLabel.setText(book.getPublishDate() != null ? book.getPublishDate() : "");
        totalLabel.setText((book.getTotal() != null ? book.getTotal() : 0) + " 册（可借 " + (book.getAvailable() != null ? book.getAvailable() : 0) + " 册）");
        descriptionLabel.setText(book.getDescription() != null ? book.getDescription() : "暂无简介");

        int available = book.getAvailable() != null ? book.getAvailable() : 0;
        if (available <= 0) {
            btnBorrow.setDisable(true);
            btnBorrow.setText("暂无库存");
            btnBorrow.setStyle("-fx-background-color: #cbd5e1; -fx-background-radius: 15; -fx-padding: 15 40; -fx-font-size: 16px; -fx-text-fill: #64748b;");
        }
    }

    private void loadReviews() {
        String token = getToken();
        Request request = new Request.Builder()
                .url(REVIEW_BASE_URL + "/book/" + bookId)
                .header("Authorization", "Bearer " + token)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    String dataStr = respBody;
                    try {
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.has("data") && !json.get("data").isJsonNull()) {
                            dataStr = json.get("data").toString();
                        }
                    } catch (Exception ignored) {}
                    List<BookReview> reviews = gson.fromJson(dataStr,
                            new TypeToken<List<BookReview>>(){}.getType());
                    if (reviews == null) reviews = new java.util.ArrayList<>();
                    final List<BookReview> finalReviews = reviews;
                    Platform.runLater(() -> {
                        reviewCountLabel.setText("读者书评 (" + finalReviews.size() + ")");
                        displayReviews(finalReviews);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void displayReviews(List<BookReview> reviews) {
        reviewsContainer.getChildren().clear();

        for (BookReview review : reviews) {
            reviewsContainer.getChildren().add(createReviewCard(review));
        }
    }

    private VBox createReviewCard(BookReview review) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        HBox userInfo = new HBox(15);
        userInfo.setStyle("-fx-alignment: CENTER_LEFT;");

        VBox avatarBox = new VBox();
        avatarBox.setStyle("-fx-min-width: 50px; -fx-min-height: 50px; -fx-background-color: #e2e8f0; " +
                "-fx-background-radius: 25; -fx-alignment: CENTER;");
        Label avatarLabel = new Label("");
        avatarLabel.setStyle("-fx-font-size: 24px;");
        avatarBox.getChildren().add(avatarLabel);

        VBox nameBox = new VBox(5);
        Label usernameLabel = new Label(review.getUsername() != null ? review.getUsername() : "匿名用户");
        usernameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        HBox ratingDateBox = new HBox(10);
        ratingDateBox.setStyle("-fx-alignment: CENTER_LEFT;");

        int rating = review.getRating() != null ? review.getRating().intValue() : 0;
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < rating ? "★" : "☆");
            star.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (i < rating ? "#fbbf24" : "#cbd5e1") + ";");
            ratingDateBox.getChildren().add(star);
        }

        Label dateLabel = new Label(review.getCreateTime() != null ? review.getCreateTime().toLocalDate().toString() : "");
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        ratingDateBox.getChildren().add(dateLabel);

        nameBox.getChildren().addAll(usernameLabel, ratingDateBox);
        userInfo.getChildren().addAll(avatarBox, nameBox);

        Label contentLabel = new Label(review.getContent() != null ? review.getContent() : "");
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-line-spacing: 5;");
        contentLabel.setWrapText(true);

        card.getChildren().addAll(userInfo, contentLabel);

        return card;
    }

    @FXML
    private void handleBack() {
        loadPageInShell("/org/example/sdubooks/homepage-view.fxml");
    }

    @FXML
    private void handleBorrow() {
        if (currentBook == null) {
            showAlert("错误", "图书信息未加载，请稍后重试", Alert.AlertType.ERROR);
            return;
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            showAlert("错误", "无法获取用户信息，请重新登录", Alert.AlertType.ERROR);
            return;
        }

        // 自定义借阅须知弹窗
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("借阅须知");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(450);

        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        Label titleLabel = new Label("📖 借阅须知 — 《" + currentBook.getTitle() + "》");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String termsText = "1. 借阅期限为 30 天，从借阅之日起计算。\n\n" +
                "2. 逾期未还者，按每天 0.1 元计收逾期费用。\n\n" +
                "3. 请爱护图书，如有损坏或遗失需照价赔偿。\n\n" +
                "4. 每人每书同一时间只能借阅一本。\n\n" +
                "5. 归还图书后方可再次借阅同一本书。";

        Label termsLabel = new Label(termsText);
        termsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569; -fx-line-spacing: 4;");
        termsLabel.setWrapText(true);

        javafx.scene.control.CheckBox confirmCheck = new javafx.scene.control.CheckBox("我已阅读并同意以上借阅须知");
        confirmCheck.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 初始时确定按钮不可用
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        okButton.setText("确定借阅");
        confirmCheck.selectedProperty().addListener((obs, oldVal, newVal) -> okButton.setDisable(!newVal));

        content.getChildren().addAll(titleLabel, termsLabel, confirmCheck);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performBorrow();
            }
        });
    }

    private void performBorrow() {
        String token = getToken();
        Long userId = getCurrentUserId();

        JsonObject borrowData = new JsonObject();
        borrowData.addProperty("bookId", bookId);
        borrowData.addProperty("userId", userId);

        String jsonBody = gson.toJson(borrowData);
        System.out.println("[DEBUG] 借阅请求 URL: " + BORROW_URL);
        System.out.println("[DEBUG] 借阅请求 Body: " + jsonBody);

        Request request = new Request.Builder()
                .url(BORROW_URL)
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 借阅响应 Code: " + response.code());
                System.out.println("[DEBUG] 借阅响应 Body: " + responseBody);
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", "借阅成功！", Alert.AlertType.INFORMATION);
                        loadBookDetail();
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("错误", "借阅失败: " + responseBody, Alert.AlertType.ERROR)
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private Long getCurrentUserId() {
        String token = getToken();
        if (token == null) {
            System.out.println("[DEBUG] Token 为 null");
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                System.out.println("[DEBUG] JWT payload: " + payload);
                JsonObject json = gson.fromJson(payload, JsonObject.class);
                // 尝试多种可能的 claim 名称
                String[] claimNames = {"userId", "id", "sub", "user_id"};
                for (String claim : claimNames) {
                    if (json.has(claim)) {
                        try {
                            Long id = json.get(claim).getAsLong();
                            System.out.println("[DEBUG] 从 claim '" + claim + "' 获取到 userId: " + id);
                            return id;
                        } catch (NumberFormatException ignored) {
                            // sub 字段可能是字符串，不是数字
                        }
                    }
                }
                System.out.println("[DEBUG] JWT 中未找到用户ID字段，可用字段: " + json.keySet());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @FXML
    private void handleWriteReview() {
        if (currentBook == null) return;

        showWriteReviewDialog();
    }

    private void showWriteReviewDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("写书评");
        dialog.setHeaderText(null);

        VBox dialogContent = new VBox(20);
        dialogContent.setStyle("-fx-padding: 20; -fx-spacing: 20;");

        Label titleLabel = new Label("写书评 - " + currentBook.getTitle());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label ratingLabel = new Label("评分");
        ratingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        HBox starBox = new HBox(10);
        starBox.setStyle("-fx-alignment: CENTER_LEFT;");
        
        final int[] currentRating = {5};
        Label[] stars = new Label[5];
        
        for (int i = 0; i < 5; i++) {
            final int starIndex = i;
            Label star = new Label("★");
            star.setStyle("-fx-font-size: 36px; -fx-text-fill: #fbbf24; -fx-cursor: hand;");
            
            star.setOnMouseClicked(e -> {
                currentRating[0] = starIndex + 1;
                for (int j = 0; j < 5; j++) {
                    if (j <= starIndex) {
                        stars[j].setStyle("-fx-font-size: 36px; -fx-text-fill: #fbbf24; -fx-cursor: hand;");
                    } else {
                        stars[j].setStyle("-fx-font-size: 36px; -fx-text-fill: #cbd5e1; -fx-cursor: hand;");
                    }
                }
            });
            
            stars[i] = star;
            starBox.getChildren().add(star);
        }

        Label contentLabel = new Label("书评内容");
        contentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        TextArea textArea = new TextArea();
        textArea.setPromptText("分享你的阅读感受...");
        textArea.setPrefRowCount(4);
        textArea.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        Button submitBtn = new Button("提交书评");
        submitBtn.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 15; -fx-padding: 15 40; " +
                "-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> {
            String reviewContent = textArea.getText();
            if (reviewContent == null || reviewContent.trim().isEmpty()) {
                showAlert("提示", "请输入书评内容", Alert.AlertType.WARNING);
                return;
            }
            submitReview(currentRating[0], reviewContent);
            dialog.close();
        });

        dialogContent.getChildren().addAll(titleLabel, ratingLabel, starBox, contentLabel, textArea, submitBtn);

        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        dialog.showAndWait();
    }

    private void submitReview(int rating, String content) {
        String token = getToken();
        Long userId = getCurrentUserId();

        JsonObject reviewData = new JsonObject();
        reviewData.addProperty("bookId", bookId);
        reviewData.addProperty("rating", (double) rating);
        reviewData.addProperty("content", content);
        if (userId != null) {
            reviewData.addProperty("userId", userId.intValue());
        }

        String json = reviewData.toString();
        System.out.println("[DEBUG] 发表书评请求: " + json);

        Request request = new Request.Builder()
                .url(REVIEW_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 发表书评响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", "书评发表成功！", Alert.AlertType.INFORMATION);
                        loadReviews();
                        loadBookDetail(); // 刷新评分
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("错误", "发表书评失败 (HTTP " + response.code() + "): " + respBody, Alert.AlertType.ERROR)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    @Override
    protected Stage getCurrentStage() {
        if (reviewsContainer != null && reviewsContainer.getScene() != null) {
            return (Stage) reviewsContainer.getScene().getWindow();
        }
        return null;
    }
}
