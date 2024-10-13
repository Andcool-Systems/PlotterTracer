package com.andcool.Tracer;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;

public interface Worker {
    Point2D work(Image image, int radius, Point2D pixelPos);
}
