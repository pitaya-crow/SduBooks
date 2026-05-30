package org.example.sdubooks.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.example.sdubooks.model.Book;
import org.example.sdubooks.model.PageResponse;
import okhttp3.*;

import java.io.IOException;

public class BookManagementController extends BaseController {

    @FXML private TextField searchField;
    @FXML private TableView<Book> booksTable;
    @FXML private Label totalBooksLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageBtn, nextPageBtn;

    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    private int currentPage = 0;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 10;
    private static final String BOOKS_URL = BASE_URL + "/admin/books";

    @FXML
    public void initialize() {
        setupTableColumns();
        loadBooks(0);
    }

    // ==================== 表格列配置 ====================
    private void setupTableColumns() {
        // 前4列已在FXML中绑定CellValueFactory，这里只需补充操作列
        TableColumn<Book, Void> actionCol = (TableColumn<Book, Void>) booksTable.getColumns().get(4);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ 编辑");
            private final Button deleteBtn = new Button("🗑️ 删除");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    Book book = getTableView().getItems().get(getIndex());
                    showBookDialog(book);
                });
                deleteBtn.setOnAction(e -> {
                    Book book = getTableView().getItems().get(getIndex());
                    confirmDelete(book);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        booksTable.setItems(bookList);
    }

    // ==================== 数据加载 ====================
    private void loadBooks(int page) {
        String token = getToken();
        if (token == null) return;

        String keyword = searchField.getText().trim();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BOOKS_URL).newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("size", String.valueOf(PAGE_SIZE));
        if (!keyword.isEmpty()) {
            urlBuilder.addQueryParameter("keyword", keyword);
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        PageResponse<Book> result = gson.fromJson(
                                response.body().string(),
                                new com.google.gson.reflect.TypeToken<PageResponse<Book>>() {}.getType()
                        );
                        Platform.runLater(() -> {
                            bookList.setAll(result.getContent());
                            totalPages = Math.max(result.getTotalPages(), 1);
                            currentPage = result.getPageNumber();
                            totalBooksLabel.setText(String.valueOf(result.getTotalElements()));
                            pageInfoLabel.setText((currentPage + 1) + "/" + totalPages);
                            prevPageBtn.setDisable(currentPage <= 0);
                            nextPageBtn.setDisable(currentPage >= totalPages - 1);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("错误", "加载书籍失败: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    // ==================== 搜索 & 分页 ====================
    @FXML private void handleSearch() { loadBooks(0); }
    @FXML private void handlePreviousPage() { if (currentPage > 0) loadBooks(currentPage - 1); }
    @FXML private void handleNextPage() { if (currentPage < totalPages - 1) loadBooks(currentPage + 1); }

    // ==================== 新增 / 编辑弹窗 ====================
    @FXML
    private void handleAddBook() { showBookDialog(null); }

    /**
     * 通用弹窗：book==null 为新增，否则为编辑
     */
    private void showBookDialog(Book book) {
        boolean isEdit = (book != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "编辑书籍" : "新增书籍");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(20));

        TextField titleField   = new TextField(isEdit ? book.getTitle() : "");
        TextField authorField  = new TextField(isEdit ? book.getAuthor() : "");
        Spinner<Integer> totalSpinner     = new Spinner<>(0, 9999, isEdit ? book.getTotal() : 0);
        Spinner<Integer> availableSpinner = new Spinner<>(0, 9999, isEdit ? book.getAvailable() : 0);
        totalSpinner.setEditable(true);
        availableSpinner.setEditable(true);

        grid.addRow(0, new Label("书名:"), titleField);
        grid.addRow(1, new Label("作者:"), authorField);
        grid.addRow(2, new Label("总库存:"), totalSpinner);
        grid.addRow(3, new Label("可借数量:"), availableSpinner);

        dialog.getDialogPane().setContent(grid);

        // 点击确定时发送请求
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                if (title.isEmpty() || author.isEmpty()) {
                    showAlert("提示", "书名和作者不能为空", Alert.AlertType.WARNING);
                    return null; // 阻止关闭
                }
                Book payload = new Book();
                payload.setTitle(title);
                payload.setAuthor(author);
                payload.setTotal(totalSpinner.getValue());
                payload.setAvailable(availableSpinner.getValue());
                if (isEdit) payload.setId(book.getId());
                saveBook(payload, isEdit);
            }
            return btn;
        });

        dialog.showAndWait();
    }

    /**
     * 统一保存（新增POST / 编辑PUT）
     */
    private void saveBook(Book book, boolean isEdit) {
        String token = getToken();
        if (token == null) return;

        String json = gson.toJson(book);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request.Builder reqBuilder = new Request.Builder()
                .header("Authorization", "Bearer " + token)
                .url(BOOKS_URL);

        if (isEdit) {
            reqBuilder.put(body);
        } else {
            reqBuilder.post(body);
        }

        new Thread(() -> {
            try (Response response = httpClient.newCall(reqBuilder.build()).execute()) {
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", isEdit ? "书籍已更新" : "书籍已添加", Alert.AlertType.INFORMATION);
                        loadBooks(currentPage);
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("失败", (isEdit ? "更新" : "新增") + "书籍失败: " + response.message(), Alert.AlertType.ERROR)
                    );
                }
            } catch (IOException e) {
                Platform.runLater(() -> showAlert("错误", "网络异常: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    // ==================== 删除 ====================
    private void confirmDelete(Book book) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除《" + book.getTitle() + "》吗？此操作不可恢复！");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                doDelete(book.getId());
            }
        });
    }

    private void doDelete(Long id) {
        String token = getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BOOKS_URL + "/" + id)
                        .delete()
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            showAlert("成功", "书籍已删除", Alert.AlertType.INFORMATION);
                            loadBooks(currentPage);
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("失败", "删除失败: " + response.message(), Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> showAlert("错误", "网络异常: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }
}