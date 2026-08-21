package com.stefano.aicompanion.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stefano.aicompanion.AiCompanionClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User-editable configuration, persisted as {@code config/aicompanion.json}.
 *
 * <p>The file is created with sensible defaults on first launch and can be
 * reloaded at runtime with {@code /ai reload}.</p>
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("aicompanion.json");

    /** Base URL of the Ollama server (no trailing slash). */
    public String ollamaUrl = "http://localhost:11434";

    /** Model tag to query, e.g. {@code llama3.2}, {@code mistral}, {@code qwen2.5}. */
    public String model = "llama3.2";

    /** Whether to attach live player context (health, position, ...) to each prompt. */
    public boolean includePlayerContext = true;

    /** Sampling temperature passed to Ollama. */
    public double temperature = 0.7;

    /** Request timeout in seconds. Large models on cold start can be slow. */
    public int timeoutSeconds = 90;

    /** System prompt that defines the assistant's persona and answer style. */
    public String systemPrompt =
            "You are a helpful in-game companion living inside the player's Minecraft world. "
                    + "Answer concisely and practically. When the player asks about crafting, mobs, "
                    + "redstone or survival strategy, give accurate, actionable advice. Keep replies "
                    + "under a few sentences unless more detail is clearly needed. Do not use markdown.";

    /**
     * Loads the config from disk, writing a default file if none exists.
     * Never throws: on any error it logs and returns a default instance so the
     * mod stays usable.
     */
    public static ModConfig load() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                ModConfig defaults = new ModConfig();
                defaults.save();
                return defaults;
            }
            String json = Files.readString(CONFIG_PATH);
            ModConfig loaded = GSON.fromJson(json, ModConfig.class);
            return loaded != null ? loaded : new ModConfig();
        } catch (Exception e) {
            AiCompanionClient.LOGGER.error("Failed to load config, using defaults", e);
            return new ModConfig();
        }
    }

    /** Writes the current values back to {@code config/aicompanion.json}. */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            AiCompanionClient.LOGGER.error("Failed to save config", e);
        }
    }
}
