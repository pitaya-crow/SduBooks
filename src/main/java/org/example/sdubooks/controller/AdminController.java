package org.example.sdubooks.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminController extends BaseController {

    @FXML private AnchorPane contentPane;
    @FXML private VBox leftNav;

    // 页面路径映射（保持整洁）
    private static final String DASHBOARD_FXML = "/org/example/sdubooks/admin/dashboard.fxml";
    private static final String BOOK_MANAGE_FXML = "/org/example/sdubooks/admin/book-management.fxml";
    private static final String USER_MANAGE_FXML = "/org/example/sdubooks/admin/user-management.fxml";
    private static final String BORROW_STATS_FXML = "/org/example/sdubooks/admin/borrow-statistics.fxml";

    @Override
    protected Stage getCurrentStage() {
        return (Stage) contentPane.getScene().getWindow();
    }

    // ==================== 导航切换方法 ====================
    @FXML
    private void switchToDashboard() {
        loadContent(DASHBOARD_FXML);
        updateActiveButton("dashboardBtn");
    }

    @FXML
    private void switchToBookManagement() {
        loadContent(BOOK_MANAGE_FXML);
        updateActiveButton("bookBtn");
    }

    @FXML
    private void switchToUserManagement() {
        loadContent(USER_MANAGE_FXML);
        updateActiveButton("userBtn");
    }

    @FXML
    private void switchToBorrowStatistics() {
        loadContent(BORROW_STATS_FXML);
        updateActiveButton("statBtn");
    }

    // 加载指定 FXML 到 contentPane
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane page = loader.load();
            contentPane.getChildren().setAll(page);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "无法加载页面：" + fxmlPath, Alert.AlertType.ERROR);
        }
    }

    // 高亮当前选中按钮，取消其他按钮高亮
    private void updateActiveButton(String activeId) {
        leftNav.getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                boolean isActive = btn.getId() != null && btn.getId().equals(activeId);
                btn.setStyle(isActive ?
                        "-fx-alignment: LEFT; -fx-padding: 14px 20px; -fx-background-color: #f0f0ff; -fx-text-fill: #5a3cff; -fx-font-weight: bold;" :
                        "-fx-alignment: LEFT; -fx-padding: 14px 20px; -fx-background-color: transparent; -fx-text-fill: #333;");
            }
        });
    }
}