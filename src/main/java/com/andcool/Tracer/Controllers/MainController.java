package com.andcool.Tracer.Controllers;

import com.andcool.Tracer.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.awt.*;

import static java.lang.Math.*;

public class MainController {

    @FXML
    public Slider threshold;
    public ImageView filteredImage;
    public Label thresholdLabel;
    public ScrollPane scrollPane;
    public Pane pane;

    public void loadImage(ActionEvent actionEvent) {
        Main.imageManager.openImage((Stage) ((Button) actionEvent.getSource()).getScene().getWindow());
        Main.imageManager.updateProcessed();
    }

    @FXML
    public void initialize() {
        threshold.valueProperty().addListener((observable, oldValue, newValue) -> {
            thresholdLabel.setText("Threshold: " + newValue.intValue());
            Main.imageManager.updateProcessed();
        });
        pane.addEventFilter(ScrollEvent.SCROLL, this::handleScroll);
        pane.setOnMousePressed(this::handleMousePressed);
        pane.setOnMouseDragged(this::handleMouseDragged);
        pane.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleScroll(ScrollEvent event) {
        event.consume();
        double delta = 0.3f * (event.getDeltaY() / abs(event.getDeltaY()));
        double scale = max(0.1f, min(pane.getScaleX() + delta, 5.0f));
        pane.setScaleX(scale);
        pane.setScaleY(scale);

        Point2D mousePos = getMousePosition(pane);

        //pane.setTranslateX(pane.getTranslateX() / 2 - mousePos.getX());
    }

    public static Point2D getMousePosition(Node scrollPane) {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        double screenX = mouseLocation.getX();
        double screenY = mouseLocation.getY() - 30;
        Point2D scenePoint = scrollPane.getScene().getRoot().sceneToLocal(screenX, screenY);
        return scrollPane.sceneToLocal(scenePoint);
    }

    private double mouseX;
    private double mouseY;
    private boolean dragging;

    private void handleMousePressed(MouseEvent event) {
        mouseX = event.getSceneX();
        mouseY = event.getSceneY();
        dragging = true;
    }

    private void handleMouseDragged(MouseEvent event) {
        if (dragging) {
            double deltaX = event.getSceneX() - mouseX;
            double deltaY = event.getSceneY() - mouseY;

            pane.setTranslateX(pane.getTranslateX() + deltaX);
            pane.setTranslateY(pane.getTranslateY() + deltaY);

            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        dragging = false;
    }

    public void render(ActionEvent actionEvent) {
        Main.render();
    }
}
