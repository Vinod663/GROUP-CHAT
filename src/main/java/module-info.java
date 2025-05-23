module org.example.groupchat {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.vinod.ChatApplication.client to javafx.fxml;
    opens com.vinod.ChatApplication.server to javafx.fxml;
    exports com.vinod.ChatApplication;
}