package me.kall.narutoloading.mixin.inworld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.duplicationless.util.Executor;
import me.kall.narutoloading.inworld.core.ServerScreenChecker;
import me.kall.narutoloading.inworld.data.Displayers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import twilightforest.block.TFPortalBlock;

import java.util.Map;
import java.util.Set;

@Mixin(TFPortalBlock.class)
public abstract class MixinTFPortalBlock {
    @WrapOperation(method = "tryToCreatePortal", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", remap = false), remap = false)
    private Set<Map.Entry<BlockPos, Boolean>> creatingPortal(@NotNull Map<BlockPos, Boolean> blocks, @NotNull Operation<Set<Map.Entry<BlockPos, Boolean>>> original, @Local Level level) {
        Set<BlockPos> positions = new ObjectOpenHashSet<>();
        for (Map.Entry<BlockPos, Boolean> entry : blocks.entrySet()) {
            if (entry.getValue()) {
                positions.add(entry.getKey());
            }
        }

        BlockPos[] corners = naruto$getCorners(positions);
        if (level instanceof ServerLevel serverLevel) {
            Executor.runAfter(2, () -> ServerScreenChecker.tryBuildScreen(null, serverLevel, corners[0], corners[1], posLong -> Displayers.isDisplayer(serverLevel, posLong)));
        }

        return original.call(blocks);
    }

    @Unique
    @Contract("_ -> new")
    private static BlockPos @NotNull [] naruto$getCorners(@NotNull Set<BlockPos> positions) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        int y = 0;

        for (BlockPos pos : positions) {
            int x = pos.getX();
            int z = pos.getZ();

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);

            y = pos.getY();
        }

        int dx = maxX - minX;
        int dz = maxZ - minZ;

        if (dx >= dz) {
            return new BlockPos[]{new BlockPos(minX, y, minZ), new BlockPos(maxX, y, minZ)};
        } else {
            return new BlockPos[]{new BlockPos(minX, y, minZ), new BlockPos(minX, y, maxZ)};
        }
    }

}
