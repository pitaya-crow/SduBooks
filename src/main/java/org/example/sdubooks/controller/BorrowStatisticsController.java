package org.example.sdubooks.controller;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.example.sdubooks.model.*;
import okhttp3.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BorrowStatisticsController extends BaseController {

    @FXML private Label totalBorrowLabel;
    @FXML private Label totalReturnLabel;
    @FXML private Label growthRateLabel;

    @FXML private VBox trendChartBox;
    @FXML private VBox categoryChartBox;
    @FXML private VBox borrowRankBox;

    private static final String BASE_URL = "http://10.27.241.94:8081/api/admin";
    private static final String STATS_URL = BASE_URL + "/borrow/stats";
    private static final String TREND_URL = BASE_URL + "/borrow/trend";
    private static final String CATEGORY_URL = BASE_URL + "/borrow/category";
    private static final String RANK_URL = BASE_URL + "/borrow/rank";

    @FXML
    public void initialize() {
        loadBorrowStatistics();
    }

    private void loadBorrowStatistics() {
        new Thread(() -> {
            try {
                loadStats();
                loadTrend();
                loadCategoryStats();
                loadBorrowRank();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("数据加载错误", "无法获取借阅统计数据，请检查网络连接", Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    private void loadStats() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(STATS_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                BorrowStats stats = gson.fromJson(response.body().string(), BorrowStats.class);
                Platform.runLater(() -> {
                    totalBorrowLabel.setText(String.valueOf(stats.getTotalBorrowCount()));
                    totalReturnLabel.setText(String.valueOf(stats.getTotalReturnCount()));

                    Double rate = stats.getMonthlyGrowthRate();
                    growthRateLabel.setText(String.format("%.1f%%", rate));

                    if (rate >= 0) {
                        growthRateLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                    } else {
                        growthRateLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTrend() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(TREND_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                List<BorrowTrend> trends = gson.fromJson(
                        response.body().string(),
                        new TypeToken<List<BorrowTrend>>(){}.getType()
                );
                Platform.runLater(() -> renderTrendChart(trends));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderTrendChart(List<BorrowTrend> trends) {
        trendChartBox.getChildren().clear();

        LineChart<String, Number> chart = new LineChart<>(
                new CategoryAxis(),
                new NumberAxis()
        );

        chart.setTitle("借阅趋势");
        chart.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
        xAxis.setLabel("月份");

        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLabel("次数");

        XYChart.Series<String, Number> borrowSeries = new XYChart.Series<>();
        borrowSeries.setName("借阅次数");

        XYChart.Series<String, Number> returnSeries = new XYChart.Series<>();
        returnSeries.setName("归还次数");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月");
        for (BorrowTrend trend : trends) {
            String month = trend.getDate().format(formatter);
            borrowSeries.getData().add(new XYChart.Data<>(month, trend.getBorrowCount()));
            returnSeries.getData().add(new XYChart.Data<>(month, trend.getReturnCount()));
        }

        chart.getData().addAll(borrowSeries, returnSeries);

        borrowSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #5B8FF9; -fx-stroke-width: 3px;");
        returnSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #5AD8A6; -fx-stroke-width: 3px;");

        trendChartBox.getChildren().add(chart);
    }

    private void loadCategoryStats() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(CATEGORY_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                List<CategoryStats> categories = gson.fromJson(
                        response.body().string(),
                        new TypeToken<List<CategoryStats>>(){}.getType()
                );
                Platform.runLater(() -> renderCategoryChart(categories));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderCategoryChart(List<CategoryStats> categories) {
        categoryChartBox.getChildren().clear();

        BarChart<String, Number> chart = new BarChart<>(
                new CategoryAxis(),
                new NumberAxis()
        );

        chart.setTitle("分类统计");
        chart.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        chart.setLegendVisible(true);

        CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
        xAxis.setLabel("分类");

        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLabel("数量");

        XYChart.Series<String, Number> bookSeries = new XYChart.Series<>();
        bookSeries.setName("图书数量");

        XYChart.Series<String, Number> borrowSeries = new XYChart.Series<>();
        borrowSeries.setName("借阅次数");

        for (CategoryStats category : categories) {
            bookSeries.getData().add(new XYChart.Data<>(category.getCategoryName(), category.getBookCount()));
            borrowSeries.getData().add(new XYChart.Data<>(category.getCategoryName(), category.getBorrowCount()));
        }

        chart.getData().addAll(bookSeries, borrowSeries);

        bookSeries.getNode().setStyle("-fx-bar-fill: #722ED1;");
        borrowSeries.getNode().setStyle("-fx-bar-fill: #FAAD14;");

        categoryChartBox.getChildren().add(chart);
    }

    private void loadBorrowRank() {
        String token = getToken();
        if (token == null) return;

        Request request = new Request.Builder()
                .url(RANK_URL)
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                List<HotBook> books = gson.fromJson(
                        response.body().string(),
                        new TypeToken<List<HotBook>>(){}.getType()
                );
                Platform.runLater(() -> renderBorrowRank(books));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderBorrowRank(List<HotBook> books) {
        borrowRankBox.getChildren().clear();

        Label title = new Label("借阅排行");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 16 0;");
        borrowRankBox.getChildren().add(title);

        for (int i = 0; i < Math.min(books.size(), 10); i++) {
            HotBook book = books.get(i);
            VBox item = new VBox();
            item.setStyle("-fx-padding: 16px 0; -fx-border-bottom: 1px solid #f0f0f0;");

            javafx.scene.layout.HBox content = new javafx.scene.layout.HBox(16);
            content.setStyle("-fx-alignment: CENTER_LEFT;");

            Label rank = new Label(String.valueOf(i + 1));
            rank.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #999; -fx-min-width: 40px;");

            if (i == 0) {
                rank.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #5B8FF9; -fx-min-width: 40px;");
            }

            VBox bookInfo = new VBox(8);
            bookInfo.setStyle("-fx-alignment: CENTER_LEFT;");

            Label bookTitle = new Label(book.getTitle());
            bookTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Label author = new Label(book.getAuthor());
            author.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

            bookInfo.getChildren().addAll(bookTitle, author);

            VBox countBox = new VBox(4);
            countBox.setStyle("-fx-alignment: CENTER_RIGHT;");
            javafx.scene.layout.HBox.setHgrow(countBox, javafx.scene.layout.Priority.ALWAYS);

            Label count = new Label(String.valueOf(book.getBorrowCount()));
            count.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #5B8FF9;");

            Label unit = new Label("次");
            unit.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");

            countBox.getChildren().addAll(count, unit);

            content.getChildren().addAll(rank, bookInfo, countBox);
            item.getChildren().add(content);
            borrowRankBox.getChildren().add(item);
        }
    }
}
