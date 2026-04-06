module org.example.sdubooks {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.sdubooks to javafx.fxml;
    exports org.example.sdubooks;

    opens org.example.sdubooks.controller to javafx.fxml;
    exports org.example.sdubooks.controller;
}