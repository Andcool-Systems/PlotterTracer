package com.andcool.Tracer.Controllers;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public class ConfigController {
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
}
