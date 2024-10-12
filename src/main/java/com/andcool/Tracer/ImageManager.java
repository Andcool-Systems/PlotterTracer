package com.andcool.Tracer;

import com.andcool.Tracer.sillyLogger.Level;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImageManager {
    public Image sourceImage;
    public static WritableImage processedImage;

    public void openImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");

        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.jfif");
        fileChooser.getExtensionFilters().add(imageFilter);

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                sourceImage = new Image(new FileInputStream(file));
            } catch (FileNotFoundException ex) {
                Main.logger.log(Level.ERROR, "Could not load image");
            }
        }
    }

    private static WritableImage applyThreshold(Image img, int threshold) {
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

    private static double getLuminance(Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3 * 255;
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

    public void updateProcessed() {
        processedImage = applyThreshold(Main.imageManager.sourceImage, (int) Main.controller.threshold.getValue());
        processedImage = insetImage(processedImage, Main.controller.filteredImage);
        Main.controller.filteredImage.setImage(processedImage);
    }
}
