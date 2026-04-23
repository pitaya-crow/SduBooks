module org.example.sdubooks {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires okhttp3;
    requires com.google.gson;

    opens org.example.sdubooks.controller to javafx.fxml, com.google.gson;

    opens org.example.sdubooks.model to com.google.gson;

    opens org.example.sdubooks to javafx.fxml;
    exports org.example.sdubooks;

    exports org.example.sdubooks.controller;
}