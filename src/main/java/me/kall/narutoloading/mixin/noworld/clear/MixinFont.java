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
            "drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I",
            "drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZIIZ)I",
            "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I",
            "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I",
            "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyColor(int value) {
        return Fader.modifyAlpha(value);
    }

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String clearDrawInBatch1(String value) {
        return Fader.transparency() ? NarutoLoading.BLANK : value;
    }

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZIIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String clearDrawInBatch2(String value) {
        return Fader.transparency() ? NarutoLoading.BLANK : value;
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component clearDrawInBatch3(Component value) {
        return Fader.transparency() ? Fader.EMPTY_COMPONENT : value;
    }

    @ModifyVariable(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence clearDrawInBatch4(FormattedCharSequence value) {
        return Fader.transparency() ? FormattedCharSequence.EMPTY : value;
    }

    @ModifyVariable(method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence clearDrawInternal(FormattedCharSequence value) {
        return Fader.transparency() ? FormattedCharSequence.EMPTY : value;
    }
}
