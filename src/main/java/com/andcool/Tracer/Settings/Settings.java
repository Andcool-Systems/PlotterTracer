package com.andcool.Tracer.Settings;

import com.andcool.Tracer.Main;
import com.andcool.Tracer.SillyLogger.Level;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Settings {
    public static int WIDTH = 100;
    public static int HEIGHT = 100;
    public static float LINE_WIDTH = 0.5F;
    public static boolean MIRROR_X = false;

    /*
    Save config to file
     */
    public static void save() {
        final File configFile = new File("./config.json");
        JSONObject jsonConfig = new JSONObject();
        jsonConfig.put("WIDTH", WIDTH);
        jsonConfig.put("HEIGHT", HEIGHT);
        jsonConfig.put("LINE_WIDTH", LINE_WIDTH);
        jsonConfig.put("MIRROR_X", MIRROR_X);
        try {
            Files.createDirectories(configFile.toPath().getParent());
            Files.writeString(configFile.toPath(), jsonConfig.toString(4));
        } catch (IOException e) {
            Main.logger.log(Level.ERROR, e, true);
        }
    }

    /*
    Load config from file
     */
    public static void load() {
        final File configFile = new File("./config.json");
        try {
            JSONObject jsonConfig = new JSONObject(Files.readString(configFile.toPath()));
            for (String key : jsonConfig.keySet()) {
                switch (key) {
                    case "WIDTH" -> WIDTH = jsonConfig.getInt(key);
                    case "HEIGHT" -> HEIGHT = jsonConfig.getInt(key);
                    case "LINE_WIDTH" -> LINE_WIDTH = jsonConfig.getFloat(key);
                    case "MIRROR_X" -> MIRROR_X = jsonConfig.getBoolean(key);
                }
            }
            Main.logger.log(Level.DEBUG, "Settings loaded!");
        } catch (Exception e) {
            Main.logger.log(Level.WARN, e, true);
            save();
        }
    }
}