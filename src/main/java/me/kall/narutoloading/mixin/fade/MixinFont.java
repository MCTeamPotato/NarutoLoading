package me.kall.narutoloading.mixin.fade;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutoloading.client.fade.FadeCenter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Font.class, priority = 1500)
public abstract class MixinFont {
    @Unique private static final Component EMPTY_COMPONENT = Component.empty();
    @Unique private static final String EMPTY_STRING = "";

    @WrapMethod(method = "drawInBatch8xOutline")
    private void fade$drawInBatch8xOutline(FormattedCharSequence text, float x, float y, int color, int backgroundColor, Matrix4f matrix, MultiBufferSource bufferSource, int packedLightCoords, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden()) return;
        original.call(text, x, y, FadeCenter.modifyTextAlpha(color), FadeCenter.modifyAlpha(backgroundColor), matrix, bufferSource, packedLightCoords);
    }

    @WrapMethod(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I")
    private int fade$drawInBatch(FormattedCharSequence text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, @NotNull Operation<Integer> original) {
        return original.call(FadeCenter.isHidden() ? FormattedCharSequence.EMPTY : text, x, y, FadeCenter.modifyTextAlpha(color), !FadeCenter.isHidden() && dropShadow, matrix, buffer, displayMode, FadeCenter.modifyAlpha(backgroundColor), packedLightCoords);
    }

    @WrapMethod(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I")
    private int fade$drawInBatch(Component text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, @NotNull Operation<Integer> original) {
        return original.call(FadeCenter.isHidden() ? EMPTY_COMPONENT : text, x, y, FadeCenter.modifyTextAlpha(color), !FadeCenter.isHidden() && dropShadow, matrix, buffer, displayMode, FadeCenter.modifyAlpha(backgroundColor), packedLightCoords);
    }

    @WrapMethod(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I")
    private int fade$drawInBatch(String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, @NotNull Operation<Integer> original) {
        return original.call(FadeCenter.isHidden() ? EMPTY_STRING : text, x, y, FadeCenter.modifyTextAlpha(color), !FadeCenter.isHidden() && dropShadow, matrix, buffer, displayMode, FadeCenter.modifyAlpha(backgroundColor), packedLightCoords);
    }

    @WrapMethod(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I")
    private int fade$drawInBatch(String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, boolean bidirectional, @NotNull Operation<Integer> original) {
        return original.call(FadeCenter.isHidden() ? EMPTY_STRING : text, x, y, FadeCenter.modifyTextAlpha(color), !FadeCenter.isHidden() && dropShadow, matrix, buffer, displayMode, FadeCenter.modifyAlpha(backgroundColor), packedLightCoords, bidirectional);
    }
}
