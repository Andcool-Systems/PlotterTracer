package com.andcool.Tracer;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Config {
    private void openSettingsWindow() {
        try {
            // Загружаем fxml-файл (если используется)
            // Parent root = FXMLLoader.load(getClass().getResource("settings.fxml"));

            // Либо можно создать элементы без fxml:
            VBox root = new VBox();
            Scene scene = new Scene(root, 250, 150);

            // Создаем новое окно
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Настройки");
            settingsStage.setScene(scene);

            // Показываем новое окно
            settingsStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
