package com.andcool.Tracer;


import com.andcool.Tracer.Controllers.MainController;
import com.andcool.Tracer.Settings.Settings;
import com.andcool.Tracer.SillyLogger.Level;
import com.andcool.Tracer.SillyLogger.SillyLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class Main extends Application {
    private static final int BATCH_SIZE = 10;
    private static final List<Line> uiUpdates = new ArrayList<>();
    public static final SillyLogger logger = new SillyLogger("Main Thread", true, Level.DEBUG);
    public static final ImageManager imageManager = new ImageManager();
    public static final Engine engine = new Engine();
    public static final Config config = new Config();
    public static MainController controller;

    public static List<String> gCode = new ArrayList<>();
    static TraceThread trace_thread = new TraceThread(null);

    @Override
    public void start(Stage primaryStage) throws IOException {
        Settings.load();
        primaryStage.setTitle("JavaFX Test");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainView.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setScene(new Scene(root, 1920, 1000));
        primaryStage.show();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> logger.log(Level.ERROR, throwable, true));

    }

    static class TraceThread extends Thread {
        WritableImage processCanvas;

        public TraceThread(WritableImage processCanvas) {
            this.processCanvas = processCanvas;
        }

        @Override
        public void run() {
            if (controller.filteredImage.getImage() == null) return;
            gCode.clear();

            Platform.runLater(() -> controller.pane.getChildren().clear());
            double factor = controller.processedContainer.getPrefWidth() / (Settings.WIDTH / Settings.LINE_WIDTH);
            engine.run((p1, p2, state) -> {
                Line line = new Line(
                        p1.getX() * factor,
                        p1.getY() * factor,
                        p2.getX() * factor,
                        p2.getY() * factor
                );

                line.setStroke(state ? Color.BLACK : (controller.displayTravel.isSelected() ? Color.GREEN : Color.TRANSPARENT));
                line.setStrokeWidth(0.5);
                uiUpdates.add(line);

                if (uiUpdates.size() >= BATCH_SIZE) {
                    List<Line> lines = new ArrayList<>(uiUpdates);
                    uiUpdates.clear();
                    Platform.runLater(() -> controller.pane.getChildren().addAll(lines));
                }

                double gcodeX = p2.getX() * Settings.LINE_WIDTH;
                double gcodeY = p2.getY() * Settings.LINE_WIDTH;
                if (Settings.MIRROR_X) gcodeX = Settings.WIDTH - gcodeX;

                Point2D point = new Point2D(gcodeX, gcodeY);

                float z = state ? 2.5f : 7;
                if (state != engine.lastState) {
                    gCode.addLast(format("G0 Z%f\n", z).replaceAll(",", "."));
                    engine.lastState = state;
                }

                int speed = state ? 2200 : 3300;
                gCode.addLast(format("G1 X%f Y%f F%d\n", point.getX() + 55, point.getY() + 55, speed).replaceAll(",", "."));
            }, this.processCanvas);

            if (!uiUpdates.isEmpty()) {
                List<Line> lines = new ArrayList<>(uiUpdates);
                uiUpdates.clear();
                Platform.runLater(() -> controller.pane.getChildren().addAll(lines));
            }
        }
    }

    public static void render() {
        if (trace_thread.isAlive()) return;

        Image inputImage = controller.filteredImage.getImage();
        int scaledOffsetX = (int) (inputImage.getWidth() - (controller.filteredImage.getScaleX() * inputImage.getWidth()));
        int scaledOffsetY = (int) (inputImage.getHeight() - (controller.filteredImage.getScaleY() * inputImage.getHeight()));
        double canvasWidth = Settings.WIDTH / Settings.LINE_WIDTH;
        double canvasHeight = Settings.HEIGHT / Settings.LINE_WIDTH;
        double factor = canvasWidth / controller.processedContainer.getPrefWidth();
        WritableImage process_canvas = imageManager.insetImage(
                inputImage,
                (int) canvasWidth,
                (int) canvasHeight,
                (int) ((controller.filteredImage.getTranslateX() + (double) scaledOffsetX / 2) * factor),
                (int) ((controller.filteredImage.getTranslateY() + (double) scaledOffsetY / 2) * factor),
                (int) (controller.filteredImage.getScaleX() * inputImage.getWidth() * factor),
                (int) (controller.filteredImage.getScaleY() * inputImage.getHeight() * factor)
        );
        controller.pane.setTranslateX(0);
        controller.pane.setTranslateY(0);
        controller.pane.setScaleX(1);
        controller.pane.setScaleY(1);

        trace_thread = new TraceThread(process_canvas);
        trace_thread.setDaemon(true);
        trace_thread.start();
    }

    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        launch(args);
    }
}
