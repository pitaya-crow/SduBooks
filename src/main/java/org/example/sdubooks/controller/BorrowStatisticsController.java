package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
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
import java.util.List;

public class BorrowStatisticsController extends BaseController {

    @FXML private Label totalBorrowLabel;
    @FXML private Label totalReturnLabel;
    @FXML private Label growthRateLabel;

    @FXML private VBox trendChartBox;
    @FXML private VBox categoryChartBox;
    @FXML private VBox borrowRankBox;

    private static final String BASE_URL = "http://localhost:8081/api/admin";
    private static final String STATS_URL = BASE_URL + "/borrows/stats";
    private static final String TREND_URL = BASE_URL + "/borrow/trend";
    private static final String CATEGORY_URL = BASE_URL + "/borrow/category";
    private static final String RANK_URL = BASE_URL + "/dashboard/hot-books"; // 借阅排行复用热门书籍接口

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

    /**
     * 从包装响应中提取 data 字段
     */
    private String extractData(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json.has("data")) {
                return json.get("data").toString();
            }
        } catch (Exception ignored) {}
        return responseBody;
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
                String respBody = response.body().string();
                System.out.println("[DEBUG] 借阅统计响应: " + respBody);
                BorrowStats stats = gson.fromJson(extractData(respBody), BorrowStats.class);
                if (stats != null) {
                    Platform.runLater(() -> {
                        totalBorrowLabel.setText(String.valueOf(stats.getTotalBorrows()));
                        totalReturnLabel.setText(String.valueOf(stats.getReturnedBooks()));
                        growthRateLabel.setText(String.valueOf(stats.getThisMonthBooks()));
                        growthRateLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] loadStats异常: " + e.getMessage());
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
                String respBody = response.body().string();
                List<BorrowTrend> trends = gson.fromJson(
                        extractData(respBody),
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
        if (trends == null || trends.isEmpty()) {
            trendChartBox.getChildren().add(new Label("暂无数据"));
            return;
        }

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

        for (BorrowTrend trend : trends) {
            String month = trend.getDate() != null ? trend.getDate() : "";
            Integer borrowCount = trend.getBorrowCount() != null ? trend.getBorrowCount() : 0;
            Integer returnCount = trend.getReturnCount() != null ? trend.getReturnCount() : 0;
            borrowSeries.getData().add(new XYChart.Data<>(month, borrowCount));
            returnSeries.getData().add(new XYChart.Data<>(month, returnCount));
        }

        chart.getData().addAll(borrowSeries, returnSeries);

        // 设置线条样式（需要延迟执行，否则节点可能为 null）
        Platform.runLater(() -> {
            try {
                if (borrowSeries.getNode() != null) {
                    borrowSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #5B8FF9; -fx-stroke-width: 3px;");
                }
                if (returnSeries.getNode() != null) {
                    returnSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #5AD8A6; -fx-stroke-width: 3px;");
                }
            } catch (Exception ignored) {}
        });

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
                String respBody = response.body().string();
                List<CategoryStats> categories = gson.fromJson(
                        extractData(respBody),
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
        if (categories == null || categories.isEmpty()) {
            categoryChartBox.getChildren().add(new Label("暂无数据"));
            return;
        }

        PieChart pieChart = new PieChart();
        pieChart.setTitle("分类统计");
        pieChart.setLabelsVisible(true);
        pieChart.setLabelLineLength(15);
        pieChart.setLegendVisible(true);
        pieChart.setStyle("-fx-font-size: 14px;");

        // 饼图颜色
        String[] colors = {"#5B8FF9", "#5AD8A6", "#F6BD16", "#E86452", "#6DC8EC", "#945FB9", "#FF9845", "#1E9493"};

        int colorIndex = 0;
        for (CategoryStats category : categories) {
            String name = category.getCategoryName();
            int count = category.getBookCount();
            if (name != null && count > 0) {
                PieChart.Data data = new PieChart.Data(name + " (" + count + ")", count);
                pieChart.getData().add(data);
            }
        }

        if (pieChart.getData().isEmpty()) {
            categoryChartBox.getChildren().add(new Label("暂无数据"));
            return;
        }

        // 设置颜色
        Platform.runLater(() -> {
            int ci = 0;
            for (PieChart.Data data : pieChart.getData()) {
                data.getNode().setStyle("-fx-pie-color: " + colors[ci % colors.length] + ";");
                ci++;
            }
        });

        categoryChartBox.getChildren().add(pieChart);
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
                String respBody = response.body().string();
                List<HotBook> books = gson.fromJson(
                        extractData(respBody),
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
        if (books == null || books.isEmpty()) {
            borrowRankBox.getChildren().add(new Label("暂无数据"));
            return;
        }

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
