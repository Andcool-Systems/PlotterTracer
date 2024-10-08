package com.andcool.Tracer;


import com.andcool.Tracer.sillyLogger.Level;
import com.andcool.Tracer.sillyLogger.SillyLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class Main extends Application {
    private static final int BATCH_SIZE = 50;
    private final List<Runnable> uiUpdates = new ArrayList<>();
    private final SillyLogger logger = new SillyLogger("Main Thread", true, Level.DEBUG);

    private Image originalImage;
    Image img;
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

        processedImageView = new ImageView();
        processedImageView.setPreserveRatio(true);
        processedImageView.setFitWidth(800);
        processedImageView.setFitHeight(800);
        processedImageView.setOnScroll(e -> {
        });

        canvas = new Canvas(1920, 1800);

        thresholdSlider = new Slider(0, 255, 128);
        thresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateProcessed();
        });
        thresholdLabel = new Label("Порог: 128");
        HBox thresholdBox = new HBox(10, thresholdSlider, thresholdLabel);
        thresholdBox.setPadding(new Insets(10));

        imageBox = new HBox(10, processedImageView, canvas);
        imageBox.setPadding(new Insets(10));

        BorderPane mainPane = new BorderPane();
        mainPane.setTop(new VBox(toolBar, thresholdBox));
        mainPane.setCenter(imageBox);

        Scene scene = new Scene(mainPane, 1920, 1000);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.log(Level.ERROR, throwable.toString());
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
            float last_z = 2;

            while (true) {
                try {
                    Pair<Integer, Integer> pixelData = engine.findNext(process_canvas, last_pixel);
                    if (pixelData == null) break;

                    Pair<Integer, Integer> finalLast_pixel = last_pixel;
                    int finalRadius = engine.radius;

                    canvasWriter.setColor(finalLast_pixel.getKey(), finalLast_pixel.getValue(), Color.WHITE);
                    uiUpdates.add(() -> {
                        drawLineOnCanvas(canvas,
                                finalLast_pixel.getKey(),
                                finalLast_pixel.getValue(),
                                pixelData.getKey(),
                                pixelData.getValue(),
                                finalRadius == 1 ? Color.BLACK : Color.GREEN);
                    });

                    if (uiUpdates.size() >= BATCH_SIZE) {
                        List<Runnable> updatesToRun = new ArrayList<>(uiUpdates);
                        uiUpdates.clear();
                        Platform.runLater(() -> updatesToRun.forEach(Runnable::run));
                    }

                    Pair<Float, Float> point = new Pair<>(
                            pixelData.getKey() / 800.f * 100.f + 50,
                            pixelData.getValue() / 800.f * 100.f + 50
                    );

                    float z = engine.radius == 1 ? 0.1f : 2;
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
                List<Runnable> updatesToRun = new ArrayList<>(uiUpdates);
                uiUpdates.clear();
                Platform.runLater(() -> updatesToRun.forEach(Runnable::run));
            }
            logger.log(Level.INFO, "end");
        }
    }

    private void Render() {
        if (thread1.isAlive()) return;
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

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.jfif");
        fileChooser.getExtensionFilters().add(imageFilter);

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                img = new Image(new FileInputStream(file));
                updateProcessed();
            } catch (FileNotFoundException ex) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить изображение.");
            }
        }
    }

    private void updateProcessed() {
        Image processedImage = insetImage(img, processedImageView);
        processedImage = applyThreshold(processedImage, (int) thresholdSlider.getValue());
        processedImageView.setImage(processedImage);
    }

    private WritableImage applyThreshold(Image img, int threshold) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();

        WritableImage processedImage = new WritableImage(width, height);
        PixelReader pixelReader = img.getPixelReader();
        PixelWriter pixelWriter = processedImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                double luminance = getLuminance(color);
                Color newColor = luminance >= threshold ? Color.WHITE : Color.BLACK;
                pixelWriter.setColor(x, y, newColor);
            }
        }
        return processedImage;
    }

    public static WritableImage insetImage(
            Image img,
            ImageView processedImageView
    ) {
        int canvasWidth = (int) processedImageView.getFitWidth();
        int canvasHeight = (int) processedImageView.getFitHeight();

        int originalWidth = (int) img.getWidth();
        int originalHeight = (int) img.getHeight();

        float factor = img.getWidth() > img.getHeight() ?
                (float) canvasWidth / originalWidth :
                (float) canvasHeight / originalHeight;

        int targetWidth = (int) (originalWidth * factor);
        int targetHeight = (int) (originalHeight * factor);

        int posX = (int) ((canvasWidth - (originalWidth * factor)) / 2);
        int posY = (int) ((canvasHeight - (originalHeight * factor)) / 2);

        WritableImage scaledImage = new WritableImage(canvasWidth, canvasHeight);
        PixelReader pixelReader = img.getPixelReader();

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        PixelWriter pixelWriter = gc.getPixelWriter();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int origX = (int) ((double) x / targetWidth * originalWidth);
                int origY = (int) ((double) y / targetHeight * originalHeight);
                Color color = pixelReader.getColor(origX, origY);
                pixelWriter.setColor(x + posX, y + posY, color);
            }
        }
        canvas.snapshot(null, scaledImage);
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
