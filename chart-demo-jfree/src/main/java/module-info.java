module com.intechcore.chartdemojfree {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.intechcore.chartdemojfree to javafx.fxml;
    exports com.intechcore.chartdemojfree;
}