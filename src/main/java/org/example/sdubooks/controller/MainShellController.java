package org.example.sdubooks.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户端主框架控制器 — 导航栏持久化，只切换内容区，缓存页面避免闪烁
 */
public class MainShellController extends BaseController {

    @FXML private HBox navBar;
    @FXML private StackPane contentPane;
    @FXML private Button btnHome;
    @FXML private Button btnRankings;
    @FXML private Button btnProfile;

    // 页面缓存：fxmlPath -> 已加载的 Node
    private final Map<String, Node> pageCache = new HashMap<>();

    @FXML
    public void initialize() {
        switchToPage("/org/example/sdubooks/homepage-view.fxml", btnHome);
    }

    @Override
    protected void handleHome() {
        switchToPage("/org/example/sdubooks/homepage-view.fxml", btnHome);
    }

    @Override
    protected void handleRankings() {
        switchToPage("/org/example/sdubooks/ranking.fxml", btnRankings);
    }

    @Override
    protected void handleProfile() {
        switchToPage("/org/example/sdubooks/self-center.fxml", btnProfile);
    }

    /**
     * 公开方法：加载子页面到内容区（供其他控制器调用，不缓存）
     */
    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentPane.getChildren().setAll(page);
            updateNavButtons(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除指定页面的缓存（下次切换时会重新加载）
     */
    public void invalidateCache(String fxmlPath) {
        pageCache.remove(fxmlPath);
    }

    /**
     * 加载子页面到内容区，高亮对应导航按钮，使用缓存避免闪烁
     */
    private void switchToPage(String fxmlPath, Button activeBtn) {
        try {
            Node page = pageCache.get(fxmlPath);
            if (page == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                page = loader.load();
                pageCache.put(fxmlPath, page);
            }
            contentPane.getChildren().setAll(page);
            updateNavButtons(activeBtn);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("错误", "无法加载页面: " + fxmlPath, javafx.scene.control.Alert.AlertType.ERROR);
        }
    }

    private void updateNavButtons(Button activeBtn) {
        String activeStyle = "-fx-background-color: transparent; -fx-font-size: 15px; -fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-font-size: 15px; -fx-text-fill: #64748b; -fx-cursor: hand;";

        btnHome.setStyle(inactiveStyle);
        btnRankings.setStyle(inactiveStyle);
        btnProfile.setStyle(inactiveStyle);

        if (activeBtn != null) {
            activeBtn.setStyle(activeStyle);
        }
    }

    @Override
    protected Stage getCurrentStage() {
        if (navBar != null && navBar.getScene() != null) {
            return (Stage) navBar.getScene().getWindow();
        }
        return null;
    }
}
