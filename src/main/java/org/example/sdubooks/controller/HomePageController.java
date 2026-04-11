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
//因为那些页面还没有写，现在只是让控制台输出你点了什么按钮
public class HomePageController extends BaseController{

    @FXML
    protected void handleCategoryClick(ActionEvent event) {
        Button button = (Button) event.getSource();
        String category = (String) button.getUserData();
        System.out.println("点击了图书分类按钮: " + category);
    }

    @FXML
    protected void handleViewRankings() {
        System.out.println("点击了查看排行榜按钮");
    }

    @FXML
    protected void handleBookCardClick(MouseEvent event) {
        HBox card = (HBox) event.getSource();
        String bookName = (String) card.getUserData();
        System.out.println("点击了书籍卡片按钮: " + bookName);
    }
}
