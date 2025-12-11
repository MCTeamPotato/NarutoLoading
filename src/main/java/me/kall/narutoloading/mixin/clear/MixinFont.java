package me.kall.narutoloading.mixin.clear;

import me.kall.narutoloading.core.MouseChecker;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Font.class, priority = 500)
public abstract class MixinFont {

    @ModifyVariable(method = {
            "drawInBatch8xOutline",
            "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyColor(int value) {
        return MouseChecker.modifyAlpha(value);
    }

    @ModifyVariable(method = "drawInBatch8xOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence clearDrawInBatch8xOutline(FormattedCharSequence formattedCharSequence) {
        return MouseChecker.transparency() ? FormattedCharSequence.EMPTY : formattedCharSequence;
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence clearDrawInBatch(FormattedCharSequence formattedCharSequence) {
        return MouseChecker.transparency() ? FormattedCharSequence.EMPTY : formattedCharSequence;
    }

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String clearDrawInBatch(String string) {
        return MouseChecker.transparency() ? MouseChecker.EMPTY_STRING : string;
    }

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String clearDrawInBatch2(String string) {
        return MouseChecker.transparency() ? MouseChecker.EMPTY_STRING : string;
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component clearDrawInBatch(Component component) {
        return MouseChecker.transparency() ? MouseChecker.EMPTY : component;
    }
}
