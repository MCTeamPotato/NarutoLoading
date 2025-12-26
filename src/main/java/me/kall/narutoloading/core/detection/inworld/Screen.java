package me.kall.narutoloading.core.detection.inworld;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Screen {
    public final BlockPos leftBottomCorner;
    public final BlockPos leftTopCorner;
    public final BlockPos rightBottomCorner;
    public final BlockPos rightTopCorner;
    public final ResourceLocation dimension;

    private final long[] array;
    private final int hash;

    public Screen(BlockPos leftBottomCorner, BlockPos leftTopCorner, BlockPos rightBottomCorner, BlockPos rightTopCorner, ResourceLocation dimension) {
        this.leftBottomCorner = leftBottomCorner;
        this.leftTopCorner = leftTopCorner;
        this.rightBottomCorner = rightBottomCorner;
        this.rightTopCorner = rightTopCorner;
        this.dimension = dimension;

        this.array = new long[]{this.leftBottomCorner.asLong(), this.leftTopCorner.asLong(), this.rightBottomCorner.asLong(), this.rightTopCorner.asLong()};
        this.hash = Objects.hash(this.leftBottomCorner, this.leftTopCorner, this.rightBottomCorner, this.rightTopCorner, this.dimension);
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
