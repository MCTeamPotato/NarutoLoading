package me.kall.narutoloading.mixin.inworld;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public abstract class MixinResourceLoadStateTracker {
    @Inject(method = "finishReload", at = @At("TAIL"))
    private void onFinishReload(CallbackInfo ci) {
        Minecraft.getInstance().execute(() -> {
            for (ObjectSet<NarutoInWorldRenderer> renderers : ClientScreensRenderer.CLIENT_SCREENS.values()) {
                for (NarutoInWorldRenderer renderer : renderers) {
                    NarutoLoading.LOGGER.info("{}Restarting {} after resource reload.", NarutoLoading.info(), renderer.screen.toString());
                    renderer.shutdown();
                    renderer.setup();
                }
            }
        });
    }
}
