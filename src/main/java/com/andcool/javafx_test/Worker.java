package com.andcool.javafx_test;

import javafx.scene.image.Image;
import javafx.util.Pair;

public interface Worker {
    Pair<Integer, Integer> work(Image image, int radius, Pair<Integer, Integer> pixelPos);
}
