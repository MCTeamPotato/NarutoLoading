package me.kall.narutoloading.mixin.structure;

import me.kall.narutoloading.server.WallTemplates;
import me.kall.narutoloading.world.Wall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(StructureTemplate.class)
public abstract class MixinStructureTemplate {
    @Unique private final List<CompoundTag> narutoLoading$walls = new ArrayList<>();

    @Inject(method = "fillFromWorld", at = @At("RETURN"))
    private void collectWalls(Level level, BlockPos pos, Vec3i size, boolean withEntities, Block toIgnore, @NotNull CallbackInfo ci) {
        this.narutoLoading$walls.clear();

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) return;

        BlockPos end = pos.offset(size).offset(-1, -1, -1);
        BlockPos min = new BlockPos(Math.min(pos.getX(), end.getX()), Math.min(pos.getY(), end.getY()), Math.min(pos.getZ(), end.getZ()));
        BlockPos max = new BlockPos(Math.max(pos.getX(), end.getX()), Math.max(pos.getY(), end.getY()), Math.max(pos.getZ(), end.getZ()));

        this.narutoLoading$walls.addAll(WallTemplates.collect(serverLevel, min, max));
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void saveWalls(CompoundTag tag, @NotNull CallbackInfoReturnable<CompoundTag> cir) {
        if (this.narutoLoading$walls.isEmpty()) return;

        ListTag list = new ListTag();
        list.addAll(this.narutoLoading$walls);
        tag.put(WallTemplates.WALLS_KEY, list);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void loadWalls(HolderGetter<Block> blocks, CompoundTag tag, @NotNull CallbackInfo ci) {
        this.narutoLoading$walls.clear();

        ListTag list = tag.getList(WallTemplates.WALLS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) this.narutoLoading$walls.add(list.getCompound(index).copy());
    }

    @Inject(method = "placeInWorld", at = @At("RETURN"))
    private void placeWalls(ServerLevelAccessor serverLevel, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, RandomSource random, int flags, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || this.narutoLoading$walls.isEmpty()) return;
        if (!(serverLevel instanceof ServerLevel level)) return;

        List<CompoundTag> restored = new ArrayList<>(this.narutoLoading$walls.size());

        for (CompoundTag template : this.narutoLoading$walls) {
            long[] relative = template.getLongArray(Wall.CORNERS_KEY);
            if (relative.length != 4) continue;

            long[] absolute = new long[4];
            for (int index = 0; index < 4; index++) {
                absolute[index] = StructureTemplate.calculateRelativePosition(settings, BlockPos.of(relative[index])).offset(offset).asLong();
            }

            CompoundTag tag = template.copy();
            tag.putLongArray(Wall.CORNERS_KEY, absolute);
            restored.add(tag);
        }

        if (restored.isEmpty()) return;

        level.getServer().execute(() -> WallTemplates.apply(level, restored));
    }
}
