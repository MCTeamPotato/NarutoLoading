package me.kall.narutoloading.mixin.substitution.nui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.widget.FrameLayout;
import me.kall.narutoloading.impl.Substitution;
import me.kall.narutoui.mui.background.MuiBackground;
import me.kall.narutoui.mui.screen.MuiConfigFragment;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MuiConfigFragment.class, remap = false)
public abstract class MixinMuiConfigFragment {
    @WrapOperation(method = {"onCreateView", "rebuildForResize"}, at = @At(value = "INVOKE", target = "Lme/kall/narutoui/mui/background/MuiBackground;attach(Lme/kall/narutoui/mui/screen/MuiScreenHost;Licyllis/modernui/widget/FrameLayout;)V"), require = 0)
    private void substituteBackground(MuiBackground background, MuiScreenHost host, FrameLayout root, Operation<Void> original) {
        if (Substitution.check()) {
            root.setBackground(new ColorDrawable(0x00000000));
            return;
        }

        original.call(background, host, root);
    }
}
