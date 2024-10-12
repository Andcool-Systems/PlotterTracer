module com.andcool.javafx_test {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.andcool.Tracer to javafx.fxml;
    exports com.andcool.Tracer.Controllers;
    exports com.andcool.Tracer.sillyLogger;
    exports com.andcool.Tracer;
}