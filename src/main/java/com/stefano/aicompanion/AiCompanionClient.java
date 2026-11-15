package com.stefano.aicompanion;

import com.stefano.aicompanion.ai.OllamaClient;
import com.stefano.aicompanion.ai.PlayerContext;
import com.stefano.aicompanion.ai.VoiceEngine;
import com.stefano.aicompanion.ai.VoiceRecorder;
import com.stefano.aicompanion.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.util.InputUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiCompanionClient implements ClientModInitializer {
    public static final String MOD_ID = "nexora_ai";
    public static final Logger LOGGER = LogManager.getLogger("Nexora AI");
    private static AiCompanionClient instance;

    private static final Text PREFIX = new LiteralText("[Nexora AI] ")
            .formatted(Formatting.AQUA, Formatting.BOLD);

    private final OllamaClient ollama = new OllamaClient();
    private final VoiceRecorder voiceRecorder = new VoiceRecorder();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nexora-ai-worker");
        thread.setDaemon(true);
        return thread;
    });

    private ModConfig config;
    private KeyBinding voiceKey;
    private boolean voiceKeyWasDown;
    private Path currentRecording;

    @Override
    public void onInitializeClient() {
        instance = this;
        config = ModConfig.load();
        voiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nexora_ai.push_to_talk",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.category.nexora_ai"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = voiceKey.isPressed();
            if (config.voiceEnabled && down && !voiceKeyWasDown) {
                startVoiceRecording();
            } else if (voiceKeyWasDown && !down) {
                stopVoiceRecording();
            }
            voiceKeyWasDown = down;
        });
        LOGGER.info("Nexora AI ready (model '{}' @ {})", config.model, config.ollamaUrl);
    }

    /**
     * Handles local /ai chat commands. Returning true means the message was consumed
     * and must not be sent to the server.
     */
    public static boolean handleChatCommand(String message) {
        if (instance == null || message == null) return false;
        if (!message.equals("/ai") && !message.startsWith("/ai ")) return false;

        String arguments = message.length() > 3 ? message.substring(4).trim() : "";
        if (arguments.isEmpty()) {
            instance.feedback(new LiteralText("Usage: /ai <question> | /ai model <name> | /ai reload")
                    .formatted(Formatting.GRAY));
            return true;
        }

        if (arguments.equals("reload")) {
            instance.config = ModConfig.load();
            instance.feedback(new LiteralText("Config reloaded. Model: " + instance.config.model)
                    .formatted(Formatting.GREEN));
            return true;
        }

        if (arguments.startsWith("model ")) {
            String model = arguments.substring(6).trim();
            if (model.isEmpty()) {
                instance.feedback(new LiteralText("Usage: /ai model <name>").formatted(Formatting.GRAY));
            } else {
                instance.config.model = model;
                instance.config.save();
                instance.feedback(new LiteralText("Model set to " + model).formatted(Formatting.GREEN));
            }
            return true;
        }

        instance.ask(arguments);
        return true;
    }

    private void startVoiceRecording() {
        if (MinecraftClient.getInstance().player == null) return;
        try {
            currentRecording = Files.createTempFile("nexora-voice-", ".wav");
            feedback(new LiteralText("Listening... release V to send").formatted(Formatting.GREEN));
            voiceRecorder.start(currentRecording,
                    () -> { },
                    error -> {
                        LOGGER.error("Microphone recording failed", error);
                        feedback(new LiteralText("Microphone error: " + error.getMessage())
                                .formatted(Formatting.RED));
                    });
        } catch (Exception e) {
            error("Could not start microphone: " + e.getMessage());
        }
    }

    private void stopVoiceRecording() {
        if (currentRecording == null) return;
        final Path recording = currentRecording;
        currentRecording = null;
        voiceRecorder.stop();
        worker.submit(() -> {
            try {
                String transcript = VoiceEngine.transcribe(config, recording,
                        MinecraftClient.getInstance().runDirectory.toPath());
                Files.deleteIfExists(recording);
                if (transcript.trim().isEmpty()) {
                    feedback(new LiteralText("I didn't catch that. Try again.").formatted(Formatting.GRAY));
                    return;
                }
                feedback(new LiteralText("You said: " + transcript).formatted(Formatting.GRAY));
                ask(transcript);
            } catch (Exception e) {
                try { Files.deleteIfExists(recording); } catch (Exception ignored) { }
                LOGGER.error("Voice transcription failed", e);
                error("Voice recognition failed: " + e.getMessage());
            }
        });
    }

    private void ask(String prompt) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            feedback(new LiteralText("You must be in a world to ask a question.").formatted(Formatting.RED));
            return;
        }

        feedback(new LiteralText("You asked: ").formatted(Formatting.GRAY)
                .append(new LiteralText(prompt).formatted(Formatting.WHITE)));
        feedback(new LiteralText("thinking...").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));

        final String fullPrompt = buildPrompt(player, prompt);
        worker.submit(() -> {
            try {
                String answer = ollama.generate(config, fullPrompt);
                feedback(new LiteralText(answer).formatted(Formatting.AQUA));
                if (config.voiceEnabled && config.speakReplies) {
                    VoiceEngine.speak(config, answer, MinecraftClient.getInstance().runDirectory.toPath());
                }
            } catch (OllamaClient.OllamaException e) {
                error(e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unexpected error while querying Ollama or speaking reply", e);
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
