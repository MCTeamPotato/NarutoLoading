package me.kall.narutoloading.mixin.noworld.impl.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import me.kall.duplicationless.mixinextras.expression.Definition;
import me.kall.duplicationless.mixinextras.expression.Expression;
import me.kall.duplicationless.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.ScrollPanel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ScrollPanel.class)
public abstract class MixinScrollPanel {
    @Shadow(remap = false) @Final private Minecraft client;

    @Shadow protected abstract void drawGradientRect(PoseStack mStack, int left, int top, int right, int bottom, int color1, int color2);

    @SuppressWarnings("MixinAnnotationTarget")
    @Definition(id = "client", field = "Lnet/minecraftforge/client/gui/ScrollPanel;client:Lnet/minecraft/client/Minecraft;")
    @Definition(id = "level", field = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;")
    @Expression("this.client.level != null")
    @ModifyExpressionValue(method = "render", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean checkLevel(boolean original) {
        return true;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/ScrollPanel;drawGradientRect(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
    private void onRender(ScrollPanel instance, PoseStack mStack, int left, int top, int right, int bottom, int color1, int color2) {
        if (this.client.level != null) this.drawGradientRect(mStack, left, top, right, bottom, color1, color2);
    }
}
