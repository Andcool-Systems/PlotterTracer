package com.andcool.Tracer;

import com.andcool.Tracer.Controllers.ConfigController;
import com.andcool.Tracer.SillyLogger.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Config {
    public static ConfigController controller;
    public static Stage settingsStage;

    public void openSettingsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("configView.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root, 500, 500);

            settingsStage = new Stage();
            settingsStage.setTitle("Machine settings");
            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (Exception e) {
            Main.logger.log(Level.ERROR, e, true);
        }
    }
}
