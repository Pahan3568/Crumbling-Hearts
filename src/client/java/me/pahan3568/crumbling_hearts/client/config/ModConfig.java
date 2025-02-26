package me.pahan3568.crumbling_hearts.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;


public class ModConfig {
    private static ModConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("crumbling_hearts.json");

    // Настройки по умолчанию
    private String normalHeartColor = "#FF0000";  // Красный для обычных сердец
    private String extraHeartColor = "#FFFF00";   // Желтый для дополнительных сердец
    private int particlesPerHeart = 64;
    private float gravityStrength = 0.04f;
    private float initialVelocity = 1.0f;
    private float fadeSpeed = 1.0f;

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadConfig();
        }
        return INSTANCE;
    }

    private static ModConfig loadConfig() {
        try {
            if (CONFIG_PATH.toFile().exists()) {
                try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                    return GSON.fromJson(reader, ModConfig.class);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
        ModConfig config = new ModConfig();
        config.saveConfig();
        return config;
    }

    public void saveConfig() {
        try {
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    // Геттеры и сеттеры
    public int getNormalHeartColor() {
        return Integer.parseInt(normalHeartColor.replace("#", ""), 16);
    }

    public int getExtraHeartColor() {
        return Integer.parseInt(extraHeartColor.replace("#", ""), 16);
    }

    public void setNormalHeartColor(String value) {
        normalHeartColor = value;
        saveConfig();
    }

    public void setExtraHeartColor(String value) {
        extraHeartColor = value;
        saveConfig();
    }

    public int getParticlesPerHeart() { return particlesPerHeart; }
    public void setParticlesPerHeart(int value) {
        particlesPerHeart = value;
        saveConfig();
    }

    public float getGravityStrength() { return gravityStrength; }
    public void setGravityStrength(float value) {
        gravityStrength = value;
        saveConfig();
    }

    public float getInitialVelocity() { return initialVelocity; }
    public void setInitialVelocity(float value) {
        initialVelocity = value;
        saveConfig();
    }

    public float getFadeSpeed() { return fadeSpeed; }
    public void setFadeSpeed(float value) {
        fadeSpeed = value;
        saveConfig();
    }

    // Добавим геттеры для строковых значений цветов
    public String getNormalHeartColorString() {
        return normalHeartColor;
    }

    public String getExtraHeartColorString() {
        return extraHeartColor;
    }
}