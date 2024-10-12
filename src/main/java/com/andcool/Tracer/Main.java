package com.andcool.Tracer;


import com.andcool.Tracer.Controllers.MainController;
import com.andcool.Tracer.sillyLogger.Level;
import com.andcool.Tracer.sillyLogger.SillyLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Pair;

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
    public static MainController controller;

    static List<String> gCode = new ArrayList<>();
    static SliceThread thread1 = new SliceThread();

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("JavaFX Test");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainView.fxml"));
        Parent root = loader.load();

        controller = loader.getController();

        primaryStage.setScene(new Scene(root, 1920, 1000));
        primaryStage.show();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
        });
    }

    static class SliceThread extends Thread {
        @Override
        public void run() {
            Pair<Integer, Integer> last_pixel = new Pair<>(0, 0);
            engine.lastWorker = 0;
            gCode.clear();
            Platform.runLater(() -> controller.pane.getChildren().clear());
            WritableImage process_canvas = engine.copyImage(controller.filteredImage.getImage());
            PixelWriter canvasWriter = process_canvas.getPixelWriter();
            float last_z = 7;

            while (true) {
                try {
                    Pair<Integer, Integer> pixelData = engine.findNext(process_canvas, last_pixel);
                    if (pixelData == null) break;

                    Pair<Integer, Integer> finalLast_pixel = last_pixel;
                    int finalRadius = engine.radius;

                    canvasWriter.setColor(finalLast_pixel.getKey(), finalLast_pixel.getValue(), Color.WHITE);

                    Line line = new Line(
                            finalLast_pixel.getKey(),
                            finalLast_pixel.getValue(),
                            pixelData.getKey(),
                            pixelData.getValue());
                    line.setStroke(finalRadius == 1 ? Color.BLACK : Color.GREEN);
                    line.setStrokeWidth(0.5);
                    uiUpdates.add(line);

                    if (uiUpdates.size() >= BATCH_SIZE) {
                        List<Line> lines = new ArrayList<>(uiUpdates);
                        uiUpdates.clear();
                        Platform.runLater(() -> controller.pane.getChildren().addAll(lines));
                    }

                    Pair<Float, Float> point = new Pair<>(
                            pixelData.getKey() / 800.f * 100.f + 60,
                            pixelData.getValue() / 800.f * 100.f + 55
                    );

                    float z = engine.radius == 1 ? 2.5f : 7;
                    if (z != last_z) {
                        gCode.addLast(format("G0 Z%f\n", z).replaceAll(",", "."));
                        last_z = z;
                    }
                    gCode.addLast(format("G1 X%f Y%f F2200\n", point.getKey(), point.getValue()).replaceAll(",", "."));

                    last_pixel = pixelData;
                    //Thread.sleep(0, 1);
                } catch (InternalError e) {
                    logger.log(Level.ERROR, e.toString());
                }
            }
            if (!uiUpdates.isEmpty()) {
                List<Line> lines = new ArrayList<>(uiUpdates);
                uiUpdates.clear();
                Platform.runLater(() -> controller.pane.getChildren().addAll(lines));
            }
            logger.log(Level.INFO, "end");
        }
    }

    public static void render() {
        if (thread1.isAlive()) return;
        thread1 = new SliceThread();
        thread1.setDaemon(true);
        thread1.start();
    }

    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        launch(args);
    }
}
