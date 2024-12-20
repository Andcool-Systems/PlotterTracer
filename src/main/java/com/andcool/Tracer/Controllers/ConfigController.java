package com.andcool.Tracer.Controllers;

import com.andcool.Tracer.Config;
import com.andcool.Tracer.Main;
import com.andcool.Tracer.Settings.Settings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import static java.lang.Math.abs;

public class ConfigController {
    @FXML
    public TextField width_el;
    public TextField height_el;
    public TextField line_width_el;
    public CheckBox mirrorX;
    public TextField offsetX;
    public TextField offsetY;

    public void filterCharacters(KeyEvent keyEvent) {
        TextField target = (TextField) (keyEvent.getTarget());
        if (!target.getText().matches("\\d*")) {
            target.setText(target.getText().replaceAll("\\D", ""));
        }
    }

    public void filterCharactersFloat(KeyEvent keyEvent) {
        TextField target = (TextField) keyEvent.getTarget();
        String text = target.getText();
        if (!text.matches("^[-+]?(\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?$")) {
            target.setText(text.replaceAll("[^0-9.eE+-]", ""));
            target.positionCaret(target.getText().length());
        }
    }

    public void save() {
        try {
            Settings.WIDTH = abs(Integer.parseInt(width_el.getText()));
        } catch (NumberFormatException e) {
            Settings.WIDTH = 100;
        }

        try {
            Settings.HEIGHT = abs(Integer.parseInt(height_el.getText()));
        } catch (NumberFormatException e) {
            Settings.HEIGHT = 100;
        }

        try {
            Settings.LINE_WIDTH = abs(Float.parseFloat(line_width_el.getText()));
        } catch (NumberFormatException e) {
            Settings.LINE_WIDTH = 0.5F;
        }

        Settings.X_OFFSET = Float.parseFloat(offsetX.getText());
        Settings.Y_OFFSET = Float.parseFloat(offsetY.getText());

        Settings.save();
        Config.settingsStage.close();
        Main.controller.configure();
    }

    @FXML
    public void initialize() {
        Settings.load();
        width_el.setText(String.valueOf(Settings.WIDTH));
        height_el.setText(String.valueOf(Settings.HEIGHT));
        line_width_el.setText(String.valueOf(Settings.LINE_WIDTH));
        mirrorX.setSelected(Settings.MIRROR_X);
        offsetX.setText(String.valueOf(Settings.X_OFFSET));
        offsetY.setText(String.valueOf(Settings.Y_OFFSET));
    }

    public void mirrorChange(ActionEvent actionEvent) {
        Settings.MIRROR_X = ((CheckBox) actionEvent.getTarget()).isSelected();
    }
}
