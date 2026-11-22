package me.kall.narutoloading.mixin.substitution;

import me.kall.narutoloading.client.overlay.Substitution;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {
    @Shadow private boolean renderBackground;
    @Shadow private boolean renderTopAndBottom;

    @Shadow protected abstract void renderBackground(GuiGraphics guiGraphics);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void substitute(AbstractSelectionList<?> instance, GuiGraphics guiGraphics) {
        if (Substitution.checkAndRun()) return;
        this.renderBackground(guiGraphics);
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderTopAndBottom:Z", opcode = Opcodes.GETFIELD))
    private boolean skipTopAndBottom(AbstractSelectionList<?> instance) {
        return !Substitution.check() && this.renderTopAndBottom;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderBackground:Z", opcode = Opcodes.GETFIELD))
    private boolean skipBackground(AbstractSelectionList<?> instance) {
        return !Substitution.check() && this.renderBackground;
    }
}
