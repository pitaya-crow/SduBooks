package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.sdubooks.model.Book;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoryController extends BaseController {

    @FXML private Label categoryTitleLabel;
    @FXML private VBox bookListContainer;
    @FXML private Label countLabel;

    private static final String BOOK_LIST_URL = BASE_URL + "/book/list";

    // 由 HomePageController 在加载前设置
    private static String selectedCategory;

    public static void setSelectedCategory(String category) {
        selectedCategory = category;
    }

    @FXML
    public void initialize() {
        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            categoryTitleLabel.setText(selectedCategory);
            loadBooksByCategory(selectedCategory);
        }
    }

    private void loadBooksByCategory(String category) {
        String token = getToken();
        String url = BOOK_LIST_URL + "?category=" + category;

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    System.out.println("[DEBUG] 分类书籍响应: code=" + response.code() + " body=" + respBody);
                    if (response.isSuccessful()) {
                        List<Book> books = null;
                        try {
                            JsonObject json = gson.fromJson(respBody, JsonObject.class);
                            if (json.has("data") && json.get("data").isJsonArray()) {
                                books = gson.fromJson(json.getAsJsonArray("data"),
                                        new TypeToken<List<Book>>(){}.getType());
                            }
                        } catch (Exception e) {
                            books = gson.fromJson(respBody, new TypeToken<List<Book>>(){}.getType());
                        }
                        if (books == null) books = new ArrayList<>();

                        final List<Book> finalBooks = books;
                        Platform.runLater(() -> {
                            countLabel.setText("共 " + finalBooks.size() + " 本书");
                            renderBookCards(finalBooks);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderBookCards(List<Book> books) {
        bookListContainer.getChildren().clear();

        // 使用 GridPane 实现两列布局，与首页风格一致
        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(25);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        for (int i = 0; i < books.size(); i++) {
            HBox card = createBookCard(books.get(i));
            grid.add(card, i % 2, i / 2);
        }

        bookListContainer.getChildren().add(grid);
    }

    private HBox createBookCard(Book book) {
        HBox card = new HBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); -fx-padding: 0; -fx-cursor: hand;");

        // 封面区域
        StackPane coverBox = new StackPane();
        coverBox.setStyle("-fx-background-color: #e2e8f0; -fx-pref-width: 150; -fx-min-width: 150; " +
                "-fx-max-width: 150; -fx-pref-height: 200; -fx-min-height: 200; -fx-max-height: 200; " +
                "-fx-background-radius: 20 0 0 20;");

        Label coverEmoji = new Label("📚");
        coverEmoji.setStyle("-fx-font-size: 40px;");
        coverBox.getChildren().add(coverEmoji);

        // 信息区域
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-padding: 20; -fx-pref-width: 350;");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #475569;");

        // 评分
        HBox ratingBox = new HBox(5);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        Label starLabel = new Label("⭐");
        starLabel.setStyle("-fx-font-size: 18px;");
        Label ratingValue = new Label(book.getRating() != null ? String.valueOf(book.getRating()) : "暂无");
        ratingValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        ratingBox.getChildren().addAll(starLabel, ratingValue);

        // 简介
        Label descLabel = new Label(book.getDescription() != null ? book.getDescription() : "");
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(320);
        descLabel.setMaxHeight(60);

        // 分类 + 借阅次数
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        Label categoryTag = new Label(book.getCategory() != null ? book.getCategory() : "");
        categoryTag.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; " +
                "-fx-font-size: 14px; -fx-padding: 5 12; -fx-background-radius: 15;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label borrowLabel = new Label("借阅 " + (book.getBorrowCount() != null ? book.getBorrowCount() : 0) + " 次");
        borrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        bottomBox.getChildren().addAll(categoryTag, spacer, borrowLabel);

        // 可借状态
        Label statusLabel;
        if (book.getAvailable() != null && book.getAvailable() > 0) {
            statusLabel = new Label("可借 " + book.getAvailable() + " 册");
            statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        } else {
            statusLabel = new Label("已借出");
            statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        }

        infoBox.getChildren().addAll(titleLabel, authorLabel, ratingBox, descLabel, bottomBox, statusLabel);

        card.getChildren().addAll(coverBox, infoBox);

        // 点击卡片跳转到详情
        card.setOnMouseClicked(e -> {
            if (book.getId() != null) {
                BookDetailController.setSelectedBookId(book.getId().longValue());
                loadPageInShell("/org/example/sdubooks/book-detail.fxml");
            }
        });

        return card;
    }

    @Override
    protected Stage getCurrentStage() {
        if (categoryTitleLabel != null && categoryTitleLabel.getScene() != null) {
            return (Stage) categoryTitleLabel.getScene().getWindow();
        }
        return null;
    }
}
