package me.kall.narutoloading.mixin.substitution;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.narutoloading.client.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void substitute(AbstractSelectionList<?> instance, GuiGraphics guiGraphics, Operation<Void> original) {
        if (Substitution.checkAndRun()) return;
        original.call(instance, guiGraphics);
    }

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderTopAndBottom:Z", opcode = Opcodes.GETFIELD))
    private boolean skipTopAndBottom(AbstractSelectionList<?> instance, Operation<Boolean> original) {
        return !Substitution.check() && original.call(instance);
    }

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z", opcode = Opcodes.GETFIELD))
    private boolean skipBackground(AbstractSelectionList<?> instance, Operation<Boolean> original) {
        return !Substitution.check() && original.call(instance);
    }
}
