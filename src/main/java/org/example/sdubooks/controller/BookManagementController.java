package org.example.sdubooks.controller;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.sdubooks.model.Book;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    // ==================== 表格列配置（使用 Callback 绑定） ====================
    private void setupTableColumns() {
        TableColumn<Book, String> titleCol = new TableColumn<>("书名");
        titleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTitle() != null ? data.getValue().getTitle() : ""));
        titleCol.setPrefWidth(200);

        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getAuthor() != null ? data.getValue().getAuthor() : ""));
        authorCol.setPrefWidth(150);

        TableColumn<Book, String> totalCol = new TableColumn<>("总库存");
        totalCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getTotal() != null ? data.getValue().getTotal() : 0)));
        totalCol.setPrefWidth(100);

        TableColumn<Book, String> availableCol = new TableColumn<>("可借数量");
        availableCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getAvailable() != null ? data.getValue().getAvailable() : 0)));
        availableCol.setPrefWidth(100);

        TableColumn<Book, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ 编辑");
            private final Button deleteBtn = new Button("🗑️ 删除");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                editBtn.setOnAction(e -> showBookDialog(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        actionCol.setPrefWidth(180);

        booksTable.getColumns().clear();
        booksTable.getColumns().addAll(titleCol, authorCol, totalCol, availableCol, actionCol);
        booksTable.setItems(bookList);
    }

    // ==================== 数据加载 ====================
    private void loadBooks(int page) {
        String token = getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BOOKS_URL)
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String respBody = response.body().string();
                        System.out.println("[DEBUG] 书籍列表响应: " + respBody);

                        // 后端返回 {"code":200, "data":[...]} — data 是数组
                        List<Book> allBooks = null;
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.has("data") && json.get("data").isJsonArray()) {
                            allBooks = gson.fromJson(json.getAsJsonArray("data"),
                                    new TypeToken<List<Book>>() {}.getType());
                        } else {
                            allBooks = gson.fromJson(respBody,
                                    new TypeToken<List<Book>>() {}.getType());
                        }

                        if (allBooks == null) allBooks = new java.util.ArrayList<>();

                        // 前端搜索过滤
                        String keyword = searchField.getText().trim().toLowerCase();
                        if (!keyword.isEmpty()) {
                            allBooks = allBooks.stream()
                                    .filter(b -> (b.getTitle() != null && b.getTitle().toLowerCase().contains(keyword))
                                            || (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(keyword))
                                            || (b.getCategory() != null && b.getCategory().toLowerCase().contains(keyword)))
                                    .collect(java.util.stream.Collectors.toList());
                        }

                        // 前端分页
                        final List<Book> finalAllBooks = allBooks;
                        int totalSize = allBooks.size();
                        int fromIndex = Math.min(page * PAGE_SIZE, totalSize);
                        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalSize);
                        List<Book> pageBooks = allBooks.subList(fromIndex, toIndex);
                        int computedTotalPages = Math.max((int) Math.ceil((double) totalSize / PAGE_SIZE), 1);

                        final int finalTotalPages = computedTotalPages;
                        final List<Book> finalPageBooks = pageBooks;
                        Platform.runLater(() -> {
                            bookList.setAll(finalPageBooks);
                            totalPages = finalTotalPages;
                            currentPage = page;
                            totalBooksLabel.setText(String.valueOf(finalAllBooks.size()));
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
            } catch (Exception e) {
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
        dialog.getDialogPane().setPrefWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        // 第一列固定宽度，确保标签不被截断
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setMinWidth(80);
        labelCol.setPrefWidth(80);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        // 输入框统一样式
        String fieldStyle = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #e2e8f0; -fx-padding: 8;";
        String labelStyle = "-fx-font-size: 13px; -fx-text-fill: #475569; -fx-padding: 8 0;";

        TextField titleField = new TextField(isEdit ? book.getTitle() : "");
        titleField.setStyle(fieldStyle);
        TextField authorField = new TextField(isEdit ? book.getAuthor() : "");
        authorField.setStyle(fieldStyle);
        // 分类下拉框（与首页八个分类一致）
        javafx.scene.control.ComboBox<String> categoryField = new javafx.scene.control.ComboBox<>();
        categoryField.getItems().addAll("小说", "科技", "历史", "科学", "艺术", "商业", "心理学", "传记");
        categoryField.setPromptText("选择分类");
        categoryField.setStyle(fieldStyle);
        categoryField.setPrefWidth(200);
        if (isEdit && book.getCategory() != null) {
            categoryField.setValue(book.getCategory());
        }
        TextField isbnField = new TextField(isEdit ? book.getIsbn() : "");
        isbnField.setStyle(fieldStyle);
        TextField publisherField = new TextField(isEdit ? book.getPublisher() : "");
        publisherField.setStyle(fieldStyle);
        TextField publishDateField = new TextField(isEdit ? book.getPublishDate() : "");
        publishDateField.setStyle(fieldStyle);
        publishDateField.setPromptText("如：2008-01");
        TextArea descriptionArea = new TextArea(isEdit ? book.getDescription() : "");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #e2e8f0; -fx-padding: 8;");

        Spinner<Integer> totalSpinner = new Spinner<>(0, 9999, isEdit ? book.getTotal() : 0);
        Spinner<Integer> availableSpinner = new Spinner<>(0, 9999, isEdit ? book.getAvailable() : 0);
        totalSpinner.setEditable(true);
        availableSpinner.setEditable(true);
        totalSpinner.setPrefWidth(120);
        availableSpinner.setPrefWidth(120);

        // 封面图片预览
        ImageView coverPreview = new ImageView();
        coverPreview.setFitWidth(80);
        coverPreview.setFitHeight(100);
        coverPreview.setPreserveRatio(true);
        coverPreview.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8;");

        // 封面 URL 输入框（可手动输入网络图片地址）
        TextField coverUrlField = new TextField(isEdit && book.getCoverUrl() != null ? book.getCoverUrl() : "");
        coverUrlField.setStyle(fieldStyle);
        coverUrlField.setPromptText("输入图片URL或选择本地文件");

        if (isEdit && book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
            try {
                coverPreview.setImage(new Image(book.getCoverUrl(), 80, 100, true, true));
            } catch (Exception ignored) {}
        }

        Button chooseCoverBtn = new Button("📁 选择图片");
        chooseCoverBtn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");

        Button removeCoverBtn = new Button("✕ 移除");
        removeCoverBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
        removeCoverBtn.setVisible(false);
        removeCoverBtn.setManaged(false);

        Label coverNameLabel = new Label(isEdit ? "当前封面" : "未选择图片");
        coverNameLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        HBox coverBtnBox = new HBox(8, chooseCoverBtn, removeCoverBtn);
        coverBtnBox.setStyle("-fx-alignment: CENTER_LEFT;");

        // 选择本地图片
        chooseCoverBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择封面图片（仅限一张）");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );
            // 使用主窗口作为父窗口
            Stage ownerStage = getCurrentStage();
            File file = fileChooser.showOpenDialog(ownerStage);
            if (file != null) {
                selectedCoverFile = file;
                coverNameLabel.setText(file.getName());
                coverUrlField.clear();
                coverPreview.setImage(new Image(file.toURI().toString(), 80, 100, true, true));
                removeCoverBtn.setVisible(true);
                removeCoverBtn.setManaged(true);
            }
        });

        // 移除已选封面
        removeCoverBtn.setOnAction(e -> {
            selectedCoverFile = null;
            coverPreview.setImage(null);
            coverNameLabel.setText("未选择图片");
            removeCoverBtn.setVisible(false);
            removeCoverBtn.setManaged(false);
        });

        // URL 输入变化时更新预览
        coverUrlField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && selectedCoverFile == null) {
                try {
                    coverPreview.setImage(new Image(newVal, 80, 100, true, true));
                    coverNameLabel.setText("URL封面");
                    removeCoverBtn.setVisible(true);
                    removeCoverBtn.setManaged(true);
                } catch (Exception ignored) {
                    coverNameLabel.setText("无效的图片URL");
                }
            } else if (newVal == null || newVal.isEmpty()) {
                if (selectedCoverFile == null) {
                    coverPreview.setImage(null);
                    coverNameLabel.setText("未选择图片");
                    removeCoverBtn.setVisible(false);
                    removeCoverBtn.setManaged(false);
                }
            }
        });

        VBox coverBox = new VBox(6, coverPreview, coverUrlField, coverBtnBox, coverNameLabel);
        coverBox.setStyle("-fx-alignment: CENTER_LEFT;");

        int row = 0;
        addFormRow(grid, row++, "书名:", titleField, labelStyle);
        addFormRow(grid, row++, "作者:", authorField, labelStyle);
        addFormRow(grid, row++, "分类:", categoryField, labelStyle);
        addFormRow(grid, row++, "ISBN:", isbnField, labelStyle);
        addFormRow(grid, row++, "出版社:", publisherField, labelStyle);
        addFormRow(grid, row++, "出版日期:", publishDateField, labelStyle);
        addFormRow(grid, row++, "总库存:", totalSpinner, labelStyle);
        addFormRow(grid, row++, "可借数量:", availableSpinner, labelStyle);

        Label coverLabel = new Label("封面:");
        coverLabel.setStyle(labelStyle);
        grid.add(coverLabel, 0, row);
        grid.add(coverBox, 1, row++);

        Label descLabel = new Label("简介:");
        descLabel.setStyle(labelStyle);
        grid.add(descLabel, 0, row);
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

                String manualCoverUrl = coverUrlField.getText().trim();

                if (isEdit) {
                    // 编辑模式：使用 JSON 更新
                    Book payload = new Book();
                    payload.setId(book.getId());
                    payload.setTitle(title);
                    payload.setAuthor(author);
                    payload.setCategory(categoryField.getValue());
                    payload.setIsbn(isbnField.getText().trim());
                    payload.setPublisher(publisherField.getText().trim());
                    payload.setPublishDate(publishDateField.getText().trim());
                    payload.setDescription(descriptionArea.getText().trim());
                    payload.setTotal(totalSpinner.getValue());
                    payload.setAvailable(availableSpinner.getValue());
                    // 如果没有选择本地文件，用手动输入的 URL
                    if (selectedCoverFile == null && !manualCoverUrl.isEmpty()) {
                        payload.setCoverUrl(manualCoverUrl);
                    }
                    saveBook(payload, true);
                } else {
                    // 新增模式：先上传封面再创建书籍
                    saveBookWithCover(title, author, categoryField.getValue(),
                            isbnField.getText().trim(), publisherField.getText().trim(),
                            publishDateField.getText().trim(), descriptionArea.getText().trim(),
                            totalSpinner.getValue(), availableSpinner.getValue(),
                            manualCoverUrl);
                }
            }
            return btn;
        });

        dialog.showAndWait();
    }

    /**
     * 新增书籍：先上传封面获取 URL，再用 JSON 创建书籍
     */
    private void saveBookWithCover(String title, String author, String category,
                                    String isbn, String publisher, String publishDate,
                                    String description, int total, int available,
                                    String manualCoverUrl) {
        String token = getToken();
        if (token == null) {
            Platform.runLater(() -> showAlert("错误", "未登录，请重新登录", Alert.AlertType.ERROR));
            return;
        }

        new Thread(() -> {
            try {
                // 确定封面 URL
                System.out.println("[DEBUG] selectedCoverFile=" + (selectedCoverFile != null ? selectedCoverFile.getName() : "null") + ", manualCoverUrl=" + manualCoverUrl);
                String coverUrl = resolveCoverUrl(token, manualCoverUrl);
                System.out.println("[DEBUG] 最终封面URL: " + coverUrl);

                // 用 JSON 创建书籍
                Book payload = new Book();
                payload.setTitle(title);
                payload.setAuthor(author);
                payload.setCategory(category);
                payload.setIsbn(isbn);
                payload.setPublisher(publisher);
                payload.setPublishDate(publishDate);
                payload.setDescription(description);
                payload.setTotal(total);
                payload.setAvailable(available);
                if (coverUrl != null && !coverUrl.isEmpty()) {
                    payload.setCoverUrl(coverUrl);
                }

                String json = gson.toJson(payload);
                System.out.println("[DEBUG] 新增书籍请求URL: " + BOOKS_URL);
                System.out.println("[DEBUG] 新增书籍请求Body: " + json);

                RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url(BOOKS_URL)
                        .header("Authorization", "Bearer " + token)
                        .post(body)
                        .build();

                System.out.println("[DEBUG] 发送请求中...");
                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    System.out.println("[DEBUG] 新增书籍响应: code=" + response.code() + " body=" + respBody);
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            showAlert("成功", "书籍已添加", Alert.AlertType.INFORMATION);
                            loadBooks(currentPage);
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("失败", "新增书籍失败 (HTTP " + response.code() + "): " + respBody, Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("[DEBUG] 新增书籍异常: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("错误", "请求异常: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    /**
     * 上传封面图片到服务器，返回可访问的 URL
     * 后端接口：POST /api/admin/upload/cover
     * 响应格式：{"code":200, "data":"http://server:8081/uploads/xxx.jpg"}
     */
    private String uploadCoverImage(String token, File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            System.out.println("[DEBUG] 封面文件不存在或为null");
            return null;
        }
        try {
            String mimeType = getMimeType(imageFile.getName());
            System.out.println("[DEBUG] 上传封面: file=" + imageFile.getAbsolutePath()
                    + " name=" + imageFile.getName() + " size=" + imageFile.length() + " mime=" + mimeType);

            RequestBody fileBody = RequestBody.create(imageFile, MediaType.get(mimeType));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(), fileBody)
                    .build();

            String uploadUrl = BASE_URL + "/admin/upload/cover";
            System.out.println("[DEBUG] 上传URL: " + uploadUrl);

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer " + token)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                System.out.println("[DEBUG] 封面上传响应: code=" + response.code() + " body=" + respBody);
                if (response.isSuccessful()) {
                    JsonObject json = gson.fromJson(respBody, JsonObject.class);
                    if (json.has("data") && !json.get("data").isJsonNull()) {
                        String url = json.get("data").getAsString();
                        System.out.println("[DEBUG] 封面上传成功: " + url);
                        return url;
                    } else {
                        System.out.println("[DEBUG] 响应中没有data字段: " + respBody);
                    }
                } else {
                    System.out.println("[DEBUG] 上传失败: HTTP " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] 封面上传异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析封面 URL：优先本地上传，其次网络 URL
     */
    private String resolveCoverUrl(String token, String manualCoverUrl) {
        // 1. 本地文件上传
        if (selectedCoverFile != null) {
            String uploaded = uploadCoverImage(token, selectedCoverFile);
            if (uploaded != null) return uploaded;
            System.out.println("[DEBUG] 本地上传失败，尝试使用手动URL");
        }
        // 2. 手动输入的网络 URL
        if (manualCoverUrl != null && !manualCoverUrl.isEmpty()) {
            return manualCoverUrl;
        }
        return null;
    }

    /**
     * 编辑书籍（JSON 更新，支持封面上传）
     */
    private void saveBook(Book book, boolean isEdit) {
        String token = getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                // 如果编辑时选择了新的封面（本地或URL），更新 coverUrl
                if (isEdit) {
                    String coverUrl = resolveCoverUrl(token, book.getCoverUrl());
                    if (coverUrl != null) {
                        book.setCoverUrl(coverUrl);
                    }
                }

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

                try (Response response = httpClient.newCall(reqBuilder.build()).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    System.out.println("[DEBUG] 保存书籍响应: code=" + response.code() + " body=" + respBody);
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            showAlert("成功", isEdit ? "书籍已更新" : "书籍已添加", Alert.AlertType.INFORMATION);
                            loadBooks(currentPage);
                        });
                    } else {
                        Platform.runLater(() ->
                                showAlert("失败", (isEdit ? "更新" : "新增") + "书籍失败 (HTTP " + response.code() + "): " + respBody, Alert.AlertType.ERROR)
                        );
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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

    /**
     * 添加表单行：标签在左，输入控件在右
     */
    private void addFormRow(GridPane grid, int row, String labelText, javafx.scene.Node field, String labelStyle) {
        Label label = new Label(labelText);
        label.setStyle(labelStyle);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
