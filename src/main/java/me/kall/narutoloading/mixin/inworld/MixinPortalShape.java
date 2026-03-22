package me.kall.narutoloading.mixin.inworld;

import me.kall.narutoloading.inworld.core.ServerScreenChecker;
import me.kall.narutoloading.inworld.data.Displayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(PortalShape.class)
public abstract class MixinPortalShape {
    @Shadow @Final private LevelAccessor level;
    @Shadow @Nullable private BlockPos bottomLeft;
    @Shadow @Final private Direction rightDir;
    @Shadow @Final private int width;

    @Inject(method = "createPortalBlocks", at = @At(value = "INVOKE", target = "Ljava/lang/Iterable;forEach(Ljava/util/function/Consumer;)V", remap = false, shift = At.Shift.AFTER))
    private void afterGenPortal(CallbackInfo ci) {
        assert this.bottomLeft != null;
        if (this.level instanceof ServerLevel serverLevel) {
            BlockPos bottomRight = this.bottomLeft.relative(this.rightDir, this.width - 1);
            serverLevel.getServer().execute(() -> ServerScreenChecker.tryBuildScreen(null, serverLevel, this.bottomLeft, bottomRight, pos -> Displayers.isDisplayer(serverLevel, pos)));
        }
    }
}
