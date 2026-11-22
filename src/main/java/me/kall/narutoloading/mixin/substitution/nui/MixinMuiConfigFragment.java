package me.kall.narutoloading.mixin.substitution.nui;

import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.widget.FrameLayout;
import me.kall.narutoloading.client.overlay.Substitution;
import me.kall.narutoui.mui.background.MuiBackground;
import me.kall.narutoui.mui.screen.MuiConfigFragment;
import me.kall.narutoui.mui.screen.MuiScreenHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MuiConfigFragment.class, remap = false)
public abstract class MixinMuiConfigFragment {
    @Redirect(method = {"onCreateView", "rebuildForResize"}, at = @At(value = "INVOKE", target = "Lme/kall/narutoui/mui/background/MuiBackground;attach(Lme/kall/narutoui/mui/screen/MuiScreenHost;Licyllis/modernui/widget/FrameLayout;)V"), require = 0)
    private void substitute(MuiBackground background, MuiScreenHost host, FrameLayout root) {
        if (Substitution.check()) {
            root.setBackground(new ColorDrawable(0x00000000));
            return;
        }

        background.attach(host, root);
    }
}
