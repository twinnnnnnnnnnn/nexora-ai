# Nexora AI

A client-side Fabric mod that adds a local AI companion to Minecraft 1.16.1. Ask questions with `/ai <question>`; the assistant can include your current health, hunger, position, dimension, biome and time of day.

Nexora AI connects to [Ollama](https://ollama.com) on your own machine. It does not require a cloud API key.

## Target versions

- Minecraft: **1.16.1**
- Mod loader: **Fabric**
- Java: **8**
- Fabric API: **0.18.0+build.387-1.16.1**
- AI backend: **Ollama**

## Features

- `/ai <question>` — ask the local model a question in chat.
- `/ai model <name>` — change the Ollama model.
- `/ai reload` — reload the configuration.
- World-aware prompts with player health, hunger, coordinates, dimension, biome and time.
- Background requests so the game does not wait for the model to answer.
- Configurable Ollama URL, model, temperature, timeout and system prompt.

## Setup

1. Install Minecraft 1.16.1 with Fabric Loader.
2. Install the matching Fabric API.
3. Install [Ollama](https://ollama.com) and start it.
4. Download a model, for example:

   ```sh
   ollama pull llama3.2
   ```

5. Put the Nexora AI JAR in the Minecraft `mods` folder and launch the Fabric profile.
6. In a world, run `/ai hello`.

Ollama normally listens at `http://localhost:11434`.

## Configuration

On first launch Nexora AI creates `config/nexora-ai.json`. You can change:

- `ollamaUrl` — Ollama server URL.
- `model` — model tag installed in Ollama.
- `includePlayerContext` — attach live player/world information to each prompt.
- `temperature` — response creativity.
- `timeoutSeconds` — maximum wait for a response.
- `systemPrompt` — assistant instructions and personality.

Edit the file and run `/ai reload` to apply changes.

## Build

Use JDK 8 and run:

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
