package me.kall.narutoloading.mixin.sound;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.InWorldScreen;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @Inject(method = "reload", at = @At("HEAD"))
    private void shutdown(CallbackInfo ci) {
        if (BaseEnv.available() && NarutoRenderer.INSTANCE.audioExecutor != null) {
            NarutoRenderer.INSTANCE.audioExecutor.shutdown();
            NarutoLoading.LOGGER.info("{}Minecraft SoundEngine starts to load. Shutting down NarutoAudioExecutor for the OpenAL context synchronization.", NarutoLoading.info());
        }
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void setup(CallbackInfo ci) {
        if (BaseEnv.available()) {
            if (NarutoRenderer.INSTANCE.lifetime != null) {
                NarutoRenderer.INSTANCE.lifetime.syncSoundEngine = true;
            }

            Minecraft.getInstance().execute(() -> {
                for (ObjectSet<NarutoInWorldRenderer> renderers : ClientScreensRenderer.CLIENT_SCREENS.values()) {
                    for (NarutoInWorldRenderer renderer : renderers) {
                        if (renderer.screen.isLocalSound() && renderer.isRunning()) {
                            naruto$replayLocalSound(renderer);
                        }
                    }
                }
            });
        }
    }

    @Unique
    private static void naruto$replayLocalSound(NarutoInWorldRenderer renderer) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        InWorldScreen screen = renderer.screen;
        if (screen.localSound() == InWorldScreen.NO_LOCAL_SOUND) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Holder<SoundEvent> soundEvent = Holder.direct(SoundEvent.createVariableRangeEvent(screen.localSound()));
            level.playSeededSound(player, screen.centerX(), screen.centerY(), screen.centerZ(), soundEvent, SoundSource.BLOCKS, 4.0F, 1.0F, level.random.nextLong());
        }
        NarutoLoading.LOGGER.info("{}Replayed local sound {} at [{}, {}, {}] after sound engine reload", NarutoLoading.info(), screen.localSound().toString(), screen.centerX(), screen.centerY(), screen.centerZ());
    }
}