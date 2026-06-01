package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import okhttp3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OverdueManagementController extends BaseController {

    @FXML private Label overdueCountLabel;
    @FXML private Label totalFeeLabel;
    @FXML private VBox overdueListBox;

    private static final String BASE_URL = "http://localhost:8081/api/admin";
    private static final String OVERDUE_URL = BASE_URL + "/overdue";
    private static final String CLEAR_URL = BASE_URL + "/overdue/%d/clear";

    @FXML
    public void initialize() {
        loadOverdueRecords();
    }

    private String extractData(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return "[]";
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json != null && json.has("data") && !json.get("data").isJsonNull()) {
                return json.get("data").toString();
            }
        } catch (Exception ignored) {}
        return responseBody;
    }

    @SuppressWarnings("unchecked")
    private void loadOverdueRecords() {
        String token = getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(OVERDUE_URL)
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    System.out.println("[DEBUG] 逾期记录响应: code=" + response.code() + " body=" + body);
                    if (response.isSuccessful()) {
                        List<Map<String, Object>> records = gson.fromJson(extractData(body),
                                new TypeToken<List<Map<String, Object>>>(){}.getType());
                        final List<Map<String, Object>> finalRecords = records != null ? records : new ArrayList<>();
                        Platform.runLater(() -> renderOverdueList(finalRecords));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderOverdueList(List<Map<String, Object>> records) {
        overdueListBox.getChildren().clear();

        int totalRecords = records.size();
        double totalFee = 0;
        for (Map<String, Object> rec : records) {
            Object feeObj = rec.get("fee");
            if (feeObj instanceof Number) totalFee += ((Number) feeObj).doubleValue();
        }

        overdueCountLabel.setText(String.valueOf(totalRecords));
        totalFeeLabel.setText("¥" + String.format("%.1f", totalFee));

        if (records.isEmpty()) {
            Label emptyLabel = new Label("暂无逾期记录");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-padding: 40;");
            overdueListBox.getChildren().add(emptyLabel);
            return;
        }

        for (Map<String, Object> rec : records) {
            overdueListBox.getChildren().add(createOverdueCard(rec));
        }
    }

    private VBox createOverdueCard(Map<String, Object> rec) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 1, 0);");

        String userName = rec.get("userName") != null ? rec.get("userName").toString() : "未知";
        String bookTitle = rec.get("bookTitle") != null ? rec.get("bookTitle").toString() : "未知";
        Object daysObj = rec.get("overdueDays");
        long overdueDays = daysObj instanceof Number ? ((Number) daysObj).longValue() : 0;
        Object feeObj = rec.get("fee");
        double fee = feeObj instanceof Number ? ((Number) feeObj).doubleValue() : 0;
        String dueAt = rec.get("dueAt") != null ? rec.get("dueAt").toString().replace("T", " ").substring(0, Math.min(16, rec.get("dueAt").toString().length())) : "";
        Object idObj = rec.get("id");
        int borrowId = idObj instanceof Number ? ((Number) idObj).intValue() : 0;

        // 顶部：书名 + 逾期天数
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label bookLabel = new Label("📖 " + bookTitle);
        bookLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(bookLabel, Priority.ALWAYS);
        Label daysLabel = new Label("逾期 " + overdueDays + " 天");
        daysLabel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-font-size: 13px; -fx-padding: 4 12; -fx-background-radius: 10; -fx-font-weight: bold;");
        topRow.getChildren().addAll(bookLabel, daysLabel);

        // 中间：借阅人 + 应还日期
        HBox midRow = new HBox(20);
        midRow.setAlignment(Pos.CENTER_LEFT);
        Label userLabel = new Label("👤 借阅人: " + userName);
        userLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
        Label dueLabel = new Label("📅 应还: " + dueAt);
        dueLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        midRow.getChildren().addAll(userLabel, dueLabel);

        // 底部：欠费 + 操作按钮
        HBox bottomRow = new HBox(15);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label feeLabel = new Label("💰 应缴欠费: ¥" + String.format("%.1f", fee));
        feeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        HBox.setHgrow(feeLabel, Priority.ALWAYS);

        Button clearBtn = new Button("✅ 确认还清");
        clearBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> handleClearOverdue(borrowId, userName, bookTitle));

        Button returnBtn = new Button("📚 确认归还");
        returnBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        returnBtn.setOnAction(e -> handleReturnBook(borrowId, userName, bookTitle));

        bottomRow.getChildren().addAll(feeLabel, returnBtn, clearBtn);

        card.getChildren().addAll(topRow, midRow, bottomRow);
        return card;
    }

    private void handleReturnBook(int borrowId, String userName, String bookTitle) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认归还");
        confirm.setContentText("确认 " + userName + " 已归还《" + bookTitle + "》？");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performReturn(borrowId);
            }
        });
    }

    private void handleClearOverdue(int borrowId, String userName, String bookTitle) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认还清");
        confirm.setContentText("确认 " + userName + " 已还清《" + bookTitle + "》的逾期欠费并归还？");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performReturn(borrowId);
            }
        });
    }

    private void performReturn(int borrowId) {
        String token = getToken();
        if (token == null) return;

        String url = String.format(CLEAR_URL, borrowId);
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .put(RequestBody.create("", MediaType.get("application/json")))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    System.out.println("[DEBUG] 还清响应: code=" + response.code() + " body=" + body);
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            showAlert("成功", "已确认归还/还清", Alert.AlertType.INFORMATION);
                            loadOverdueRecords(); // 刷新列表
                        });
                    } else {
                        Platform.runLater(() -> showAlert("失败", "操作失败: " + body, Alert.AlertType.ERROR));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("错误", "网络异常: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }
}
