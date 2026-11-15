package com.stefano.aicompanion;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.stefano.aicompanion.ai.OllamaClient;
import com.stefano.aicompanion.ai.PlayerContext;
import com.stefano.aicompanion.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiCompanionClient implements ClientModInitializer {
    public static final String MOD_ID = "nexora_ai";
    public static final Logger LOGGER = LogManager.getLogger("Nexora AI");

    private static final Text PREFIX = new LiteralText("[Nexora AI] ")
            .formatted(Formatting.AQUA, Formatting.BOLD);

    private final OllamaClient ollama = new OllamaClient();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nexora-ai-worker");
        thread.setDaemon(true);
        return thread;
    });

    private ModConfig config;

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();
        LOGGER.info("Nexora AI ready (model '{}' @ {})", config.model, config.ollamaUrl);
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("ai")
                .then(ClientCommandManager.literal("reload").executes(context -> {
                    config = ModConfig.load();
                    feedback(new LiteralText("Config reloaded. Model: " + config.model)
                            .formatted(Formatting.GREEN));
                    return 1;
                }))
                .then(ClientCommandManager.literal("model")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    config.model = StringArgumentType.getString(context, "name");
                                    config.save();
                                    feedback(new LiteralText("Model set to " + config.model)
                                            .formatted(Formatting.GREEN));
                                    return 1;
                                })))
                .then(ClientCommandManager.argument("prompt", StringArgumentType.greedyString())
                        .executes(context -> {
                            ask(StringArgumentType.getString(context, "prompt"));
                            return 1;
                        }))
                .executes(context -> {
                    feedback(new LiteralText("Usage: /ai <question> | /ai model <name> | /ai reload")
                            .formatted(Formatting.GRAY));
                    return 1;
                }));
    }

    private void ask(String prompt) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        feedback(new LiteralText("You asked: ").formatted(Formatting.GRAY)
                .append(new LiteralText(prompt).formatted(Formatting.WHITE)));
        feedback(new LiteralText("thinking...").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));

        final String fullPrompt = buildPrompt(player, prompt);
        worker.submit(() -> {
            try {
                feedback(new LiteralText(ollama.generate(config, fullPrompt)).formatted(Formatting.AQUA));
            } catch (OllamaClient.OllamaException e) {
                error(e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unexpected error while querying Ollama", e);
                error("Something went wrong: " + e.getMessage());
            }
        });
    }

    private String buildPrompt(ClientPlayerEntity player, String prompt) {
        if (!config.includePlayerContext) return prompt;
        String context = PlayerContext.describe(player);
        return context.isEmpty() ? prompt : context + "\n\nPlayer question: " + prompt;
    }

    private void feedback(Text message) {
        final MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(new LiteralText("").append(PREFIX).append(message));
            }
        });
    }

    private void error(String message) {
        feedback(new LiteralText(message).formatted(Formatting.RED));
    }
}