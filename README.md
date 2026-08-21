# AI Companion

A [Fabric](https://fabricmc.net/) mod that puts a **local AI assistant inside Minecraft**. Type `/ai <question>` in chat and a language model running on your own machine answers back, aware of what is happening in your world.

No cloud, no API keys, no telemetry. Everything runs locally through [Ollama](https://ollama.com).

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![Loader](https://img.shields.io/badge/Fabric-0.16.5+-DBD0B4)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Features

- **`/ai <question>`** &mdash; ask anything and get an answer in chat.
- **World-aware answers** &mdash; the model is told your health, hunger, position, dimension, biome and the time of day, so it can say things like *"it's night, light up your base before mobs spawn."*
- **Fully local & private** &mdash; talks to Ollama on `localhost`; nothing leaves your machine.
- **Non-blocking** &mdash; requests run on a background thread, so the game never freezes while the model thinks.
- **Configurable** &mdash; swap models, tweak the system prompt and temperature from a simple JSON file, or hot-swap the model in-game with `/ai model <name>`.
- **Friendly errors** &mdash; clear in-chat messages when Ollama is offline or a model isn't pulled yet.

## Demo

```
> /ai what should I do before it gets dark?
[AI] You asked: what should I do before it gets dark?
[AI] thinking...
[AI] You're in a plains biome at night already. Place torches around
     your base, block up any open doorways, and keep a sword handy.
```

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.5+ |
| Fabric API | required |
| Java | 21+ |
| [Ollama](https://ollama.com) | running locally |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.1.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and drop it in your `mods` folder.
3. Download the AI Companion `.jar` from the [releases page](https://github.com/apolloclaws-oss/minecraft-ai-companion/releases) (or build it yourself, see below) and put it in `mods` too.
4. Install and start Ollama, then pull a model:
   ```bash
   ollama pull llama3.2
   ```
5. Launch Minecraft with the Fabric profile and type `/ai hello` in any world.

## Usage

| Command | Description |
|---|---|
| `/ai <question>` | Ask the AI a question. |
| `/ai model <name>` | Switch the active model (e.g. `mistral`, `qwen2.5`). |
| `/ai reload` | Reload the config file from disk. |

## Configuration

On first launch the mod writes `config/aicompanion.json`:

```json
{
  "ollamaUrl": "http://localhost:11434",
  "model": "llama3.2",
  "includePlayerContext": true,
  "temperature": 0.7,
  "timeoutSeconds": 90,
  "systemPrompt": "You are a helpful in-game companion ..."
}
```

- **ollamaUrl** &mdash; where your Ollama server lives.
- **model** &mdash; any model you've pulled with `ollama pull`.
- **includePlayerContext** &mdash; set to `false` for pure Q&A without world state.
- **systemPrompt** &mdash; rewrite this to change the assistant's personality.

Edit the file and run `/ai reload` to apply changes without restarting.

## Building from source

```bash
git clone https://github.com/apolloclaws-oss/minecraft-ai-companion.git
cd minecraft-ai-companion
./gradlew build
```

The compiled mod lands in `build/libs/`. Use `./gradlew runClient` to launch a dev instance.

## How it works

```
/ai command  ->  background worker thread  ->  HTTP POST /api/generate  ->  Ollama
     ^                                                                        |
     |------------------------  reply printed to chat  <----------------------|
```

The command is registered client-side through Fabric's `ClientCommandManager`. Player context is collected on the main thread, the network call happens on a dedicated worker so the render loop never blocks, and the reply is posted back to chat via `client.execute(...)`.

## License

Released under the [MIT License](LICENSE).
