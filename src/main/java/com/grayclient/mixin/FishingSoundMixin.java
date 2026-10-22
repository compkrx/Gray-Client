package com.grayclient.mixin;

import com.grayclient.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class FishingSoundMixin {
    private static final Identifier GRAY_SPLASH = Identifier.of("minecraft", "entity.fishing_bobber.splash");

    // RETURN runs after vanilla's main-thread dispatch, never on the networking thread.
    @Inject(method = "onPlaySound", at = @At("RETURN"))
    private void gray$fishingSplash(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (packet.getSound().value().id().equals(GRAY_SPLASH))
            Modules.AUTO_FISH.onSplash(packet.getX(), packet.getY(), packet.getZ());
    }
}
