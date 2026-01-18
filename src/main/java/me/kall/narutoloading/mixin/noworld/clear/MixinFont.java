package me.kall.narutoloading.mixin.noworld.clear;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.noworld.fade.Fader;
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
            "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyColor(int color) {
        return Fader.modifyAlpha(color);
    }

    @ModifyVariable(method = {
            "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int modifyBgColor(int color) {
        return Fader.modifyAlpha(color);
    }

    @ModifyVariable(method = "drawInBatch8xOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence clearDrawInBatch8xOutline(FormattedCharSequence formattedCharSequence) {
        return Fader.transparency() ? FormattedCharSequence.EMPTY : formattedCharSequence;
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence clearDrawInBatch(FormattedCharSequence formattedCharSequence) {
        return Fader.transparency() ? FormattedCharSequence.EMPTY : formattedCharSequence;
    }

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String clearDrawInBatch(String string) {
        return Fader.transparency() ? NarutoLoading.BLANK : string;
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component clearDrawInBatch(Component component) {
        return Fader.transparency() ? Fader.EMPTY_COMPONENT : component;
    }
}
