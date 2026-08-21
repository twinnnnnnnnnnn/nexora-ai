package com.stefano.aicompanion;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.stefano.aicompanion.ai.OllamaClient;
import com.stefano.aicompanion.ai.PlayerContext;
import com.stefano.aicompanion.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point for the client-side AI Companion mod.
 *
 * <p>Registers the {@code /ai} command family and owns the shared config,
 * Ollama client and a single-threaded executor that keeps network calls off the
 * render thread.</p>
 */
public class AiCompanionClient implements ClientModInitializer {

    public static final String MOD_ID = "aicompanion";
    public static final Logger LOGGER = LoggerFactory.getLogger("AI Companion");

    private static final Text PREFIX =
            Text.literal("[AI] ").formatted(Formatting.AQUA, Formatting.BOLD);

    private final OllamaClient ollama = new OllamaClient();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ai-companion-worker");
        thread.setDaemon(true);
        return thread;
    });

    private ModConfig config;

    @Override
    public void onInitializeClient() {
        this.config = ModConfig.load();
        LOGGER.info("AI Companion ready (model '{}' @ {})", config.model, config.ollamaUrl);
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("ai")
                        .then(ClientCommandManager.literal("reload")
                                .executes(ctx -> {
                                    this.config = ModConfig.load();
                                    feedback(Text.literal("Config reloaded. Model: " + config.model)
                                            .formatted(Formatting.GREEN));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("model")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            config.model = StringArgumentType.getString(ctx, "name");
                                            config.save();
                                            feedback(Text.literal("Model set to " + config.model)
                                                    .formatted(Formatting.GREEN));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.argument("prompt", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ask(StringArgumentType.getString(ctx, "prompt"));
                                    return 1;
                                }))
                        .executes(ctx -> {
                            feedback(Text.literal("Usage: /ai <question>  |  /ai model <name>  |  /ai reload")
                                    .formatted(Formatting.GRAY));
                            return 1;
                        })));
    }

    /** Handles a player question: echoes it, then answers asynchronously. */
    private void ask(String prompt) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }

        feedback(Text.literal("You asked: ").formatted(Formatting.GRAY)
                .append(Text.literal(prompt).formatted(Formatting.WHITE)));
        feedback(Text.literal("thinking...").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));

        String fullPrompt = buildPrompt(player, prompt);

        worker.submit(() -> {
            try {
                String answer = ollama.generate(config, fullPrompt);
                feedback(Text.literal(answer).formatted(Formatting.AQUA));
            } catch (OllamaClient.OllamaException e) {
                error(e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unexpected error while querying Ollama", e);
                error("Something went wrong: " + e.getMessage());
            }
        });
    }

    private String buildPrompt(ClientPlayerEntity player, String prompt) {
        if (!config.includePlayerContext) {
            return prompt;
        }
        String context = PlayerContext.describe(player);
        return context.isEmpty()
                ? prompt
                : context + "\n\nPlayer question: " + prompt;
    }

    /** Prints a normal message to the chat on the client thread. */
    private void feedback(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.empty().append(PREFIX).append(message), false);
            }
        });
    }

    /** Prints an error message (red) to the chat on the client thread. */
    private void error(String message) {
        feedback(Text.literal(message).formatted(Formatting.RED));
    }
}
