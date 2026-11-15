package com.stefano.aicompanion.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stefano.aicompanion.config.ModConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OllamaClient {
    public String generate(ModConfig config, String userPrompt) throws OllamaException {
        JsonObject options = new JsonObject();
        options.addProperty("temperature", config.temperature);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model);
        body.addProperty("prompt", userPrompt);
        body.addProperty("system", config.systemPrompt);
        body.addProperty("stream", false);
        body.add("options", options);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.ollamaUrl + "/api/generate");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(config.timeoutSeconds * 1000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            OutputStream output = connection.getOutputStream();
            try {
                output.write(payload);
            } finally {
                output.close();
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = readFully(stream);

            if (status == 404) {
                throw new OllamaException("Model '" + config.model
                        + "' not found. Pull it first with 'ollama pull " + config.model + "'.");
            }
            if (status != 200) {
                throw new OllamaException("Ollama returned HTTP " + status + ": " + responseBody);
            }

            JsonObject json = new JsonParser().parse(responseBody).getAsJsonObject();
            if (json.has("error")) throw new OllamaException(json.get("error").getAsString());
            String reply = json.get("response").getAsString().trim();
            return reply.isEmpty() ? "(the model returned an empty response)" : reply;
        } catch (OllamaException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new OllamaException("Could not reach Ollama at " + config.ollamaUrl
                    + ". Is it running? Try 'ollama serve'.", e);
        } catch (Exception e) {
            throw new OllamaException("Request to Ollama failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readFully(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) result.append(line);
        } finally {
            reader.close();
        }
        return result.toString();
    }

    public static class OllamaException extends Exception {
        public OllamaException(String message) { super(message); }
        public OllamaException(String message, Throwable cause) { super(message, cause); }
    }
}