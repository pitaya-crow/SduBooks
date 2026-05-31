package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.sdubooks.model.Book;
import org.example.sdubooks.model.PageResponse;
import okhttp3.*;

import java.io.File;
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
                        String respBody = response.body().string();
                        System.out.println("[DEBUG] 书籍列表响应: " + respBody);

                        // 后端返回 {"code":200, "data":{...}} 包装格式
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        PageResponse<Book> result;
                        if (json.has("data") && json.get("data").isJsonObject()) {
                            result = gson.fromJson(json.getAsJsonObject("data"),
                                    new TypeToken<PageResponse<Book>>() {}.getType());
                        } else {
                            // 兼容直接返回 PageResponse 的情况
                            result = gson.fromJson(respBody,
                                    new TypeToken<PageResponse<Book>>() {}.getType());
                        }

                        PageResponse<Book> finalResult = result;
                        Platform.runLater(() -> {
                            if (finalResult.getContent() != null) {
                                bookList.setAll(finalResult.getContent());
                            } else {
                                bookList.clear();
                            }
                            totalPages = Math.max(finalResult.getTotalPages(), 1);
                            currentPage = finalResult.getPageNumber();
                            totalBooksLabel.setText(String.valueOf(finalResult.getTotalElements()));
                            pageInfoLabel.setText((currentPage + 1) + "/" + totalPages);
                            prevPageBtn.setDisable(currentPage <= 0);
                            nextPageBtn.setDisable(currentPage >= totalPages - 1);
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("错误", "加载书籍失败: " + response.message(), Alert.AlertType.ERROR)
                        );
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

    private File selectedCoverFile;

    private void showBookDialog(Book book) {
        boolean isEdit = (book != null);
        selectedCoverFile = null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "编辑书籍" : "新增书籍");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(20));

        TextField titleField = new TextField(isEdit ? book.getTitle() : "");
        TextField authorField = new TextField(isEdit ? book.getAuthor() : "");
        TextField categoryField = new TextField(isEdit ? book.getCategory() : "");
        TextField isbnField = new TextField(isEdit ? book.getIsbn() : "");
        TextField publisherField = new TextField(isEdit ? book.getPublisher() : "");
        TextField publishDateField = new TextField(isEdit ? book.getPublishDate() : "");
        TextArea descriptionArea = new TextArea(isEdit ? book.getDescription() : "");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        Spinner<Integer> totalSpinner = new Spinner<>(0, 9999, isEdit ? book.getTotal() : 0);
        Spinner<Integer> availableSpinner = new Spinner<>(0, 9999, isEdit ? book.getAvailable() : 0);
        totalSpinner.setEditable(true);
        availableSpinner.setEditable(true);

        // 封面图片预览
        ImageView coverPreview = new ImageView();
        coverPreview.setFitWidth(80);
        coverPreview.setFitHeight(100);
        coverPreview.setPreserveRatio(true);
        coverPreview.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 8;");

        if (isEdit && book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
            try {
                coverPreview.setImage(new Image(book.getCoverUrl(), 80, 100, true, true));
            } catch (Exception ignored) {}
        }

        Button chooseCoverBtn = new Button("📁 选择封面");
        chooseCoverBtn.setStyle("-fx-background-color: #e2e8f0; -fx-cursor: hand;");
        Label coverNameLabel = new Label(isEdit ? "当前封面" : "未选择");
        coverNameLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        chooseCoverBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择封面图片");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );
            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedCoverFile = file;
                coverNameLabel.setText(file.getName());
                coverPreview.setImage(new Image(file.toURI().toString(), 80, 100, true, true));
            }
        });

        VBox coverBox = new VBox(8, coverPreview, chooseCoverBtn, coverNameLabel);
        coverBox.setStyle("-fx-alignment: CENTER;");

        int row = 0;
        grid.addRow(row++, new Label("书名:"), titleField);
        grid.addRow(row++, new Label("作者:"), authorField);
        grid.addRow(row++, new Label("分类:"), categoryField);
        grid.addRow(row++, new Label("ISBN:"), isbnField);
        grid.addRow(row++, new Label("出版社:"), publisherField);
        grid.addRow(row++, new Label("出版日期:"), publishDateField);
        grid.addRow(row++, new Label("总库存:"), totalSpinner);
        grid.addRow(row++, new Label("可借数量:"), availableSpinner);
        grid.add(new Label("封面:"), 0, row);
        grid.add(coverBox, 1, row++);
        grid.add(new Label("简介:"), 0, row);
        grid.add(descriptionArea, 1, row++);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                if (title.isEmpty() || author.isEmpty()) {
                    showAlert("提示", "书名和作者不能为空", Alert.AlertType.WARNING);
                    return null;
                }

                if (isEdit) {
                    // 编辑模式：使用 JSON 更新
                    Book payload = new Book();
                    payload.setId(book.getId());
                    payload.setTitle(title);
                    payload.setAuthor(author);
                    payload.setCategory(categoryField.getText().trim());
                    payload.setIsbn(isbnField.getText().trim());
                    payload.setPublisher(publisherField.getText().trim());
                    payload.setPublishDate(publishDateField.getText().trim());
                    payload.setDescription(descriptionArea.getText().trim());
                    payload.setTotal(totalSpinner.getValue());
                    payload.setAvailable(availableSpinner.getValue());
                    saveBook(payload, true);
                } else {
                    // 新增模式：使用 multipart 上传
                    saveBookWithCover(title, author, categoryField.getText().trim(),
                            isbnField.getText().trim(), publisherField.getText().trim(),
                            publishDateField.getText().trim(), descriptionArea.getText().trim(),
                            totalSpinner.getValue(), availableSpinner.getValue());
                }
            }
            return btn;
        });

        dialog.showAndWait();
    }

    /**
     * 新增书籍（支持封面图片上传）
     */
    private void saveBookWithCover(String title, String author, String category,
                                    String isbn, String publisher, String publishDate,
                                    String description, int total, int available) {
        String token = getToken();
        if (token == null) return;

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("author", author)
                .addFormDataPart("category", category)
                .addFormDataPart("isbn", isbn)
                .addFormDataPart("publisher", publisher)
                .addFormDataPart("publishDate", publishDate)
                .addFormDataPart("description", description)
                .addFormDataPart("total", String.valueOf(total))
                .addFormDataPart("available", String.valueOf(available));

        // 添加封面图片（可选）
        if (selectedCoverFile != null) {
            String mimeType = getMimeType(selectedCoverFile.getName());
            multipartBuilder.addFormDataPart("cover", selectedCoverFile.getName(),
                    RequestBody.create(selectedCoverFile, MediaType.get(mimeType)));
        }

        Request request = new Request.Builder()
                .url(BOOKS_URL)
                .header("Authorization", "Bearer " + token)
                .post(multipartBuilder.build())
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 新增书籍响应: " + response.code() + " " + respBody);
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", "书籍已添加", Alert.AlertType.INFORMATION);
                        loadBooks(currentPage);
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("失败", "新增书籍失败: " + respBody, Alert.AlertType.ERROR)
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("错误", "网络异常: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    /**
     * 编辑书籍（JSON 更新）
     */
    private void saveBook(Book book, boolean isEdit) {
        String token = getToken();
        if (token == null) return;

        String json = gson.toJson(book);
        System.out.println("[DEBUG] 编辑书籍请求: " + json);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request.Builder reqBuilder = new Request.Builder()
                .header("Authorization", "Bearer " + token)
                .url(BOOKS_URL + (isEdit ? "/" + book.getId() : ""));

        if (isEdit) {
            reqBuilder.put(body);
        } else {
            reqBuilder.post(body);
        }

        new Thread(() -> {
            try (Response response = httpClient.newCall(reqBuilder.build()).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 保存书籍响应: " + response.code() + " " + respBody);
                if (response.isSuccessful()) {
                    Platform.runLater(() -> {
                        showAlert("成功", isEdit ? "书籍已更新" : "书籍已添加", Alert.AlertType.INFORMATION);
                        loadBooks(currentPage);
                    });
                } else {
                    Platform.runLater(() ->
                            showAlert("失败", (isEdit ? "更新" : "新增") + "书籍失败: " + respBody, Alert.AlertType.ERROR)
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

    // ==================== 工具方法 ====================
    private String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
