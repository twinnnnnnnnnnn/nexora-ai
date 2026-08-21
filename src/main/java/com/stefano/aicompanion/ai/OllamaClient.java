package com.stefano.aicompanion.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stefano.aicompanion.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal client for the local <a href="https://ollama.com">Ollama</a> HTTP API.
 *
 * <p>Uses the non-streaming {@code /api/generate} endpoint so a full reply is
 * returned in one shot, which maps cleanly onto a single chat message.</p>
 */
public class OllamaClient {

    private final HttpClient httpClient;

    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends a prompt to Ollama and returns the model's reply.
     *
     * @param config      active configuration (server url, model, timeout, ...)
     * @param userPrompt  the player's question, optionally prefixed with context
     * @return the assistant's answer text
     * @throws OllamaException if the request fails or the server returns an error
     */
    public String generate(ModConfig config, String userPrompt) throws OllamaException {
        JsonObject options = new JsonObject();
        options.addProperty("temperature", config.temperature);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model);
        body.addProperty("prompt", userPrompt);
        body.addProperty("system", config.systemPrompt);
        body.addProperty("stream", false);
        body.add("options", options);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.ollamaUrl + "/api/generate"))
                .timeout(Duration.ofSeconds(config.timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.ConnectException e) {
            throw new OllamaException(
                    "Could not reach Ollama at " + config.ollamaUrl
                            + ". Is it running? Try 'ollama serve'.", e);
        } catch (Exception e) {
            throw new OllamaException("Request to Ollama failed: " + e.getMessage(), e);
        }

        if (response.statusCode() == 404) {
            throw new OllamaException(
                    "Model '" + config.model + "' not found. Pull it first with 'ollama pull "
                            + config.model + "'.");
        }
        if (response.statusCode() != 200) {
            throw new OllamaException(
                    "Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) {
                throw new OllamaException(json.get("error").getAsString());
            }
            String reply = json.get("response").getAsString().trim();
            return reply.isEmpty() ? "(the model returned an empty response)" : reply;
        } catch (OllamaException e) {
            throw e;
        } catch (Exception e) {
            throw new OllamaException("Could not parse Ollama response.", e);
        }
    }

    /** Thrown when a request to the Ollama server cannot be completed. */
    public static class OllamaException extends Exception {
        public OllamaException(String message) {
            super(message);
        }

        public OllamaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
