package site.siredvin.yars.testmod.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.siredvin.yars.testmod.client.MinecraftExtensions;

@Mixin(Minecraft.class)
class MinecraftMixin implements MinecraftExtensions {
    @Final @Shadow public LevelRenderer levelRenderer;
    @Shadow public ClientLevel level;
    @Shadow public LocalPlayer player;
    @Unique private final AtomicBoolean yars$isStable = new AtomicBoolean(false);

    @Inject(method = "runTick", at = @At("TAIL"))
    private void yars$updateStable(boolean render, CallbackInfo callback) {
        yars$isStable.set(level != null && player != null
            && levelRenderer.isChunkCompiled(player.blockPosition())
            && levelRenderer.countRenderedChunks() > 10
            && levelRenderer.hasRenderedAllChunks());
    }

    @Override
    public boolean yars$isRenderingStable() {
        return yars$isStable.get();
    }
}
