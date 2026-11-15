# Nexora AI

A client-side Fabric mod for Minecraft 1.16.1 with a local, world-aware AI companion. Ask questions with `/ai <question>` or talk to it with push-to-talk voice input. The assistant can include your current health, hunger, position, dimension, biome and time of day.

Nexora AI connects to [Ollama](https://ollama.com) on your own machine. Voice recognition and spoken replies use local Whisper and Piper programs; no cloud speech API is used.

## Target versions

- Minecraft: **1.16.1**
- Mod loader: **Fabric**
- Java: **8**
- Fabric API: **0.18.0+build.387-1.16.1**
- AI backend: **Ollama**

## Features

- `/ai <question>` — ask the local model a question in chat.
- Hold **V** to record a voice question; release **V** to send it.
- Local speech-to-text with whisper.cpp.
- Optional spoken AI replies with Piper.
- `/ai model <name>` — change the Ollama model.
- `/ai reload` — reload the configuration.
- World-aware prompts with player health, hunger, coordinates, dimension, biome and time.
- Background requests so the game does not wait for the model to answer.

## Setup

1. Install Minecraft 1.16.1 with Fabric Loader and the matching Fabric API.
2. Install [Ollama](https://ollama.com), start it, and download a model:

   ```sh
   ollama pull llama3.2
   ```

3. Install [whisper.cpp](https://github.com/ggml-org/whisper.cpp) for Windows. Put `whisper-cli.exe` in the game's `voice` folder and download a Whisper model such as `ggml-base.bin` into the same folder.
4. Install [Piper](https://github.com/rhasspy/piper) for Windows. Put `piper.exe` in the game's `voice` folder and put a Piper voice model (the `.onnx` file and its matching `.onnx.json`) there too.
5. Launch Minecraft once with Nexora AI installed. Open `config/nexora-ai.json` in the game directory.
6. Set `voiceEnabled` to `true`. The default paths expect these files:

   - `voice/whisper-cli.exe`
   - `voice/ggml-base.bin`
   - `voice/piper.exe`
   - `voice/en_US-lessac-medium.onnx`

   Change the paths in the config if your files are somewhere else. Set `speakReplies` to `false` if you only want microphone input.
7. Restart Minecraft or run `/ai reload`. Hold **V** while speaking, then release it to send your question.

Whisper's language setting is `whisperLanguage` in the config (for example `auto`, `en`, or `ja`). The microphone is only accessed while the push-to-talk key is held and voice is enabled.

## Configuration

Nexora AI creates `config/nexora-ai.json` on first launch. It includes:

- `ollamaUrl`, `model`, `temperature`, `timeoutSeconds`, `systemPrompt` — Ollama settings.
- `includePlayerContext` — attach live player/world information to each prompt.
- `voiceEnabled` — enable push-to-talk microphone input (off by default).
- `speakReplies` — read AI replies aloud.
- `whisperExecutable`, `whisperModel`, `whisperLanguage` — local speech recognition settings.
- `piperExecutable`, `piperModel` — local text-to-speech settings.

Relative executable and model paths are resolved from the Minecraft game directory. Edit the file and run `/ai reload` to apply changes.

## Build

Use JDK 21 to run the Gradle wrapper (the mod itself targets Java 8 bytecode):

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The remapped mod JAR is generated in `build/libs/`.

## Credits and license

Based on the MIT-licensed [AI Companion](https://github.com/apolloclaws-oss/minecraft-ai-companion) project by Stefano, backported for Minecraft 1.16.1 and maintained as Nexora AI.
