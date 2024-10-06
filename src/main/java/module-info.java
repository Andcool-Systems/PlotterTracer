module com.andcool.javafx_test {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.andcool.javafx_test to javafx.fxml;
    exports com.andcool.javafx_test;
}