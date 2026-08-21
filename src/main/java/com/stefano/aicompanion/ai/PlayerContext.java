package com.stefano.aicompanion.ai;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * Builds a compact, human-readable snapshot of the player's current situation
 * so the language model can give context-aware answers ("it's night, mobs are
 * about to spawn", "you're low on health", ...).
 */
public final class PlayerContext {

    private PlayerContext() {
    }

    /**
     * @return a single-line description of the player's state, or an empty
     *         string if no player/world is available.
     */
    public static String describe(ClientPlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return "";
        }

        ClientWorld world = (ClientWorld) player.getWorld();
        BlockPos pos = player.getBlockPos();

        String dimension = shortName(world.getRegistryKey().getValue().toString());
        String biome = biomeName(world, pos);
        String daytime = describeDaytime(world.getTimeOfDay() % 24000L);

        return String.format(
                "Player state -> name: %s, health: %.0f/%.0f, hunger: %d/20, "
                        + "position: (%d, %d, %d), dimension: %s, biome: %s, time: %s.",
                player.getGameProfile().getName(),
                player.getHealth(), player.getMaxHealth(),
                player.getHungerManager().getFoodLevel(),
                pos.getX(), pos.getY(), pos.getZ(),
                dimension, biome, daytime);
    }

    private static String biomeName(ClientWorld world, BlockPos pos) {
        RegistryEntry<Biome> entry = world.getBiome(pos);
        return entry.getKey()
                .map(key -> shortName(key.getValue().toString()))
                .orElse("unknown");
    }

    private static String describeDaytime(long timeOfDay) {
        if (timeOfDay < 1000 || timeOfDay >= 23000) {
            return "sunrise";
        } else if (timeOfDay < 12000) {
            return "day";
        } else if (timeOfDay < 13000) {
            return "sunset";
        } else {
            return "night (hostile mobs spawn)";
        }
    }

    /** Strips the namespace from an identifier, e.g. {@code minecraft:plains -> plains}. */
    private static String shortName(String identifier) {
        int colon = identifier.indexOf(':');
        return colon >= 0 ? identifier.substring(colon + 1) : identifier;
    }
}
