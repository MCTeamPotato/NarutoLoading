package me.kall.narutoloading.mixin.fade;

import me.kall.narutoloading.client.fade.FadeCenter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = Font.class, priority = 1500)
public abstract class MixinFont {
    @Unique private static final String OUTLINE = "drawInBatch8xOutline";
    @Unique private static final String BATCH_SEQUENCE = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I";
    @Unique private static final String BATCH_COMPONENT = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I";
    @Unique private static final String BATCH_STRING = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I";
    @Unique private static final String BATCH_STRING_BIDI = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I";

    @Inject(method = OUTLINE, at = @At("HEAD"), cancellable = true)
    private void fade$hideOutline(FormattedCharSequence text, float x, float y, int color, int backgroundColor, Matrix4f matrix, MultiBufferSource bufferSource, int packedLightCoords, CallbackInfo ci) {
        if (FadeCenter.isHidden()) ci.cancel();
    }

    @ModifyVariable(method = OUTLINE, at = @At("HEAD"), argsOnly = true, index = 4)
    private int fade$outlineColor(int color) {
        return FadeCenter.modifyTextAlpha(color);
    }

    @ModifyVariable(method = OUTLINE, at = @At("HEAD"), argsOnly = true, index = 5)
    private int fade$outlineBackground(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @Inject(method = BATCH_SEQUENCE, at = @At("HEAD"), cancellable = true)
    private void fade$hideSequence(FormattedCharSequence text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, CallbackInfoReturnable<Integer> cir) {
        if (FadeCenter.isHidden()) cir.setReturnValue(0);
    }

    @ModifyVariable(method = BATCH_SEQUENCE, at = @At("HEAD"), argsOnly = true, index = 4)
    private int fade$sequenceColor(int color) {
        return FadeCenter.modifyTextAlpha(color);
    }

    @ModifyVariable(method = BATCH_SEQUENCE, at = @At("HEAD"), argsOnly = true, index = 9)
    private int fade$sequenceBackground(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @Inject(method = BATCH_COMPONENT, at = @At("HEAD"), cancellable = true)
    private void fade$hideComponent(Component text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, CallbackInfoReturnable<Integer> cir) {
        if (FadeCenter.isHidden()) cir.setReturnValue(0);
    }

    @ModifyVariable(method = BATCH_COMPONENT, at = @At("HEAD"), argsOnly = true, index = 4)
    private int fade$componentColor(int color) {
        return FadeCenter.modifyTextAlpha(color);
    }

    @ModifyVariable(method = BATCH_COMPONENT, at = @At("HEAD"), argsOnly = true, index = 9)
    private int fade$componentBackground(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @Inject(method = BATCH_STRING, at = @At("HEAD"), cancellable = true)
    private void fade$hideString(String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, CallbackInfoReturnable<Integer> cir) {
        if (FadeCenter.isHidden()) cir.setReturnValue(0);
    }

    @ModifyVariable(method = BATCH_STRING, at = @At("HEAD"), argsOnly = true, index = 4)
    private int fade$stringColor(int color) {
        return FadeCenter.modifyTextAlpha(color);
    }

    @ModifyVariable(method = BATCH_STRING, at = @At("HEAD"), argsOnly = true, index = 9)
    private int fade$stringBackground(int color) {
        return FadeCenter.modifyAlpha(color);
    }

    @Inject(method = BATCH_STRING_BIDI, at = @At("HEAD"), cancellable = true)
    private void fade$hideStringBidi(String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource buffer, Font.DisplayMode displayMode, int backgroundColor, int packedLightCoords, boolean bidirectional, CallbackInfoReturnable<Integer> cir) {
        if (FadeCenter.isHidden()) cir.setReturnValue(0);
    }

    @ModifyVariable(method = BATCH_STRING_BIDI, at = @At("HEAD"), argsOnly = true, index = 4)
    private int fade$stringBidiColor(int color) {
        return FadeCenter.modifyTextAlpha(color);
    }

    @ModifyVariable(method = BATCH_STRING_BIDI, at = @At("HEAD"), argsOnly = true, index = 9)
    private int fade$stringBidiBackground(int color) {
        return FadeCenter.modifyAlpha(color);
    }
}
