package me.kall.narutoloading.mixin.noworld.impl.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.noworld.core.NarutoRenderer;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen extends AbstractContainerEventHandler {
    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void dirtScreenByeBye(CallbackInfo ci) {
        if (BaseEnv.available()) {
            Screen screen = (Screen) (Object) this;
            PoseStack poseStack = new PoseStack();
            NarutoRenderer.INSTANCE.renderFrame(poseStack);
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.BackgroundDrawnEvent(screen, poseStack));
            ci.cancel();
        }
    }
}
