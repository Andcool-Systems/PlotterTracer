package com.andcool.Tracer;


import com.andcool.Tracer.Controllers.MainController;
import com.andcool.Tracer.Settings.Settings;
import com.andcool.Tracer.SillyLogger.Level;
import com.andcool.Tracer.SillyLogger.SillyLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
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

public class Main extends Application {
    private static final int BATCH_SIZE = 10;
    private static final List<Line> uiUpdates = new ArrayList<>();
    public static final SillyLogger logger = new SillyLogger("Main Thread", true, Level.DEBUG);
    public static final ImageManager imageManager = new ImageManager();
    public static final Engine engine = new Engine();
    public static final Config config = new Config();
    public static MainController controller;

    static List<String> gCode = new ArrayList<>();
    static TraceThread thread1 = new TraceThread(null);

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

            engine.run((p1, p2, state) -> {
                Line line = new Line(
                        p1.getX(),
                        p1.getY(),
                        p2.getX(),
                        p2.getY()
                );
                line.setStroke(state ? Color.BLACK : Color.GREEN);
                line.setStrokeWidth(0.5);
                uiUpdates.add(line);

                if (uiUpdates.size() >= BATCH_SIZE) {
                    List<Line> lines = new ArrayList<>(uiUpdates);
                    uiUpdates.clear();
                    Platform.runLater(() -> controller.pane.getChildren().addAll(lines));
                }
            }, this.processCanvas);

            if (uiUpdates.size() >= BATCH_SIZE) {
                List<Line> lines = new ArrayList<>(uiUpdates);
                uiUpdates.clear();
                Platform.runLater(() -> controller.pane.getChildren().addAll(lines));
            }
            /*
            Point2D point = new Point2D(
                    pixelData.getX() / 800.f * 100.f + 60,
                    pixelData.getY() / 800.f * 100.f + 55
            );

            float z = engine.radius == 1 ? 2.5f : 7;
            if (z != last_z) {
                gCode.addLast(format("G0 Z%f\n", z).replaceAll(",", "."));
                last_z = z;
            }
            gCode.addLast(format("G1 X%f Y%f F2200\n", point.getX(), point.getY()).replaceAll(",", "."));
             */
        }
    }

    public static void render() {
        if (thread1.isAlive()) return;

        Image inputImage = controller.filteredImage.getImage();
        int scaledOffsetX = (int) (inputImage.getWidth() - (controller.filteredImage.getScaleX() * inputImage.getWidth()));
        int scaledOffsetY = (int) (inputImage.getHeight() - (controller.filteredImage.getScaleY() * inputImage.getHeight()));
        WritableImage process_canvas = imageManager.insetImage(
                inputImage,
                Settings.WIDTH,
                Settings.HEIGHT,
                (int) controller.filteredImage.getTranslateX() + scaledOffsetX / 2,
                (int) controller.filteredImage.getTranslateY() + scaledOffsetY / 2,
                (int) (controller.filteredImage.getScaleX() * inputImage.getWidth() * ((double) 800 / Settings.WIDTH)),
                (int) (controller.filteredImage.getScaleY() * inputImage.getHeight() * ((double) 800 / Settings.HEIGHT))
        );

        controller.pane.setTranslateX(0);
        controller.pane.setTranslateY(0);
        controller.pane.setScaleX(1);
        controller.pane.setScaleY(1);

        thread1 = new TraceThread(process_canvas);
        thread1.setDaemon(true);
        thread1.start();
    }

    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        launch(args);
    }
}
