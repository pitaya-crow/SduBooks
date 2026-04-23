package org.example.sdubooks.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
