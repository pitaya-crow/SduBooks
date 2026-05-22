package org.example.sdubooks.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class AdminController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button bookBtn;

    @FXML
    private Button userBtn;

    @FXML
    private Button borrowBtn;

    // 选中按钮样式
    private final String ACTIVE_STYLE =
            "-fx-background-color:#f3e8ff;" +
                    "-fx-text-fill:#9333ea;" +
                    "-fx-font-size:26px;" +
                    "-fx-background-radius:20;" +
                    "-fx-border-radius:20;" +
                    "-fx-border-color:#d8b4fe;" +
                    "-fx-border-width:2;" +
                    "-fx-alignment:CENTER-LEFT;" +
                    "-fx-padding:0 0 0 30;" +
                    "-fx-cursor:hand;";

    // 默认按钮样式
    private final String NORMAL_STYLE =
            "-fx-background-color:white;" +
                    "-fx-font-size:26px;" +
                    "-fx-background-radius:20;" +
                    "-fx-border-radius:20;" +
                    "-fx-alignment:CENTER-LEFT;" +
                    "-fx-padding:0 0 0 30;" +
                    "-fx-cursor:hand;";

    @FXML
    public void initialize() {
        loadPage("/fxml/dashboard.fxml");
    }

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        loadPage("/fxml/dashboard.fxml");
    }

    @FXML
    private void showBooks() {
        setActiveButton(bookBtn);
        loadPage("/fxml/book.fxml");
    }

    @FXML
    private void showUsers() {
        setActiveButton(userBtn);
        loadPage("/fxml/user.fxml");
    }

    @FXML
    private void showBorrow() {
        setActiveButton(borrowBtn);
        loadPage("/fxml/borrow.fxml");
    }

    /**
     * 切换页面
     */
    private void loadPage(String fxmlPath) {

        try {

            Parent root =
                    FXMLLoader.load(getClass().getResource(fxmlPath));

            contentPane.getChildren().clear();

            contentPane.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换按钮高亮
     */
    private void setActiveButton(Button activeBtn) {

        dashboardBtn.setStyle(NORMAL_STYLE);
        bookBtn.setStyle(NORMAL_STYLE);
        userBtn.setStyle(NORMAL_STYLE);
        borrowBtn.setStyle(NORMAL_STYLE);

        activeBtn.setStyle(ACTIVE_STYLE);
    }
}