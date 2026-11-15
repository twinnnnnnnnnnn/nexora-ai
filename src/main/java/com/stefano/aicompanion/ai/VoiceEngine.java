package com.stefano.aicompanion.ai;

import com.stefano.aicompanion.config.ModConfig;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Offline speech recognition (whisper.cpp) and speech synthesis (Piper). */
public final class VoiceEngine {
    private VoiceEngine() { }

    public static String transcribe(ModConfig config, Path audioFile, Path gameDirectory) throws Exception {
        File executable = resolve(gameDirectory, config.whisperExecutable);
        File model = resolve(gameDirectory, config.whisperModel);
        requireFile(executable, "Whisper executable");
        requireFile(model, "Whisper model");

        List<String> command = new ArrayList<String>();
        command.add(executable.getAbsolutePath());
        command.add("-m");
        command.add(model.getAbsolutePath());
        command.add("-f");
        command.add(audioFile.toAbsolutePath().toString());
        command.add("-nt");
        command.add("-np");
        command.add("-l");
        command.add(config.whisperLanguage == null || config.whisperLanguage.trim().isEmpty()
                ? "auto" : config.whisperLanguage.trim());

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = readProcessOutput(process.getInputStream());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Whisper exited with code " + exit + ": " + output.trim());
        }
        return output.trim();
    }

    public static void speak(ModConfig config, String text, Path gameDirectory) throws Exception {
        if (text == null || text.trim().isEmpty()) return;
        File executable = resolve(gameDirectory, config.piperExecutable);
        File model = resolve(gameDirectory, config.piperModel);
        requireFile(executable, "Piper executable");
        requireFile(model, "Piper voice model");

        Path output = Files.createTempFile("nexora-reply-", ".wav");
        try {
            List<String> command = new ArrayList<String>();
            command.add(executable.getAbsolutePath());
            command.add("--model");
            command.add(model.getAbsolutePath());
            command.add("--output_file");
            command.add(output.toAbsolutePath().toString());

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            OutputStream stdin = process.getOutputStream();
            stdin.write(text.getBytes(StandardCharsets.UTF_8));
            stdin.write('\n');
            stdin.close();
            String processOutput = readProcessOutput(process.getInputStream());
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("Piper exited with code " + exit + ": " + processOutput.trim());
            }

            AudioInputStream audio = AudioSystem.getAudioInputStream(output.toFile());
            Clip clip = AudioSystem.getClip();
            try {
                clip.open(audio);
                clip.start();
                while (clip.isRunning()) {
                    Thread.sleep(50L);
                }
            } finally {
                clip.close();
                audio.close();
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static File resolve(Path gameDirectory, String configuredPath) {
        Path path = Paths.get(configuredPath == null ? "" : configuredPath);
        if (!path.isAbsolute()) path = gameDirectory.resolve(path);
        return path.normalize().toFile();
    }

    private static void requireFile(File file, String description) {
        if (!file.isFile()) {
            throw new IllegalStateException(description + " not found: " + file.getAbsolutePath());
        }
    }

    private static String readProcessOutput(InputStream input) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (output.length() > 0) output.append('\n');
            output.append(line);
        }
        reader.close();
        return output.toString();
    }
}
