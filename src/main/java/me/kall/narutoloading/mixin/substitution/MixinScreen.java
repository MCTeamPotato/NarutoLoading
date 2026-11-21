package me.kall.narutoloading.mixin.substitution;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.impl.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @SuppressWarnings("UnstableApiUsage")
    @WrapMethod(method = "renderDirtBackground")
    private void substitute(GuiGraphics guiGraphics, Operation<Void> original) {
        if (Substitution.checkAndRun()) {
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen) (Object) this, guiGraphics));
        } else {
            original.call(guiGraphics);
        }
    }
}
