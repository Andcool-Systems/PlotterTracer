package com.andcool.Tracer;


import com.andcool.Tracer.SillyLogger.Level;
import com.andcool.Tracer.SillyLogger.SillyLogger;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import static java.lang.Math.*;

public class Engine {
    public static final SillyLogger logger = new SillyLogger("Trace Engine", true, Level.DEBUG);
    private final Worker[] workers;
    public int lastWorker = 0;
    public int radius = 1;
    public boolean lastState = false;
    public Point2D savedPoint = new Point2D(0, 0);

    Engine() {
        workers = new Worker[]{
                this::top,
                this::right,
                this::bottom,
                this::left,
        };
    }

    Point2D top(Image image, int radius, Point2D pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int x = -radius; x < radius + 1; x++) {
            try {
                if (pixelReader.getColor((int) (pixelPos.getX() + x), (int) (pixelPos.getY() - radius)).getBrightness() < 1) {
                    return new Point2D(pixelPos.getX() + x, pixelPos.getY() - radius);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    Point2D right(Image image, int radius, Point2D pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int y = -radius; y < radius; y++) {
            try {
                if (pixelReader.getColor((int) (pixelPos.getX() + radius), (int) (pixelPos.getY() + y)).getBrightness() < 1) {
                    return new Point2D(pixelPos.getX() + radius, pixelPos.getY() + y);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    Point2D bottom(Image image, int radius, Point2D pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int x = radius; x > -radius; x--) {
            try {
                if (pixelReader.getColor((int) (pixelPos.getX() + x), (int) (pixelPos.getY() + radius)).getBrightness() < 1) {
                    return new Point2D(pixelPos.getX() + x, pixelPos.getY() + radius);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    Point2D left(Image image, int radius, Point2D pixelPos) {
        PixelReader pixelReader = image.getPixelReader();
        for (int y = radius; y > -radius; y--) {
            try {
                if (pixelReader.getColor((int) (pixelPos.getX() - radius), (int) (pixelPos.getY() + y)).getBrightness() < 1) {
                    return new Point2D(pixelPos.getX() - radius, pixelPos.getY() + y);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        return null;
    }

    public Point2D findNext(WritableImage image, Point2D last_pixel) {
        int width = (int) image.getWidth();
        radius = 1;
        while (true) {
            for (int number = 0; number < 4; number++) {
                int worker = (number + lastWorker) % 4;
                if (worker < 0) worker += 4;

                Point2D result = workers[worker].work(image, radius, last_pixel);
                if (result != null) {
                    lastWorker = worker - 1;
                    return result;
                }
            }
            lastWorker = 0;
            ++radius;
            if (radius > width) return null;
        }
    }

    public void run(Callback callback, WritableImage process_canvas) {
        Point2D last_pixel = new Point2D(0, 0);
        PixelWriter canvasWriter = process_canvas.getPixelWriter();
        boolean lastDown = false;
        double lastAngle = 0;
        lastWorker = 0;
        savedPoint = new Point2D(0, 0);
        int counter = 0;

        while (true) {
            try {
                Point2D pixelData = findNext(process_canvas, last_pixel);
                if (pixelData == null) break;
                canvasWriter.setColor((int) last_pixel.getX(), (int) last_pixel.getY(), Color.WHITE);

                boolean down = radius == 1;
                double angle = getAngle(
                        last_pixel.getX(),
                        last_pixel.getY(),
                        pixelData.getX(),
                        pixelData.getY()
                );

                if (down != lastDown || ((counter > Main.controller.smoothStep.getValue() || angle != lastAngle) && lastDown)) {
                    Point2D next_point;
                    if (!lastDown) {
                        next_point = last_pixel;
                    } else {
                        next_point = new Point2D(
                                (savedPoint.getX() + last_pixel.getX()) / 2,
                                (savedPoint.getY() + last_pixel.getY()) / 2);
                    }

                    callback.execute(savedPoint, next_point, lastDown);

                    lastAngle = angle;
                    savedPoint = next_point;
                    lastDown = down;
                    counter = 0;
                }

                last_pixel = pixelData;
                ++counter;
            } catch (Throwable throwable) {
                logger.log(Level.ERROR, throwable, true);
                break;
            }
        }
        logger.log(Level.INFO, "Tracing finished");
    }

    public static double getAngle(double x1, double y1, double x2, double y2) {
        double range_r = sqrt(pow(x1 - x2, 2) + pow(y1 - y2, 2));
        if (range_r == 0) return 0.f;

        double angle = acos((x2 - x1) / range_r) * (180.f / PI);
        if (y1 > y2) angle = 180.f + (180.f - angle);

        return angle;
    }

}

@FunctionalInterface
interface Callback {
    void execute(Point2D p1, Point2D p2, boolean state);
}