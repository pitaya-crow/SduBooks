package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import okhttp3.*;
import org.example.sdubooks.model.HomeStats;
import org.example.sdubooks.model.HotBook;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomePageController extends BaseController {

    @FXML private Label bookCountLabel;
    @FXML private Label borrowCountLabel;
    @FXML private Label userCountLabel;
    @FXML private GridPane categoryGrid;
    @FXML private VBox hotBooksContainer;

    private static final String STATS_URL = BASE_URL + "/home/stats";
    private static final String HOT_BOOKS_URL = BASE_URL + "/home/hot-books";

    // 书名 -> bookId 映射，用于点击卡片时传递 ID
    private final Map<String, Long> bookIdMap = new HashMap<>();

    // 数据定义 —— 所有分类一目了然
    private static final String[][] CATEGORIES = {
            {"📚", "小说"}, {"💻", "科技"}, {"📜", "历史"}, {"🔬", "科学"},
            {"🎨", "艺术"}, {"💼", "商业"}, {"🧠", "心理学"}, {"👤", "传记"}
    };

    /**
     * 初始化方法（只能有一个）
     */
    @FXML
    public void initialize() {
        // 1. 加载统计数据
        fetchStatsData();
        // 2. 加载热门书籍数据
        fetchHotBooks();
        // 3. 动态生成图书分类
        initCategoryGrid();
    }

    @Override
    protected Stage getCurrentStage() {
        if (categoryGrid != null && categoryGrid.getScene() != null) {
            return (Stage) categoryGrid.getScene().getWindow();
        }
        return null;
    }

    /**
     * 动态生成图书分类卡片（与最热书籍相同的卡片样式）
     */
    private void initCategoryGrid() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            String emoji = CATEGORIES[i][0];
            String name = CATEGORIES[i][1];

            HBox card = new HBox(15);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                    "-fx-padding: 20; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;");

            // 图标区域
            javafx.scene.layout.StackPane iconBox = new javafx.scene.layout.StackPane();
            iconBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 15; " +
                    "-fx-min-width: 60; -fx-max-width: 60; -fx-min-height: 60; -fx-max-height: 60; -fx-alignment: CENTER;");
            Label iconLabel = new Label(emoji);
            iconLabel.setStyle("-fx-font-size: 28px;");
            iconBox.getChildren().add(iconLabel);

            // 信息区域
            VBox infoBox = new VBox(5);
            infoBox.setStyle("-fx-alignment: CENTER_LEFT;");
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label descLabel = new Label("浏览" + name + "类图书");
            descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

            infoBox.getChildren().addAll(nameLabel, descLabel);

            // 箭头
            Label arrowLabel = new Label("→");
            arrowLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #94a3b8;");

            card.getChildren().addAll(iconBox, infoBox, arrowLabel);

            // 点击事件
            final String categoryName = name;
            card.setOnMouseClicked(e -> {
                CategoryController.setSelectedCategory(categoryName);
                loadPageInShell("/org/example/sdubooks/category.fxml");
            });

            categoryGrid.add(card, i % 2, i / 2);
        }
    }

    /**
     * 获取首页统计数据
     */
    private void fetchStatsData() {
        Request request = new Request.Builder()
                .url(STATS_URL)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    System.out.println("[DEBUG] 首页统计响应: " + jsonData);
                    // 提取 data 字段
                    String dataStr = jsonData;
                    try {
                        com.google.gson.JsonObject json = gson.fromJson(jsonData, com.google.gson.JsonObject.class);
                        if (json.has("data") && !json.get("data").isJsonNull()) {
                            dataStr = json.get("data").toString();
                        }
                    } catch (Exception ignored) {}

                    HomeStats stats = gson.fromJson(dataStr, HomeStats.class);
                    if (stats != null) {
                        javafx.application.Platform.runLater(() -> {
                            bookCountLabel.setText(String.valueOf(stats.getBookCount()));
                            borrowCountLabel.setText(String.valueOf(stats.getBorrowCount()));
                            userCountLabel.setText(String.valueOf(stats.getUserCount()));
                        });
                    }
                }
            }
        });
    }

    /**
     * 获取热门书籍数据并渲染卡片
     */
    private void fetchHotBooks() {
        Request request = new Request.Builder()
                .url(HOT_BOOKS_URL)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("[DEBUG] 获取热门书籍失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    System.out.println("[DEBUG] 热门书籍响应: " + jsonData);

                    JsonObject json = gson.fromJson(jsonData, JsonObject.class);
                    if (json.has("data") && json.get("data").isJsonArray()) {
                        List<HotBook> hotBooks = gson.fromJson(
                                json.getAsJsonArray("data"),
                                new TypeToken<List<HotBook>>(){}.getType());
                        for (HotBook book : hotBooks) {
                            if (book.getId() != null) {
                                bookIdMap.put(book.getTitle(), book.getId());
                            }
                        }
                        // 渲染书籍卡片
                        javafx.application.Platform.runLater(() -> renderHotBooks(hotBooks));
                    }
                }
            }
        });
    }

    /**
     * 渲染热门书籍卡片（两列 GridPane 布局）
     */
    private void renderHotBooks(List<HotBook> hotBooks) {
        if (hotBooksContainer == null) return;
        hotBooksContainer.getChildren().clear();

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(15);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        for (int i = 0; i < hotBooks.size(); i++) {
            HBox card = createHotBookCard(hotBooks.get(i), i);
            grid.add(card, i % 2, i / 2);
        }

        hotBooksContainer.getChildren().add(grid);
    }

    private HBox createHotBookCard(HotBook book, int index) {
        HBox card = new HBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); -fx-padding: 0; -fx-cursor: hand;");

        // 封面区域
        javafx.scene.layout.StackPane coverBox = new javafx.scene.layout.StackPane();
        String[] colors = {"#d4a574", "#7c8cf5", "#6bc5a0", "#e8a87c", "#a78bfa", "#67b8d6", "#f5a5a5", "#8dd6a0"};
        String color = colors[index % colors.length];
        coverBox.setStyle("-fx-background-color: " + color + "; -fx-pref-width: 150; -fx-min-width: 150; " +
                "-fx-max-width: 150; -fx-pref-height: 180; -fx-min-height: 180; -fx-max-height: 180; " +
                "-fx-background-radius: 20 0 0 20; -fx-alignment: CENTER;");
        Label emoji = new Label("📚");
        emoji.setStyle("-fx-font-size: 40px;");
        coverBox.getChildren().add(emoji);

        // 信息区域
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-padding: 18; -fx-alignment: CENTER_LEFT;");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #64748b;");

        javafx.scene.layout.HBox ratingBox = new javafx.scene.layout.HBox(5);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        Label star = new Label("⭐");
        star.setStyle("-fx-font-size: 16px;");
        Label ratingVal = new Label(book.getRating() != null ? String.valueOf(book.getRating()) : "暂无");
        ratingVal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        ratingBox.getChildren().addAll(star, ratingVal);

        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        Label rankTag = new Label("Top " + (index + 1));
        rankTag.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; " +
                "-fx-font-size: 13px; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label borrowLabel = new Label("借阅 " + book.getBorrowCount() + " 次");
        borrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        bottomBox.getChildren().addAll(rankTag, spacer, borrowLabel);

        infoBox.getChildren().addAll(titleLabel, authorLabel, ratingBox, bottomBox);
        card.getChildren().addAll(coverBox, infoBox);

        // 点击跳转详情
        final Long bookId = book.getId();
        final String bookName = book.getTitle();
        card.setOnMouseClicked(e -> {
            if (bookId != null) {
                BookDetailController.setSelectedBookId(bookId);
                loadPageInShell("/org/example/sdubooks/book-detail.fxml");
            }
        });

        return card;
    }

    /**
     * 处理分类点击事件
     */
    private void handleCategoryClick(ActionEvent event) {
        Button btn = (Button) event.getSource();
        String categoryName = (String) btn.getUserData();
        System.out.println("点击了分类: " + categoryName);

        // 设置分类名，然后通过 Shell 加载分类页面
        CategoryController.setSelectedCategory(categoryName);
        loadPageInShell("/org/example/sdubooks/category.fxml");
    }

    /**
     * 查看排行榜
     */
    @FXML
    protected void handleViewRankings(ActionEvent event) {
        System.out.println("点击了查看排行榜按钮");
        loadPageInShell("/org/example/sdubooks/ranking.fxml");
    }

    /**
     * 处理书籍卡片点击
     */
    @FXML
    protected void handleBookCardClick(MouseEvent event) {
        HBox card = (HBox) event.getSource();
        String bookName = (String) card.getUserData();
        System.out.println("点击了书籍卡片按钮: " + bookName);

        Long bookId = bookIdMap.get(bookName);
        if (bookId == null) {
            System.out.println("[DEBUG] 未找到书籍ID: " + bookName + "，bookIdMap: " + bookIdMap);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("无法获取书籍信息，请稍后重试");
            alert.showAndWait();
            return;
        }

        navigateToBookDetail(bookId, bookName, (Node) event.getSource());
    }

    /**
     * 跳转到图书详情页并传递 bookId
     */
    private void navigateToBookDetail(Long bookId, String bookName, Node sourceNode) {
        BookDetailController.setSelectedBookId(bookId);
        loadPageInShell("/org/example/sdubooks/book-detail.fxml");
    }

    // ==========================================
    // 🌟 提取公共的页面跳转方法，避免代码重复
    // ==========================================

    /**
     * 通用页面跳转方法
     * @param fxmlPath  FXML 文件路径
     * @param title     新窗口标题
     * @param sourceNode 触发事件的节点（用于获取当前 Stage）
     */
    private void navigateTo(String fxmlPath, String title, Node sourceNode) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(false);
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载页面: " + fxmlPath);
            alert.showAndWait();
        }
    }
}