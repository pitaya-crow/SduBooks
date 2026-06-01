package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.sdubooks.model.User;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManagementController extends BaseController {

    @FXML private TextField searchField;
    @FXML private TableView<User> usersTable;
    @FXML private Label totalUsersLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private CheckBox selectAllCheckbox;
    @FXML private Button batchDeleteBtn;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private List<User> selectedUsers = new ArrayList<>();
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 1;
    private long totalElements = 0;

    private static final String BASE_URL = "http://localhost:8081/api/admin";
    private static final String USERS_URL = BASE_URL + "/users";
    private static final String UPDATE_STATUS_URL = BASE_URL + "/user/%d/status";
    private static final String DELETE_USER_URL = BASE_URL + "/user/%d";
    private static final String USER_BORROWS_URL = BASE_URL + "/user/%d/borrows";
    private static final String RETURN_URL = BASE_URL + "/borrow/%d/return";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double OVERDUE_RATE = 0.1; // 每天0.1元

    @FXML
    public void initialize() {
        setupTableColumns();
        loadUsers(currentPage);
    }

    // ==================== 表格列配置 ====================
    private void setupTableColumns() {
        // 复选框列
        TableColumn<User, Boolean> checkCol = new TableColumn<>("");
        checkCol.setCellValueFactory(data -> {
            User u = data.getValue();
            boolean selected = selectedUsers.contains(u);
            return new SimpleBooleanProperty(selected);
        });
        checkCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    if (cb.isSelected()) {
                        if (!selectedUsers.contains(user)) selectedUsers.add(user);
                    } else {
                        selectedUsers.remove(user);
                    }
                    updateBatchDeleteBtn();
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    cb.setSelected(selectedUsers.contains(user));
                    setGraphic(cb);
                }
            }
        });
        checkCol.setPrefWidth(40);
        checkCol.setResizable(false);

        // 用户名列
        TableColumn<User, String> usernameCol = new TableColumn<>("用户名");
        usernameCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUserName() != null ? d.getValue().getUserName() : ""));
        usernameCol.setPrefWidth(150);

        // 角色列
        TableColumn<User, String> roleCol = new TableColumn<>("角色");
        roleCol.setCellValueFactory(d -> {
            Integer typeId = d.getValue().getUserTypeId();
            return new SimpleStringProperty(typeId != null && typeId == 2 ? "ADMIN" : "USER");
        });
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setText(null); setStyle(""); }
                else {
                    setText(role);
                    setStyle("ADMIN".equals(role) ?
                            "-fx-font-weight:bold;-fx-text-fill:#ff5722;-fx-alignment:CENTER;" :
                            "-fx-text-fill:#333;-fx-alignment:CENTER;");
                }
            }
        });
        roleCol.setPrefWidth(80);

        // 状态列
        TableColumn<User, Boolean> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(d -> new SimpleBooleanProperty(
                d.getValue().getStatus() != null && d.getValue().getStatus() == 1));
        statusCol.setCellFactory(col -> new TableCell<>() {
            private final ToggleButton btn = new ToggleButton();
            { btn.setOnAction(e -> toggleUserStatus(getTableView().getItems().get(getIndex()))); }
            @Override
            protected void updateItem(Boolean enabled, boolean empty) {
                super.updateItem(enabled, empty);
                if (empty || enabled == null) { setGraphic(null); return; }
                btn.setSelected(enabled);
                btn.setText(enabled ? "✓ 启用" : "✗ 禁用");
                btn.setStyle(enabled ?
                        "-fx-background-color:#4CAF50;-fx-text-fill:white;" :
                        "-fx-background-color:#f44336;-fx-text-fill:white;");
                setGraphic(btn);
            }
        });
        statusCol.setPrefWidth(100);

        // 操作列（查看借阅 + 删除）
        TableColumn<User, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button borrowBtn = new Button("📖 借阅管理");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox box = new HBox(6, borrowBtn, deleteBtn);
            {
                borrowBtn.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-font-size:12px;");
                deleteBtn.setStyle("-fx-background-color:#f44336;-fx-text-fill:white;-fx-font-size:12px;");
                borrowBtn.setOnAction(e -> showUserBorrows(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        actionCol.setPrefWidth(160);

        usersTable.getColumns().clear();
        usersTable.getColumns().addAll(checkCol, usernameCol, roleCol, statusCol, actionCol);
        usersTable.setItems(userList);
    }

    // ==================== 数据加载 ====================
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

    private void loadUsers(int page) {
        String token = getToken();
        if (token == null) { showAlert("错误", "未登录", Alert.AlertType.ERROR); return; }

        String keyword = searchField.getText().trim();
        String url = USERS_URL;
        if (!keyword.isEmpty()) {
            url += "?keyword=" + keyword;
        }

        final String finalUrl = url;
        new Thread(() -> {
            try {
                Request req = new Request.Builder().url(finalUrl)
                        .header("Authorization", "Bearer " + token).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    System.out.println("[DEBUG] 用户列表: code=" + resp.code() + " body=" + body);
                    if (resp.isSuccessful()) {
                        List<User> all = gson.fromJson(extractData(body),
                                new TypeToken<List<User>>(){}.getType());
                        if (all == null) all = new ArrayList<>();

                        final List<User> finalAll = all;
                        int size = all.size();
                        int from = Math.min(page * pageSize, size);
                        int to = Math.min(from + pageSize, size);
                        List<User> sub = all.subList(from, to);
                        int tp = Math.max((int) Math.ceil((double) size / pageSize), 1);

                        final int fTp = tp;
                        final List<User> fSub = sub;
                        Platform.runLater(() -> {
                            selectedUsers.clear();
                            userList.setAll(fSub);
                            totalElements = size;
                            totalPages = fTp;
                            currentPage = page;
                            totalUsersLabel.setText(String.valueOf(totalElements));
                            updatePageInfo();
                            updatePageButtons();
                            updateBatchDeleteBtn();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("错误", "加载失败: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    // ==================== 用户借阅记录弹窗 ====================
    private void showUserBorrows(User user) {
        String token = getToken();
        if (token == null) return;

        String url = String.format(USER_BORROWS_URL, user.getPersonId());
        new Thread(() -> {
            try {
                Request req = new Request.Builder().url(url)
                        .header("Authorization", "Bearer " + token).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    System.out.println("[DEBUG] 用户借阅: " + body);
                    if (resp.isSuccessful()) {
                        List<Map<String, Object>> records = gson.fromJson(extractData(body),
                                new TypeToken<List<Map<String, Object>>>(){}.getType());
                        final List<Map<String, Object>> finalRecords = records != null ? records : new ArrayList<>();
                        Platform.runLater(() -> showBorrowDialog(user, finalRecords));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void showBorrowDialog(User user, List<Map<String, Object>> records) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("借阅记录 — " + user.getUserName());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(700);

        VBox container = new VBox(12);
        container.setPadding(new Insets(20));

        double totalFee = 0;

        if (records.isEmpty()) {
            container.getChildren().add(new Label("该用户暂无借阅记录"));
        } else {
            for (Map<String, Object> rec : records) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:10;-fx-padding:12;");

                String bookTitle = rec.get("bookTitle") != null ? rec.get("bookTitle").toString() : "未知";
                String borrowedAt = rec.get("borrowedAt") != null ? rec.get("borrowedAt").toString().replace("T", " ").substring(0, Math.min(16, rec.get("borrowedAt").toString().length())) : "";
                String dueAt = rec.get("dueAt") != null ? rec.get("dueAt").toString().replace("T", " ").substring(0, Math.min(16, rec.get("dueAt").toString().length())) : "";
                String returnedAt = rec.get("returnedAt") != null ? rec.get("returnedAt").toString().replace("T", " ").substring(0, Math.min(16, rec.get("returnedAt").toString().length())) : "";
                Object statusObj = rec.get("status");
                int status = statusObj instanceof Number ? ((Number) statusObj).intValue() : 0;
                Object idObj = rec.get("id");
                int borrowId = idObj instanceof Number ? ((Number) idObj).intValue() : 0;

                boolean returned = status == 0 || (returnedAt != null && !returnedAt.isEmpty());

                // 书名
                Label titleLbl = new Label("📖 " + bookTitle);
                titleLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

                // 日期信息
                Label dateLbl = new Label("借阅: " + borrowedAt + "  |  应还: " + dueAt);
                dateLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;");

                HBox bottomRow = new HBox(10);
                bottomRow.setAlignment(Pos.CENTER_LEFT);

                if (returned) {
                    Label returnedLbl = new Label("✅ 已归还" + (!returnedAt.isEmpty() ? " (" + returnedAt + ")" : ""));
                    returnedLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#22c55e;-fx-font-weight:bold;");
                    bottomRow.getChildren().add(returnedLbl);
                } else {
                    // 计算逾期
                    double fee = 0;
                    long overdueDays = 0;
                    try {
                        LocalDateTime due = LocalDateTime.parse(dueAt.replace(" ", "T"));
                        LocalDateTime now = LocalDateTime.now();
                        if (now.isAfter(due)) {
                            overdueDays = ChronoUnit.DAYS.between(due, now);
                            fee = overdueDays * OVERDUE_RATE;
                        }
                    } catch (Exception ignored) {}

                    if (overdueDays > 0) {
                        Label overdueLbl = new Label("⚠️ 逾期 " + overdueDays + " 天，欠费 ¥" + String.format("%.1f", fee));
                        overdueLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#ef4444;-fx-font-weight:bold;");
                        bottomRow.getChildren().add(overdueLbl);
                        totalFee += fee;
                    } else {
                        Label okLbl = new Label("📗 借阅中");
                        okLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#3b82f6;");
                        bottomRow.getChildren().add(okLbl);
                    }

                    // 归还按钮
                    Button returnBtn = new Button("确认归还");
                    returnBtn.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-background-radius:6;");
                    returnBtn.setOnAction(e -> {
                        performReturn(borrowId, dialog, user);
                    });
                    bottomRow.getChildren().add(returnBtn);
                }

                card.getChildren().addAll(titleLbl, dateLbl, bottomRow);
                container.getChildren().add(card);
            }
        }

        // 总欠费
        if (totalFee > 0) {
            Label feeLabel = new Label("💰 总逾期欠费: ¥" + String.format("%.1f", totalFee));
            feeLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#ef4444;-fx-padding:10 0 0 0;");
            container.getChildren().add(feeLabel);

            Button clearFeeBtn = new Button("确认还清");
            clearFeeBtn.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:8 20;");
            clearFeeBtn.setOnAction(e -> {
                showAlert("确认", "已标记为还清", Alert.AlertType.INFORMATION);
                dialog.close();
            });
            container.getChildren().add(clearFeeBtn);
        }

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setPrefHeight(400);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }

    private void performReturn(int borrowId, Dialog<?> dialog, User user) {
        String token = getToken();
        if (token == null) return;

        String url = String.format(RETURN_URL, borrowId);
        new Thread(() -> {
            try {
                Request req = new Request.Builder().url(url)
                        .header("Authorization", "Bearer " + token)
                        .put(RequestBody.create("", MediaType.get("application/json")))
                        .build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    System.out.println("[DEBUG] 归还响应: code=" + resp.code() + " body=" + body);
                    if (resp.isSuccessful()) {
                        Platform.runLater(() -> {
                            showAlert("成功", "归还成功", Alert.AlertType.INFORMATION);
                            dialog.close();
                            showUserBorrows(user); // 刷新
                        });
                    } else {
                        Platform.runLater(() -> showAlert("失败", "归还失败: " + body, Alert.AlertType.ERROR));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("错误", "网络异常: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    // ==================== 状态切换 ====================
    public void toggleUserStatus(User user) {
        String token = getToken();
        if (token == null) return;
        String url = String.format(UPDATE_STATUS_URL, user.getPersonId());
        boolean newStatus = !user.getEnabled();

        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(gson.toJson(newStatus), MediaType.get("application/json"));
                Request req = new Request.Builder().url(url).put(body)
                        .header("Authorization", "Bearer " + token).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) {
                        Platform.runLater(() -> {
                            user.setStatus(newStatus ? 1 : 0);
                            usersTable.refresh();
                            showAlert("成功", "状态已更新", Alert.AlertType.INFORMATION);
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ==================== 删除 ====================
    public void deleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setContentText("确定删除用户 \"" + user.getUserName() + "\" ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) performDelete(user.getPersonId().longValue());
        });
    }

    @FXML
    private void handleBatchDelete() {
        if (selectedUsers.isEmpty()) { showAlert("提示", "请先选择用户", Alert.AlertType.WARNING); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("批量删除");
        alert.setContentText("确定删除选中的 " + selectedUsers.size() + " 个用户?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                for (User u : new ArrayList<>(selectedUsers)) performDelete(u.getPersonId().longValue());
                selectedUsers.clear();
                selectAllCheckbox.setSelected(false);
                updateBatchDeleteBtn();
            }
        });
    }

    private void performDelete(Long userId) {
        String token = getToken();
        if (token == null) return;
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(String.format(DELETE_USER_URL, userId)).delete()
                        .header("Authorization", "Bearer " + token).build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful()) {
                        Platform.runLater(() -> { loadUsers(currentPage); showAlert("成功", "已删除", Alert.AlertType.INFORMATION); });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ==================== 搜索 / 分页 / 全选 ====================
    @FXML private void handleSearch() { currentPage = 0; loadUsers(currentPage); }
    @FXML private void handlePreviousPage() { if (currentPage > 0) loadUsers(currentPage - 1); }
    @FXML private void handleNextPage() { if (currentPage < totalPages - 1) loadUsers(currentPage + 1); }

    @FXML
    private void handleSelectAll() {
        boolean sel = selectAllCheckbox.isSelected();
        selectedUsers.clear();
        if (sel) selectedUsers.addAll(userList);
        usersTable.refresh();
        updateBatchDeleteBtn();
    }

    private void updatePageInfo() { pageInfoLabel.setText((currentPage + 1) + "/" + totalPages); }
    private void updatePageButtons() {
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= totalPages - 1);
    }
    private void updateBatchDeleteBtn() {
        if (batchDeleteBtn != null) batchDeleteBtn.setDisable(selectedUsers.isEmpty());
    }
}
