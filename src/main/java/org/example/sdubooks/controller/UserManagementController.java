package org.example.sdubooks.controller;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.sdubooks.model.PageResponse;
import org.example.sdubooks.model.User;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserManagementController extends BaseController {

    // UI组件
    @FXML private TextField searchField;
    @FXML private TableView<User> usersTable;
    @FXML private Label totalUsersLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private CheckBox selectAllCheckbox;

    // 数据
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private List<User> selectedUsers = new ArrayList<>();
    private int currentPage = 0;
    private int pageSize = 10;
    private int totalPages = 1;
    private long totalElements = 0;

    // 接口路径
    private static final String BASE_URL = "http://10.27.241.94:8081/api/admin";
    private static final String USERS_URL = BASE_URL + "/admin/users";
    private static final String UPDATE_STATUS_URL = BASE_URL + "/admin/users/%d/status";
    private static final String DELETE_USER_URL = BASE_URL + "/admin/users/%d";

    @FXML
    public void initialize() {
        // 初始化表格列
        setupTableColumns();

        // 加载用户列表
        loadUsers(currentPage);
    }

    // 设置表格列
    private void setupTableColumns() {
        // 用户名列
        TableColumn<User, String> usernameCol = new TableColumn<>("用户名");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(150);

        // 邮箱列
        TableColumn<User, String> emailCol = new TableColumn<>("邮箱");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(200);

        // 角色列
        TableColumn<User, String> roleCol = new TableColumn<>("角色");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setCellFactory(column -> {
            TableCell<User, String> cell = new TableCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.setStyle("-fx-alignment: CENTER;");
            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                if ("ADMIN".equals(newVal)) {
                    cell.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff5722; -fx-alignment: CENTER;");
                } else {
                    cell.setStyle("-fx-text-fill: #333; -fx-alignment: CENTER;");
                }
            });
            return cell;
        });
        roleCol.setPrefWidth(100);

        // 状态列（使用自定义单元格工厂）
        TableColumn<User, Boolean> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        statusCol.setCellFactory(column -> new TableCell<>() {
            private final ToggleButton toggleBtn = new ToggleButton();

            {
                toggleBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    toggleUserStatus(user);
                });
            }

            @Override
            protected void updateItem(Boolean enabled, boolean empty) {
                super.updateItem(enabled, empty);
                if (empty || enabled == null) {
                    setGraphic(null);
                } else {
                    toggleBtn.setSelected(enabled);
                    toggleBtn.setText(enabled ? "✓ 启用" : "✗ 禁用");
                    toggleBtn.setStyle(enabled ?
                            "-fx-background-color: #4CAF50; -fx-text-fill: white;" :
                            "-fx-background-color: #f44336; -fx-text-fill: white;");
                    setGraphic(toggleBtn);
                }
            }
        });
        statusCol.setPrefWidth(120);

        // 操作列（只有删除按钮）
        TableColumn<User, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");

            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
        actionCol.setPrefWidth(100);

        // 添加所有列到表格
        usersTable.getColumns().clear();
        usersTable.getColumns().addAll(usernameCol, emailCol, roleCol, statusCol, actionCol);
        usersTable.setItems(userList);
    }

    // ==================== 数据加载 ====================

    // 加载用户列表
    private void loadUsers(int page) {
        String token = getToken();
        if (token == null) {
            showAlert("错误", "未登录，请重新登录", Alert.AlertType.ERROR);
            return;
        }

        String url = String.format("%s?page=%d&size=%d", USERS_URL, page, pageSize);
        if (!searchField.getText().trim().isEmpty()) {
            url += "&keyword=" + searchField.getText().trim();
        }
        final String finalUrl = url;

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(finalUrl)
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        PageResponse<User> pageResponse = gson.fromJson(
                                response.body().string(),
                                new TypeToken<PageResponse<User>>(){}.getType()
                        );

                        Platform.runLater(() -> {
                            userList.setAll(pageResponse.getContent());
                            totalElements = pageResponse.getTotalElements();
                            totalPages = pageResponse.getTotalPages();
                            currentPage = pageResponse.getPageNumber();
                            totalUsersLabel.setText(String.valueOf(totalElements));
                            updatePageInfo();
                            updatePageButtons();
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("错误", "加载用户列表失败: " + response.message(), Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    // ==================== 状态切换 ====================

    // 切换用户状态（启用/禁用）
    public void toggleUserStatus(User user) {
        String token = getToken();
        if (token == null) {
            showAlert("错误", "未登录，请重新登录", Alert.AlertType.ERROR);
            return;
        }

        String url = String.format(UPDATE_STATUS_URL, user.getId());
        boolean newStatus = !user.getEnabled();

        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(
                        gson.toJson(newStatus),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .put(body)
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            user.setEnabled(newStatus);
                            usersTable.refresh();
                            showAlert("成功", "用户状态已更新", Alert.AlertType.INFORMATION);
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("错误", "更新用户状态失败: " + response.message(), Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    // ==================== 删除操作 ====================

    // 删除单个用户
    public void deleteUser(User user) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认删除");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText(String.format("确定要删除用户 \"%s\" 吗？此操作不可恢复！", user.getUsername()));

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performDeleteUser(user.getId());
            }
        });
    }

    // 批量删除
    @FXML
    private void handleBatchDelete() {
        if (selectedUsers.isEmpty()) {
            showAlert("提示", "请先选择要删除的用户", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认批量删除");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText(String.format("确定要删除选中的 %d 个用户吗？此操作不可恢复！", selectedUsers.size()));

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                for (User user : selectedUsers) {
                    performDeleteUser(user.getId());
                }
                selectedUsers.clear();
                selectAllCheckbox.setSelected(false);
            }
        });
    }

    // 执行删除
    private void performDeleteUser(Long userId) {
        String token = getToken();
        if (token == null) {
            showAlert("错误", "未登录，请重新登录", Alert.AlertType.ERROR);
            return;
        }

        String url = String.format(DELETE_USER_URL, userId);

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .delete()
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            loadUsers(currentPage); // 重新加载当前页
                            showAlert("成功", "用户已删除", Alert.AlertType.INFORMATION);
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("错误", "删除用户失败: " + response.message(), Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert("错误", "网络请求失败: " + e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    // ==================== 搜索功能 ====================

    @FXML
    private void handleSearch() {
        currentPage = 0; // 搜索时重置到第一页
        loadUsers(currentPage);
    }

    // ==================== 分页功能 ====================

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 0) {
            loadUsers(currentPage - 1);
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            loadUsers(currentPage + 1);
        }
    }

    private void updatePageInfo() {
        pageInfoLabel.setText(String.format("%d/%d", currentPage + 1, totalPages));
    }

    private void updatePageButtons() {
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= totalPages - 1);
    }

    // ==================== 全选功能 ====================

    @FXML
    private void handleSelectAll() {
        boolean isSelected = selectAllCheckbox.isSelected();
        selectedUsers.clear();
        if (isSelected) {
            selectedUsers.addAll(userList);
        }
        // 更新批量删除按钮状态
        // 这里需要在 FXML 中添加对批量删除按钮的引用
    }
}