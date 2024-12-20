package com.andcool.Tracer.Controllers;

import com.andcool.Tracer.Files;
import com.andcool.Tracer.Main;
import com.andcool.Tracer.Settings.Settings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

    public float factor = 1;

    @FXML
    public Slider threshold;
    public ImageView filteredImage;
    public Label thresholdLabel;
    public Pane paneParent;
    public Pane pane;
    public Pane processedContainer;
    public Slider smoothStep;
    public Label smoothStepLabel;
    public CheckBox displayTravel;
    public Slider travelRange;
    public Label travelRangeLabel;

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

        smoothStep.valueProperty().addListener((observable, oldValue, newValue) -> {
            smoothStepLabel.setText("Smooth steps: " + newValue.intValue());
        });

        travelRange.valueProperty().addListener((observable, oldValue, newValue) -> {
            travelRangeLabel.setText("Travel range: " + newValue.intValue());
        });

        pane.addEventFilter(ScrollEvent.SCROLL, event -> handleScroll(event, pane));
        paneParent.setOnMousePressed(this::handleMousePressed);
        paneParent.setOnMouseDragged(this::handleMouseDragged);
        paneParent.setOnMouseReleased(event -> paneParent.setCursor(Cursor.DEFAULT));

        processedContainer.addEventFilter(ScrollEvent.SCROLL, event -> handleScroll(event, filteredImage));
        processedContainer.setOnMousePressed(this::handleMousePressedProcessed);
        processedContainer.setOnMouseDragged(this::handleMouseDraggedProcessed);

        threshold.setValue(128);
        configure();
    }

    public void configure() {

        factor = Settings.WIDTH > Settings.HEIGHT ?
                (float) 800 / Settings.WIDTH :
                (float) 800 / Settings.HEIGHT;

        processedContainer.setPrefWidth(Settings.WIDTH * factor);
        processedContainer.setPrefHeight(Settings.HEIGHT * factor);
        Rectangle clip = new Rectangle(0, 0, Settings.WIDTH * factor + 2, Settings.HEIGHT * factor + 2);
        processedContainer.setClip(clip);

        paneParent.setPrefWidth(Settings.WIDTH * factor);
        paneParent.setPrefHeight(Settings.HEIGHT * factor);
        Rectangle clipRender = new Rectangle(0, 0, Settings.WIDTH * factor + 2, Settings.HEIGHT * factor + 2);
        paneParent.setClip(clipRender);
    }

    private void handleScroll(ScrollEvent event, Node node) {
        event.consume();
        double delta = 0.1 * (event.getDeltaY() > 0 ? 1 : -1);
        double scale = Math.max(0.1, Math.min(node.getScaleX() + delta, 10.0));
        Point2D mousePos = new Point2D(event.getX(), event.getY());
        Point2D panePosBefore = node.localToParent(mousePos);

        node.setScaleX(scale);
        node.setScaleY(scale);
        Point2D panePosAfter = node.localToParent(mousePos);

        double dx = panePosAfter.getX() - panePosBefore.getX();
        double dy = panePosAfter.getY() - panePosBefore.getY();

        node.setTranslateX(node.getTranslateX() - dx);
        node.setTranslateY(node.getTranslateY() - dy);
    }

    private double mouseX1;
    private double mouseY1;

    private void handleMousePressed(MouseEvent event) {
        event.consume();
        mouseX1 = event.getSceneX();
        mouseY1 = event.getSceneY();
        paneParent.setCursor(Cursor.MOVE);
    }

    private void handleMouseDragged(MouseEvent event) {
        event.consume();
        double deltaX = event.getSceneX() - mouseX1;
        double deltaY = event.getSceneY() - mouseY1;

        pane.setTranslateX(pane.getTranslateX() + deltaX);
        pane.setTranslateY(pane.getTranslateY() + deltaY);

        mouseX1 = event.getSceneX();
        mouseY1 = event.getSceneY();
    }

    private double mouseX2;
    private double mouseY2;

    private void handleMousePressedProcessed(MouseEvent event) {
        event.consume();
        mouseX2 = event.getSceneX();
        mouseY2 = event.getSceneY();
        processedContainer.setCursor(Cursor.MOVE);
    }

    private void handleMouseDraggedProcessed(MouseEvent event) {
        event.consume();
        double deltaX = event.getSceneX() - mouseX2;
        double deltaY = event.getSceneY() - mouseY2;

        filteredImage.setTranslateX(filteredImage.getTranslateX() + deltaX);
        filteredImage.setTranslateY(filteredImage.getTranslateY() + deltaY);

        mouseX2 = event.getSceneX();
        mouseY2 = event.getSceneY();
    }

    public void render(ActionEvent actionEvent) {
        Main.render();
    }

    public void openConfig(ActionEvent actionEvent) {
        Main.config.openSettingsWindow();
    }

    public void export(ActionEvent actionEvent) throws IOException {
        Files.export(Main.gCode);
    }
}
