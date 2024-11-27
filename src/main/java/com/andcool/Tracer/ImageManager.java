package com.andcool.Tracer;

import com.andcool.Tracer.SillyLogger.Level;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
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
        return color.getOpacity() > 0 ? (color.getRed() + color.getGreen() + color.getBlue()) / 3 * 255 : 255;
    }

    public WritableImage insetImage(
            Image img,
            int canvasWidth,
            int canvasHeight,
            int posX,
            int posY,
            int targetWidth,
            int targetHeight
    ) {
        int originalWidth = (int) img.getWidth();
        int originalHeight = (int) img.getHeight();

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
                if (x + posX < 0 || x + posX > canvasWidth || y + posY < 0 || y + posY > canvasHeight) continue;
                pixelWriter.setColor(x + posX, y + posY, color);
            }
        }
        canvas.snapshot(null, scaledImage);
        return scaledImage;
    }

    public void updateProcessed() {
        if (Main.imageManager.sourceImage == null) return;
        processedImage = applyThreshold(Main.imageManager.sourceImage, (int) Main.controller.threshold.getValue());
        //processedImage = insetImage(processedImage, Settings.WIDTH, Settings.HEIGHT);
        Main.controller.filteredImage.setImage(processedImage);
    }
}
