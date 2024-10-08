package com.andcool.Tracer;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.util.Pair;

public class Engine {
    private final Worker[] workers;
    public int lastWorker = 0;
    public int radius = 1;

    Engine() {
        workers = new Worker[]{
                this::top,
                this::right,
                this::bottom,
                this::left,
        };
    }

    Pair<Integer, Integer> top(Image image, int radius, Pair<Integer, Integer> pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int x = -radius; x < radius + 1; x++) {
            try {
                if (pixelReader.getColor(pixelPos.getKey() + x, pixelPos.getValue() - radius).getBrightness() < 1) {
                    return new Pair<>(pixelPos.getKey() + x, pixelPos.getValue() - radius);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    Pair<Integer, Integer> right(Image image, int radius, Pair<Integer, Integer> pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int y = -radius; y < radius; y++) {
            try {
                if (pixelReader.getColor(pixelPos.getKey() + radius, pixelPos.getValue() + y).getBrightness() < 1) {
                    return new Pair<>(pixelPos.getKey() + radius, pixelPos.getValue() + y);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    Pair<Integer, Integer> bottom(Image image, int radius, Pair<Integer, Integer> pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int x = radius; x > -radius; x--) {
            try {
                if (pixelReader.getColor(pixelPos.getKey() + x, pixelPos.getValue() + radius).getBrightness() < 1) {
                    return new Pair<>(pixelPos.getKey() + x, pixelPos.getValue() + radius);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    Pair<Integer, Integer> left(Image image, int radius, Pair<Integer, Integer> pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int y = radius; y > -radius; y--) {
            try {
                if (pixelReader.getColor(pixelPos.getKey() - radius, pixelPos.getValue() + y).getBrightness() < 1) {
                    return new Pair<>(pixelPos.getKey() - radius, pixelPos.getValue() + y);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    public Pair<Integer, Integer> findNext(WritableImage image, Pair<Integer, Integer> last_pixel) {
        int width = (int) image.getWidth();
        radius = 1;
        while (true) {
            for (int number = 0; number < 4; number++) {
                int worker = (number + lastWorker) % 4;
                if (worker < 0) worker += 4;

                Pair<Integer, Integer> result = workers[worker].work(image, radius, last_pixel);
                if (result != null) {
                    this.lastWorker = worker - 1;
                    return result;
                }
            }
            this.lastWorker = 0;
            ++radius;
            if (radius > width) return null;
        }
    }

    public WritableImage copyImage(Image inputImage) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();

        WritableImage writableImage = new WritableImage(width, height);
        PixelReader pixelReader = inputImage.getPixelReader();

        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                pixelWriter.setColor(x, y, color);
            }
        }
        return writableImage;
    }
}
