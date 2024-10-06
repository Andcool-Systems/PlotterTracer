package com.andcool.javafx_test;

import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class Files {
    public static void export(List<String> gCode) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./test.gcode"));
        writer.write("G28\n");
        writer.write("G0 Z5\n");
        writer.write("G0 X50 Y50 F2200\n");

        for (String line : gCode) {
            writer.write(line);
        }
        writer.write("G0 Z5\n");
        writer.write("G28");

        writer.close();
    }
}
