package com.stefano.aicompanion.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stefano.aicompanion.AiCompanionClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("nexora-ai.json");

    public String ollamaUrl = "http://localhost:11434";
    public String model = "llama3.2";
    public boolean includePlayerContext = true;
    public double temperature = 0.7;
    public int timeoutSeconds = 90;
    public String systemPrompt =
            "You are a helpful in-game companion living inside the player's Minecraft world. "
                    + "Answer concisely and practically. When the player asks about crafting, mobs, "
                    + "redstone or survival strategy, give accurate, actionable advice. Keep replies "
                    + "under a few sentences unless more detail is clearly needed. Do not use markdown.";

    public static ModConfig load() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                ModConfig defaults = new ModConfig();
                defaults.save();
                return defaults;
            }
            String json = new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8);
            ModConfig loaded = GSON.fromJson(json, ModConfig.class);
            return loaded != null ? loaded : new ModConfig();
        } catch (Exception e) {
            AiCompanionClient.LOGGER.error("Failed to load config, using defaults", e);
            return new ModConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(CONFIG_PATH, GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            AiCompanionClient.LOGGER.error("Failed to save config", e);
        }
    }
}