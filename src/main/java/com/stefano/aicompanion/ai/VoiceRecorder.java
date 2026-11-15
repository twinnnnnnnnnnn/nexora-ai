package com.stefano.aicompanion.ai;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Records push-to-talk microphone audio as a 16 kHz mono WAV for Whisper. */
public class VoiceRecorder {
    private volatile boolean recording;
    private volatile TargetDataLine line;

    public synchronized void start(Path output, Runnable onStarted, Consumer<Throwable> onFailure) {
        if (recording) return;
        recording = true;
        Thread thread = new Thread(() -> {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            TargetDataLine openedLine = null;
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                openedLine = (TargetDataLine) AudioSystem.getLine(info);
                openedLine.open(format);
                synchronized (VoiceRecorder.this) {
                    if (!recording) {
                        openedLine.close();
                        return;
                    }
                    line = openedLine;
                }
                openedLine.start();
                onStarted.run();
                AudioInputStream audio = new AudioInputStream(openedLine);
                AudioSystem.write(audio, AudioFileFormat.Type.WAVE, output.toFile());
            } catch (Throwable error) {
                if (recording) onFailure.accept(error);
            } finally {
                if (openedLine != null) openedLine.close();
                line = null;
                recording = false;
            }
        }, "nexora-microphone");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        recording = false;
        TargetDataLine activeLine = line;
        if (activeLine != null) {
            activeLine.stop();
            activeLine.close();
        }
    }
}
