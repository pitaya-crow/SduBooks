package org.example.sdubooks.controller;

import javafx.event.ActionEvent;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.sdubooks.model.HomeStats;
import okhttp3.*;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController extends BaseController{

    @FXML
    private Label bookCountLabel;
    @FXML
    private Label borrowCountLabel;
    @FXML
    private Label userCountLabel;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final String BASE_URL = "http://10.27.241.94:8081/api/home";
    private static final String STATS_URL = BASE_URL + "/stats";

    @FXML
    public void initialize() {
        fetchStatsData();
    }

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
    @FXML
    protected void handleCategoryClick(ActionEvent event) {
        Button button = (Button) event.getSource();
        String category = (String) button.getUserData();
        System.out.println("点击了图书分类按钮: " + category);

        // 跳转到图书分类页面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/category.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) ((Button) fxmlLoader.getNamespace().get("usernameField")).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("图书分类" +  category);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载图书分类页面");
            alert.showAndWait();
        }
    }

    @FXML
    protected void handleViewRankings() {
        System.out.println("点击了查看排行榜按钮");

        // 跳转到排行榜页面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/ranking.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) ((Button) fxmlLoader.getNamespace().get("usernameField")).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("排行榜");
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载排行榜页面");
            alert.showAndWait();
        }
    }

    @FXML
    protected void handleBookCardClick(MouseEvent event) {
        HBox card = (HBox) event.getSource();
        String bookName = (String) card.getUserData();
        System.out.println("点击了书籍卡片按钮: " + bookName);

        // 跳转到图书详情页面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/example/sdubooks/book-detail.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 450, 650);
            Stage stage = (Stage) ((Button) fxmlLoader.getNamespace().get("usernameField")).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("图书详情: " + bookName);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("无法加载图书详情");
            alert.showAndWait();
        }
    }
}
