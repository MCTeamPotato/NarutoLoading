package me.kall.narutoloading.core.detection.inworld;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Screen {
    private final BlockPos leftBottomCorner;
    private final BlockPos leftTopCorner;
    private final BlockPos rightBottomCorner;
    private final BlockPos rightTopCorner;
    private final ResourceLocation dimension;

    private final LongSet involved;

    private final long[] array;
    private final int hash;

    public Screen(BlockPos leftBottomCorner, BlockPos leftTopCorner, BlockPos rightBottomCorner, BlockPos rightTopCorner, ResourceLocation dimension) {
        this.leftBottomCorner = leftBottomCorner;
        this.leftTopCorner = leftTopCorner;
        this.rightBottomCorner = rightBottomCorner;
        this.rightTopCorner = rightTopCorner;
        this.dimension = dimension;

        this.involved = getInvolved(leftBottomCorner, leftTopCorner, rightBottomCorner, rightTopCorner);

        this.array = new long[]{this.leftBottomCorner.asLong(), this.leftTopCorner.asLong(), this.rightBottomCorner.asLong(), this.rightTopCorner.asLong()};
        this.hash = Objects.hash(this.leftBottomCorner, this.leftTopCorner, this.rightBottomCorner, this.rightTopCorner, this.dimension);
    }

    private static @NotNull LongList getLine(@NotNull BlockPos firstCorner, @NotNull BlockPos secondCorner) {
        LongList involved = new LongArrayList();

        int firstCornerX = firstCorner.getX();
        int firstCornerY = firstCorner.getY();
        int firstCornerZ = firstCorner.getZ();

        int secondCornerX = secondCorner.getX();
        int secondCornerY = secondCorner.getY();
        int secondCornerZ = secondCorner.getZ();

        int directionX = Integer.compare(secondCornerX, firstCornerX);
        int directionY = Integer.compare(secondCornerY, firstCornerY);
        int directionZ = Integer.compare(secondCornerZ, firstCornerZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(firstCornerX, firstCornerY, firstCornerZ);

        involved.add(pos.asLong());

        while (!pos.equals(secondCorner)) {
            involved.add(pos.move(directionX, directionY, directionZ).asLong());
        }

        return involved;
    }

    private static @NotNull LongSet getInvolved(BlockPos leftBottom, BlockPos leftTop, BlockPos rightBottom, BlockPos rightTop) {
        LongSet border = new LongOpenHashSet();

        border.addAll(getLine(leftBottom, leftTop));
        border.addAll(getLine(rightBottom, rightTop));
        border.addAll(getLine(leftBottom, rightBottom));
        border.addAll(getLine(leftTop, rightTop));

        return border;
    }

    public BlockPos leftBottomCorner() {
        return this.leftBottomCorner;
    }

    public BlockPos leftTopCorner() {
        return this.leftTopCorner;
    }

    public BlockPos rightBottomCorner() {
        return this.rightBottomCorner;
    }

    public BlockPos rightTopCorner() {
        return this.rightTopCorner;
    }

    public ResourceLocation dimension() {
        return this.dimension;
    }

    public LongSet involved() {
        return this.involved;
    }

    @Contract(" -> new")
    public long @NotNull [] toLongArray() {
        return this.array;
    }

    @Contract("_, _ -> new")
    public static @NotNull Screen from(long @NotNull [] corners, ResourceLocation dimension) {
        return new Screen(BlockPos.of(corners[0]), BlockPos.of(corners[1]), BlockPos.of(corners[2]), BlockPos.of(corners[3]), dimension);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Screen screen) {
            return screen.leftBottomCorner.equals(this.leftBottomCorner) && screen.leftTopCorner.equals(this.leftTopCorner) && screen.rightTopCorner.equals(this.rightTopCorner) && screen.rightBottomCorner.equals(this.rightBottomCorner) && screen.dimension.equals(this.dimension);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    public @NotNull String toLocalString() {
        return Component.translatable("screen.narutoloading.arg", this.leftBottomCorner.toShortString(), this.leftTopCorner.toShortString(), this.rightBottomCorner.toShortString(), this.rightTopCorner.toShortString(), this.dimension.toString()).getString();
    }

    @Override
    public @NotNull String toString() {
        return "Screen: {LeftBottom: [" + this.leftBottomCorner.toShortString() + "], LeftTop: [" + this.leftTopCorner.toShortString() + "], RightBottom: [" + this.rightBottomCorner.toShortString() + "], RightTop: [" + this.rightTopCorner.toShortString() + "], Dimension: [" + this.dimension.toString() + "]}";
    }
}
