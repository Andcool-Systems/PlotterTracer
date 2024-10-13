package com.andcool.Tracer;

import com.andcool.Tracer.sillyLogger.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Config {
    public void openSettingsWindow() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("configView.fxml")));
            Scene scene = new Scene(root, 500, 500);

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Machine settings");
            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (Exception e) {
            Main.logger.log(Level.ERROR, e, true);
        }
    }
}
