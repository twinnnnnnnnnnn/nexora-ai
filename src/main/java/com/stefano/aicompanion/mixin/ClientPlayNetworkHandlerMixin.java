package com.stefano.aicompanion.mixin;

import com.stefano.aicompanion.AiCompanionClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void nexoraAi$interceptLocalCommand(String message, CallbackInfo ci) {
        if (AiCompanionClient.handleChatCommand(message)) {
            ci.cancel();
        }
    }
}
