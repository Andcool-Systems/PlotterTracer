package com.andcool.Tracer.Controllers;

import com.andcool.Tracer.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainController {

    @FXML
    public Slider threshold;
    public ImageView filteredImage;
    public Label thresholdLabel;
    public Pane paneParent;
    public Pane pane;

    public void loadImage(ActionEvent actionEvent) {
        Stage stage = (Stage) ((MenuItem) actionEvent.getTarget()).getParentPopup().getOwnerWindow();
        Main.imageManager.openImage(stage);
        Main.imageManager.updateProcessed();
    }

    @FXML
    public void initialize() {
        threshold.valueProperty().addListener((observable, oldValue, newValue) -> {
            thresholdLabel.setText("Threshold: " + newValue.intValue());
            Main.imageManager.updateProcessed();
        });
        pane.addEventFilter(ScrollEvent.SCROLL, this::handleScroll);
        paneParent.setOnMousePressed(this::handleMousePressed);
        paneParent.setOnMouseDragged(this::handleMouseDragged);
        paneParent.setOnMouseReleased(event -> paneParent.setCursor(Cursor.DEFAULT));

        threshold.setValue(128);
    }

    private void handleScroll(ScrollEvent event) {
        event.consume();
        double delta = 0.3 * (event.getDeltaY() > 0 ? 1 : -1);
        double scale = Math.max(0.1, Math.min(pane.getScaleX() + delta, 10.0));
        Point2D mousePos = new Point2D(event.getX(), event.getY());
        Point2D panePosBefore = pane.localToParent(mousePos);

        pane.setScaleX(scale);
        pane.setScaleY(scale);
        Point2D panePosAfter = pane.localToParent(mousePos);

        double dx = panePosAfter.getX() - panePosBefore.getX();
        double dy = panePosAfter.getY() - panePosBefore.getY();

        pane.setTranslateX(pane.getTranslateX() - dx);
        pane.setTranslateY(pane.getTranslateY() - dy);
    }

    private double mouseX;
    private double mouseY;

    private void handleMousePressed(MouseEvent event) {
        event.consume();
        mouseX = event.getSceneX();
        mouseY = event.getSceneY();
        paneParent.setCursor(Cursor.MOVE);
    }

    private void handleMouseDragged(MouseEvent event) {
        event.consume();
        double deltaX = event.getSceneX() - mouseX;
        double deltaY = event.getSceneY() - mouseY;

        pane.setTranslateX(pane.getTranslateX() + deltaX);
        pane.setTranslateY(pane.getTranslateY() + deltaY);

        mouseX = event.getSceneX();
        mouseY = event.getSceneY();
    }

    public void render(ActionEvent actionEvent) {
        Main.render();
    }

    public void openConfig(ActionEvent actionEvent) {
        Main.config.openSettingsWindow();
    }
}
