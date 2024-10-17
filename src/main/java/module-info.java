module com.andcool.javafx_test {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires org.json;

    opens com.andcool.Tracer to javafx.fxml;
    exports com.andcool.Tracer.Controllers;
    exports com.andcool.Tracer.SillyLogger;
    exports com.andcool.Tracer;
}