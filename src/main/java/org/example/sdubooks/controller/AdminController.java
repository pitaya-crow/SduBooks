package org.example.sdubooks.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
    private static final String DASHBOARD_FXML = "/org/example/sdubooks/dashboard.fxml";
    private static final String BOOK_MANAGE_FXML = "/org/example/sdubooks/book-management.fxml";
    private static final String USER_MANAGE_FXML = "/org/example/sdubooks/user-management.fxml";
    private static final String BORROW_STATS_FXML = "/org/example/sdubooks/borrow-statistics.fxml";

    @Override
    protected Stage getCurrentStage() {
        return (Stage) contentPane.getScene().getWindow();
    }

    @FXML
    public void initialize() {
        // 进入管理后台时自动加载仪表板
        switchToDashboard();
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

    // 加载指定 FXML 到 contentPane（支持任意根节点类型）
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            AnchorPane.setTopAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0);
            AnchorPane.setLeftAnchor(page, 0.0);
            AnchorPane.setRightAnchor(page, 0.0);
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
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12 16; -fx-background-color: #ede9fe; -fx-text-fill: #5a3cff; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-cursor: hand;" :
                        "-fx-alignment: CENTER_LEFT; -fx-padding: 12 16; -fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 14px; -fx-background-radius: 10; -fx-cursor: hand;");
                btn.setMaxWidth(Double.MAX_VALUE);
            }
        });
    }
}