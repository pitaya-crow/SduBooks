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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.sdubooks.model.RankingBook;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class RankingController extends BaseController {

    @FXML private Button btnAll;
    @FXML private Button btnMonth;
    @FXML private VBox rankingList;

    private String currentTab = "borrow";

    private static final String RANKING_BASE_URL = "http://localhost:8081/api/ranking";
    private static final String BORROW_TOP_URL = RANKING_BASE_URL + "/borrow";
    private static final String RATING_TOP_URL = RANKING_BASE_URL + "/score";

    @FXML
    public void initialize() {
        loadRankingData();
    }

    @FXML
    private void handleSwitchTab(javafx.event.ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        String tab = (String) clickedBtn.getUserData();

        if (currentTab.equals(tab)) {
            return;
        }

        currentTab = tab;

        // 更新按钮样式
        updateButtonStyles(clickedBtn);

        // 重新加载数据
        loadRankingData();
    }

    private void updateButtonStyles(Button activeBtn) {
        String activeStyle = "-fx-background-color: #ffffff; -fx-background-radius: 25; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 10 30; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-font-size: 16px; -fx-text-fill: #64748b; -fx-padding: 10 30; -fx-cursor: hand;";

        btnAll.setStyle(inactiveStyle);
        btnMonth.setStyle(inactiveStyle);

        activeBtn.setStyle(activeStyle);
    }

    private void loadRankingData() {
        String url;
        switch (currentTab) {
            case "rating":
                url = RATING_TOP_URL;
                break;
            case "borrow":
            default:
                url = BORROW_TOP_URL;
                break;
        }

        String token = getToken();
        System.out.println("[DEBUG] 排行榜请求URL: " + url);

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    System.out.println("[DEBUG] 排行榜响应: code=" + response.code() + " body=" + respBody);
                    if (response.isSuccessful()) {
                        // 后端返回 {"code":200, "data":[...]}
                        String dataStr = respBody;
                        try {
                            JsonObject json = gson.fromJson(respBody, JsonObject.class);
                            if (json.has("data") && !json.get("data").isJsonNull()) {
                                dataStr = json.get("data").toString();
                            }
                        } catch (Exception ignored) {}

                        List<RankingBook> books = gson.fromJson(dataStr,
                                new TypeToken<List<RankingBook>>(){}.getType());
                        Platform.runLater(() -> renderRankingList(books != null ? books : new java.util.ArrayList<>()));
                    } else {
                        Platform.runLater(() ->
                                showAlert("错误", "获取排行榜数据失败 (HTTP " + response.code() + ")", Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void renderRankingList(List<RankingBook> books) {
        rankingList.getChildren().clear();

        for (int i = 0; i < books.size(); i++) {
            RankingBook book = books.get(i);
            rankingList.getChildren().add(createRankingItem(book, i + 1));
        }
    }

    private VBox createRankingItem(RankingBook book, int rank) {
        VBox item = new VBox(10);
        item.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");

        HBox content = new HBox(20);
        content.setStyle("-fx-alignment: CENTER_LEFT;");

        // 排名
        VBox rankBox = new VBox();
        rankBox.setStyle("-fx-alignment: CENTER; -fx-min-width: 60px;");

        if (rank <= 3) {
            Label medalLabel = new Label(getMedal(rank));
            medalLabel.setStyle("-fx-font-size: 36px;");
            rankBox.getChildren().add(medalLabel);
        } else {
            Label rankLabel = new Label(String.valueOf(rank));
            rankLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
            rankBox.getChildren().add(rankLabel);
        }

        // 封面
        VBox coverBox = new VBox();
        coverBox.setStyle("-fx-min-width: 100px; -fx-min-height: 130px; " +
                "-fx-background-color: #e2e8f0; -fx-background-radius: 10; " +
                "-fx-alignment: CENTER;");
        Label coverLabel = new Label("📚");
        coverLabel.setStyle("-fx-font-size: 40px;");
        coverBox.getChildren().add(coverLabel);

        // 书籍信息
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-alignment: CENTER_LEFT;");
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        HBox categoryBox = new HBox(10);
        categoryBox.setStyle("-fx-alignment: CENTER_LEFT;");

        Label categoryLabel = new Label(book.getCategory());
        categoryLabel.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 15; " +
                "-fx-padding: 5 15; -fx-font-size: 14px; -fx-text-fill: #475569;");

        Label ratingLabel = new Label("评分 " + book.getRating());
        ratingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        categoryBox.getChildren().addAll(categoryLabel, ratingLabel);

        infoBox.getChildren().addAll(titleLabel, authorLabel, categoryBox);

        // 借阅次数
        VBox countBox = new VBox(5);
        countBox.setStyle("-fx-alignment: CENTER_RIGHT; -fx-min-width: 120px;");

        Label countLabel = new Label(String.valueOf(book.getBorrowCount()));
        countLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");

        Label countTextLabel = new Label("借阅次数");
        countTextLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        Label hotLabel = new Label("热门");
        hotLabel.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #ea580c); " +
                "-fx-background-radius: 15; -fx-padding: 5 15; -fx-font-size: 14px; " +
                "-fx-text-fill: white; -fx-font-weight: bold;");

        countBox.getChildren().addAll(countLabel, countTextLabel, hotLabel);

        content.getChildren().addAll(rankBox, coverBox, infoBox, countBox);
        item.getChildren().add(content);

        return item;
    }

    private String getMedal(int rank) {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return "";
        }
    }

    @Override
    protected Stage getCurrentStage() {
        // 通过任意已绑定的FXML控件获取Stage
        if (rankingList != null && rankingList.getScene() != null) {
            return (Stage) rankingList.getScene().getWindow();
        }
        return null;
    }
}
