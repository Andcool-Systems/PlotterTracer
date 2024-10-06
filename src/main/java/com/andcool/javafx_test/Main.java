package com.andcool.javafx_test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

public class Main extends Application {
    private static final int BATCH_SIZE = 50;
    private List<Runnable> uiUpdates = new ArrayList<>();

    private Image originalImage;
    private ImageView processedImageView;
    private Slider thresholdSlider;
    private Label thresholdLabel;
    private Canvas canvas;
    HBox imageBox;
    private final Engine engine = new Engine();
    List<String> gCode = new ArrayList<>();
    SliceThread thread1 = new SliceThread();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Test");

        // Создание панели инструментов
        ToolBar toolBar = new ToolBar();
        Button btnLoadImage = new Button("Load image");
        toolBar.getItems().add(btnLoadImage);
        btnLoadImage.setOnAction(e -> openImage(primaryStage));

        Button btnRenderImage = new Button("Render");
        toolBar.getItems().add(btnRenderImage);
        btnRenderImage.setOnAction(e -> Render());

        Button btExportImage = new Button("Export");
        toolBar.getItems().add(btExportImage);
        btExportImage.setOnAction(e -> {
            try {
                Files.export(this.gCode);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Создание ImageView для отображения обработанного изображения
        processedImageView = new ImageView();
        processedImageView.setPreserveRatio(true);
        processedImageView.setFitWidth(400);
        processedImageView.setFitHeight(400);

        canvas = new Canvas(1920, 1800);

        // Создание ползунка для задания порога
        thresholdSlider = new Slider(0, 255, 128);
        thresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            thresholdLabel.setText("Порог: " + newValue.intValue());
            applyThreshold(newValue.intValue());
        });

        // Создание метки для отображения текущего порога
        thresholdLabel = new Label("Порог: 128");

        // Создание панели для ползунка и метки
        HBox thresholdBox = new HBox(10, thresholdSlider, thresholdLabel);
        thresholdBox.setPadding(new Insets(10));

        // Создание основного рабочего пространства
        imageBox = new HBox(10, processedImageView, canvas);
        imageBox.setPadding(new Insets(10));

        BorderPane mainPane = new BorderPane();
        mainPane.setTop(new VBox(toolBar, thresholdBox));
        mainPane.setCenter(imageBox);

        // Создание и установка сцены
        Scene scene = new Scene(mainPane, 1920, 1000);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println(throwable.toString());
        });
    }

    class SliceThread extends Thread {
        @Override
        public void run() {
            Pair<Integer, Integer> last_pixel = new Pair<>(0, 0);
            engine.lastWorker = 0;
            gCode.clear();
            canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            WritableImage process_canvas = engine.copyImage(processedImageView.getImage());
            PixelWriter canvasWriter = process_canvas.getPixelWriter();
            //float last_z = 2;

            while (true) {
                try {
                    Pair<Integer, Integer> pixelData = engine.findNext(process_canvas, last_pixel);
                    if (pixelData == null) break;

                    Pair<Integer, Integer> finalLast_pixel = last_pixel;
                    int finalRadius = engine.radius;
                    canvasWriter.setColor(finalLast_pixel.getKey(), finalLast_pixel.getValue(), Color.WHITE);
                    uiUpdates.add(() -> {
                        drawLineOnCanvas(canvas,
                                finalLast_pixel.getKey() * 2,
                                finalLast_pixel.getValue() * 2,
                                pixelData.getKey() * 2,
                                pixelData.getValue() * 2,
                                finalRadius == 1 ? Color.BLACK : Color.GREEN);
                    });

                    if (uiUpdates.size() >= BATCH_SIZE) {
                        List<Runnable> updatesToRun = new ArrayList<>(uiUpdates);
                        uiUpdates.clear();
                        Platform.runLater(() -> updatesToRun.forEach(Runnable::run));
                    }

                    /*
                    Pair<Float, Float> point = new Pair<>(
                            pixelData.getKey() / 400.f * 100.f + 50,
                            pixelData.getValue() / 400.f * 100.f + 50
                    );


                    float z = engine.radius == 1 ? 0.1f : 2;
                    if (z != last_z) {
                        gCode.addLast(format("G0 Z%f\n", z).replaceAll(",", "."));
                        last_z = z;
                    }
                    gCode.addLast(format("G1 X%f Y%f F1200\n", point.getKey(), point.getValue()).replaceAll(",", "."));
                     */


                    last_pixel = pixelData;
                    //Thread.sleep(0, 1);
                } catch (InternalError e) {
                    System.out.println(e.toString());
                    break;
                }
            }
            if (!uiUpdates.isEmpty()) {
                List<Runnable> updatesToRun = new ArrayList<>(uiUpdates);
                uiUpdates.clear();
                Platform.runLater(() -> updatesToRun.forEach(Runnable::run));
            }
            System.out.println("end");
        }
    }

    private void Render() {
        if (thread1.isAlive()) return;
        imageBox.getChildren().remove(1);
        canvas = new Canvas(800, 800);
        imageBox.getChildren().add(1, canvas);

        thread1 = new SliceThread();
        thread1.setDaemon(true);
        thread1.start();
    }

    private void drawLineOnCanvas(Canvas canvas, double x0, double y0, double x1, double y1, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(color);
        gc.strokeLine(x0, y0, x1, y1);
    }

    private void openImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");

        // Установка фильтров для файлов изображений
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.jfif");
        fileChooser.getExtensionFilters().add(imageFilter);

        // Открытие диалогового окна и получение выбранного файла
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                // Загрузка изображения
                originalImage = scaleImage(new Image(new FileInputStream(file)), (int) processedImageView.getFitWidth(), (int) processedImageView.getFitHeight());
                // Применение пороговой фильтрации с текущим значением порога
                applyThreshold((int) thresholdSlider.getValue());
            } catch (FileNotFoundException ex) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить изображение.");
            }
        }
    }

    private void applyThreshold(int threshold) {
        if (originalImage == null) {
            processedImageView.setImage(null);
            return;
        }

        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        WritableImage processedImage = new WritableImage(width, height);
        PixelReader pixelReader = originalImage.getPixelReader();
        PixelWriter pixelWriter = processedImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                double luminance = getLuminance(color);
                Color newColor = luminance >= threshold ? Color.WHITE : Color.BLACK;
                pixelWriter.setColor(x, y, newColor);
            }
        }

        processedImageView.setImage(processedImage);
    }

    public static WritableImage scaleImage(Image inputImage, int targetWidth, int targetHeight) {
        WritableImage scaledImage = new WritableImage(targetWidth, targetHeight);
        PixelReader pixelReader = inputImage.getPixelReader();
        PixelWriter pixelWriter = scaledImage.getPixelWriter();

        int originalWidth = (int) inputImage.getWidth();
        int originalHeight = (int) inputImage.getHeight();

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int origX = (int) ((double) x / targetWidth * originalWidth);
                int origY = (int) ((double) y / targetHeight * originalHeight);
                Color color = pixelReader.getColor(origX, origY);
                pixelWriter.setColor(x, y, color);
            }
        }

        return scaledImage;
    }

    private double getLuminance(Color color) {
        // Используем стандартную формулу для вычисления яркости
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3 * 255;
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        launch(args);
    }
}
