package org.example.sdubooks.controller;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
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
import javafx.stage.Stage;
import okhttp3.*;
import org.example.sdubooks.model.HomeStats;

import java.io.IOException;

public class HomePageController extends BaseController {

    @FXML private Label bookCountLabel;
    @FXML private Label borrowCountLabel;
    @FXML private Label userCountLabel;
    @FXML private GridPane categoryGrid; // 确保 FXML 中 GridPane 的 fx:id 是 categoryGrid

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final String BASE_URL = "http://10.27.241.94:8081/api/home";
    private static final String STATS_URL = BASE_URL + "/stats";

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
        // 2. 动态生成图书分类
        initCategoryGrid();
    }

    /**
     * 动态生成图书分类卡片
     */
    private void initCategoryGrid() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            String emoji = CATEGORIES[i][0];
            String name = CATEGORIES[i][1];

            Button btn = new Button(emoji);
            btn.setStyle("-fx-background-color: transparent; -fx-font-size: 48px; -fx-cursor: hand;");
            btn.setUserData(name);
            btn.setOnAction(this::handleCategoryClick);

            Label label = new Label(name);
            label.setStyle("-fx-font-size: 16px; -fx-text-fill: #1e293b; -fx-font-weight: 500;");

            VBox card = new VBox(10.0, btn, label);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 30; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

            categoryGrid.add(card, i % 4, i / 4);
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

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    HomeStats stats = gson.fromJson(jsonData, HomeStats.class);

                    // 在 JavaFX 主线程更新 UI
                    javafx.application.Platform.runLater(() -> {
                        bookCountLabel.setText(String.valueOf(stats.getBookCount()));
                        borrowCountLabel.setText(String.valueOf(stats.getBorrowCount()));
                        userCountLabel.setText(String.valueOf(stats.getUserCount()));
                    });
                }
            }
        });
    }

    /**
     * 处理分类点击事件
     */
    private void handleCategoryClick(ActionEvent event) {
        Button btn = (Button) event.getSource();
        String categoryName = (String) btn.getUserData();
        System.out.println("点击了分类: " + categoryName);

        // TODO: 这里可以添加跳转到对应分类图书列表的逻辑
        // navigateTo("/org/example/sdubooks/book-list.fxml", "分类: " + categoryName, btn);
    }

    /**
     * 查看排行榜
     */
    @FXML
    protected void handleViewRankings(ActionEvent event) {
        System.out.println("点击了查看排行榜按钮");
        // 通过 event.getSource() 获取当前窗口，而不是去新 FXML 里找控件
        navigateTo("/org/example/sdubooks/ranking.fxml", "排行榜", (Node) event.getSource());
    }

    /**
     * 处理书籍卡片点击
     */
    @FXML
    protected void handleBookCardClick(MouseEvent event) {
        HBox card = (HBox) event.getSource();
        String bookName = (String) card.getUserData();
        System.out.println("点击了书籍卡片按钮: " + bookName);

        navigateTo("/org/example/sdubooks/book-detail.fxml", "图书详情: " + bookName, (Node) event.getSource());
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
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);

            // ✅ 正确获取 Stage 的方式：从触发事件的节点往上找
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
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