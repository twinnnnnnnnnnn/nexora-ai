package com.stefano.aicompanion.ai;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.math.BlockPos;

public final class PlayerContext {
    private PlayerContext() {}

    public static String describe(ClientPlayerEntity player) {
        if (player == null || player.world == null) return "";

        ClientWorld world = (ClientWorld) player.world;
        BlockPos pos = player.getBlockPos();
        String dimension = shortName(world.getRegistryKey().getValue().toString());
        String biome = shortName(Registry.BIOME.getId(world.getBiome(pos)).toString());
        String daytime = describeDaytime(world.getTimeOfDay() % 24000L);

        return String.format(
                "Player state -> name: %s, health: %.0f/%.0f, hunger: %d/20, "
                        + "position: (%d, %d, %d), dimension: %s, biome: %s, time: %s.",
                player.getGameProfile().getName(), player.getHealth(), player.getMaxHealth(),
                player.getHungerManager().getFoodLevel(), pos.getX(), pos.getY(), pos.getZ(),
                dimension, biome, daytime);
    }

    private static String describeDaytime(long time) {
        if (time < 1000 || time >= 23000) return "sunrise";
        if (time < 12000) return "day";
        if (time < 13000) return "sunset";
        return "night (hostile mobs spawn)";
    }

    private static String shortName(String identifier) {
        int colon = identifier.indexOf(':');
        return colon >= 0 ? identifier.substring(colon + 1) : identifier;
    }
}