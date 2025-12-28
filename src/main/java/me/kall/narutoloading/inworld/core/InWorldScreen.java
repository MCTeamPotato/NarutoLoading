package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class InWorldScreen {
    private final BlockPos leftBottomCorner;
    private final BlockPos leftTopCorner;
    private final BlockPos rightBottomCorner;
    private final BlockPos rightTopCorner;
    private final ResourceLocation dimension;

    private LongSet involved;

    private final int hashCode;

    private String video = "", audio = "";

    public InWorldScreen(BlockPos leftBottomCorner, BlockPos leftTopCorner, BlockPos rightBottomCorner, BlockPos rightTopCorner, ResourceLocation dimension) {
        this.leftBottomCorner = leftBottomCorner;
        this.leftTopCorner = leftTopCorner;
        this.rightBottomCorner = rightBottomCorner;
        this.rightTopCorner = rightTopCorner;
        this.dimension = dimension;

        this.hashCode = Objects.hash(this.leftBottomCorner, this.leftTopCorner, this.rightBottomCorner, this.rightTopCorner, this.dimension);
    }

    public String video(String fallback) {
        return this.video.isBlank() ? fallback : this.video;
    }

    public String audio(String fallback) {
        return this.audio.isBlank() ? fallback : this.audio;
    }

    public void set(String video, String audio) {
        this.video = video;
        this.audio = audio;
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
        if (this.involved == null) this.involved = LongSets.unmodifiable(InWorldScreen.genInvolved(leftBottomCorner(), leftTopCorner(), rightBottomCorner(), rightTopCorner()));
        return this.involved;
    }

    @Contract(" -> new")
    public @NotNull InWorldScreen copy() {
        InWorldScreen inWorldScreen = new InWorldScreen(this.leftBottomCorner(), this.leftTopCorner(), this.rightBottomCorner(), this.rightTopCorner(), this.dimension());
        inWorldScreen.video = this.video;
        inWorldScreen.audio = this.audio;
        return inWorldScreen;
    }

    @Contract(" -> new")
    public long @NotNull [] toLongArray() {
        return new long[]{this.leftBottomCorner().asLong(), this.leftTopCorner().asLong(), this.rightBottomCorner().asLong(), this.rightTopCorner().asLong()};
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InWorldScreen inWorldScreen) {
            if (inWorldScreen.hashCode != this.hashCode) return false;
            return inWorldScreen.leftBottomCorner().equals(this.leftBottomCorner()) && inWorldScreen.leftTopCorner().equals(this.leftTopCorner()) && inWorldScreen.rightTopCorner().equals(this.rightTopCorner()) && inWorldScreen.rightBottomCorner().equals(this.rightBottomCorner()) && inWorldScreen.dimension().equals(this.dimension());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    public @NotNull String toLocalString() {
        return Component.translatable("screen.narutoloading.arg", this.leftBottomCorner().toShortString(), this.leftTopCorner().toShortString(), this.rightBottomCorner().toShortString(), this.rightTopCorner().toShortString(), this.dimension().toString()).getString();
    }

    @Override
    public @NotNull String toString() {
        return "Screen: {LeftBottom: [" + this.leftBottomCorner().toShortString() + "], LeftTop: [" + this.leftTopCorner().toShortString() + "], RightBottom: [" + this.rightBottomCorner().toShortString() + "], RightTop: [" + this.rightTopCorner().toShortString() + "], Dimension: [" + this.dimension().toString() + "]}";
    }

    public static @NotNull InWorldScreen from(long @NotNull [] corners, ResourceLocation dimension, @Nullable String video, @Nullable String audio) {
        InWorldScreen inWorldScreen = new InWorldScreen(BlockPos.of(corners[0]), BlockPos.of(corners[1]), BlockPos.of(corners[2]), BlockPos.of(corners[3]), dimension);
        inWorldScreen.video = video == null ? "" : video;
        inWorldScreen.audio = audio == null ? "" : audio;
        return inWorldScreen;
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

    private static @NotNull LongSet genInvolved(BlockPos leftBottom, BlockPos leftTop, BlockPos rightBottom, BlockPos rightTop) {
        LongList leftY = getLine(leftBottom, leftTop);
        LongList rightY = getLine(rightBottom, rightTop);
        LongList bottom = getLine(leftBottom, rightBottom);
        LongList top = getLine(leftTop, rightTop);
        LongSet borders = new LongOpenHashSet(leftY.size() + rightY.size() + bottom.size() + top.size() - 4);

        borders.addAll(leftY);
        borders.addAll(rightY);
        borders.addAll(bottom);
        borders.addAll(top);

        return borders;
    }
}
