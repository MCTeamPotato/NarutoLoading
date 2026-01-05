package me.kall.narutoloading.inworld.core;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
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

    private String relativeVideoPath = NarutoLoading.BLANK, relativeAudioPath = NarutoLoading.BLANK;
    private boolean hideInner = true;

    private ResourceLocation localSound = NO_LOCAL_SOUND;
    private float soundVolume = 4.0F;

    private LongSet areaInvolved;
    private LongSet borderInvolved;

    private int hashCode;
    private boolean genHash = true;

    private int videoWidth = 1280, videoHeight = 720;

    public static final ResourceLocation NO_LOCAL_SOUND = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "empty");
    public static final ResourceLocation HAS_LOCAL_SOUND = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, "pending");

    public InWorldScreen(BlockPos leftBottomCorner, BlockPos leftTopCorner, BlockPos rightBottomCorner, BlockPos rightTopCorner, ResourceLocation dimension) {
        this.leftBottomCorner = leftBottomCorner;
        this.leftTopCorner = leftTopCorner;
        this.rightBottomCorner = rightBottomCorner;
        this.rightTopCorner = rightTopCorner;
        this.dimension = dimension;
    }

    public String relativeVideoPath(String fallback) {
        return this.relativeVideoPath.isBlank() ? fallback : this.relativeVideoPath;
    }

    public String relativeAudioPath(String fallback) {
        return this.relativeAudioPath.isBlank() ? fallback : this.relativeAudioPath;
    }

    public void setPath(String relativeVideoPath, String relativeAudioPath) {
        this.relativeVideoPath = relativeVideoPath;
        this.relativeAudioPath = relativeAudioPath;
    }

    public void setSize(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public void setLocalSound(ResourceLocation localSound) {
        this.localSound = localSound;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }

    public void setHideInner(boolean hideInner) {
        this.hideInner = hideInner;
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

    public LongSet borderInvolved() {
        if (this.borderInvolved == null) this.borderInvolved = InWorldScreen.borderInvolved(leftBottomCorner(), leftTopCorner(), rightBottomCorner(), rightTopCorner());
        return this.borderInvolved;
    }

    public @NotNull LongSet areaInvolved() {
        if (this.areaInvolved == null) this.areaInvolved = InWorldScreen.areaInvolved(this.area());
        return this.areaInvolved;
    }

    public boolean isLocalSound() {
        return !this.localSound.equals(NO_LOCAL_SOUND);
    }

    public ResourceLocation localSound() {
        return this.localSound;
    }

    public float soundVolume() {
        return this.soundVolume;
    }

    public int videoWidth() {
        return this.videoWidth;
    }

    public int videoHeight() {
        return this.videoHeight;
    }

    public boolean hideInner() {
        return this.hideInner;
    }

    @Contract(" -> new")
    public @NotNull InWorldScreen finalCopy() {
        return new InWorldScreen(this.leftBottomCorner(), this.leftTopCorner(), this.rightBottomCorner(), this.rightTopCorner(), this.dimension());
    }

    @Contract(" -> new")
    public long @NotNull [] toLongArray() {
        return new long[]{this.leftBottomCorner().asLong(), this.leftTopCorner().asLong(), this.rightBottomCorner().asLong(), this.rightTopCorner().asLong()};
    }

    public double centerX() {
        return (double) (this.leftBottomCorner.getX() + this.rightTopCorner.getX()) / 2;
    }

    public double centerY() {
        return (double) (this.leftBottomCorner.getY() + this.rightTopCorner.getY()) / 2;
    }

    public double centerZ() {
        return (double) (this.leftBottomCorner.getZ() + this.rightTopCorner.getZ()) / 2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InWorldScreen inWorldScreen) {
            if (inWorldScreen.hashCode() != this.hashCode()) return false;
            return inWorldScreen.leftBottomCorner().equals(this.leftBottomCorner()) && inWorldScreen.leftTopCorner().equals(this.leftTopCorner()) && inWorldScreen.rightTopCorner().equals(this.rightTopCorner()) && inWorldScreen.rightBottomCorner().equals(this.rightBottomCorner()) && inWorldScreen.dimension().equals(this.dimension());
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (this.genHash) {
            this.genHash = false;
            this.hashCode = Objects.hash(this.leftBottomCorner, this.leftTopCorner, this.rightBottomCorner, this.rightTopCorner, this.dimension);
        }
        return this.hashCode;
    }

    public @NotNull String toLocalString() {
        return Component.translatable("screen.narutoloading.arg", this.leftBottomCorner().toShortString(), this.leftTopCorner().toShortString(), this.rightBottomCorner().toShortString(), this.rightTopCorner().toShortString(), this.dimension().toString()).getString();
    }

    @Override
    public @NotNull String toString() {
        return "Screen: {LeftBottom: [" + this.leftBottomCorner().toShortString() + "], LeftTop: [" + this.leftTopCorner().toShortString() + "], RightBottom: [" + this.rightBottomCorner().toShortString() + "], RightTop: [" + this.rightTopCorner().toShortString() + "], Dimension: [" + this.dimension().toString() + "], Video: [Path: " + this.relativeVideoPath + ", Width: " + this.videoWidth + ", Height: " + this.videoHeight + "], Audio: [" + this.relativeAudioPath + "], LocalSound: [Location: " + this.localSound().toString() +", Volume: " + this.soundVolume + "]}";
    }

    private @NotNull AABB area() {
        double leftBottomCornerX = this.leftBottomCorner.getX();
        double leftBottomCornerY = this.leftBottomCorner.getY();
        double leftBottomCornerZ = this.leftBottomCorner.getZ();

        double leftTopCornerX = this.leftTopCorner.getX();
        double leftTopCornerY = this.leftTopCorner.getY();
        double leftTopCornerZ = this.leftTopCorner.getZ();

        double rightBottomCornerX = this.rightBottomCorner.getX();
        double rightBottomCornerY = this.rightBottomCorner.getY();
        double rightBottomCornerZ = this.rightBottomCorner.getZ();

        double rightTopCornerX = this.rightTopCorner.getX();
        double rightTopCornerY = this.rightTopCorner.getY();
        double rightTopCornerZ = this.rightTopCorner.getZ();

        double minX = Math.min(Math.min(leftBottomCornerX, leftTopCornerX), Math.min(rightBottomCornerX, rightTopCornerX));
        double minY = Math.min(Math.min(leftBottomCornerY, leftTopCornerY), Math.min(rightBottomCornerY, rightTopCornerY));
        double minZ = Math.min(Math.min(leftBottomCornerZ, leftTopCornerZ), Math.min(rightBottomCornerZ, rightTopCornerZ));

        double maxX = Math.max(Math.max(leftBottomCornerX, leftTopCornerX), Math.max(rightBottomCornerX, rightTopCornerX));
        double maxY = Math.max(Math.max(leftBottomCornerY, leftTopCornerY), Math.max(rightBottomCornerY, rightTopCornerY));
        double maxZ = Math.max(Math.max(leftBottomCornerZ, leftTopCornerZ), Math.max(rightBottomCornerZ, rightTopCornerZ));

        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public static @NotNull InWorldScreen from(long @NotNull [] corners, ResourceLocation dimension, @Nullable String video, @Nullable String audio, ResourceLocation localSound, float soundVolume, boolean hideInner, int videoWidth, int videoHeight) {
        InWorldScreen inWorldScreen = new InWorldScreen(BlockPos.of(corners[0]), BlockPos.of(corners[1]), BlockPos.of(corners[2]), BlockPos.of(corners[3]), dimension);
        inWorldScreen.setPath(video == null ? NarutoLoading.BLANK : video, audio == null ? NarutoLoading.BLANK : audio);
        inWorldScreen.setLocalSound(localSound);
        inWorldScreen.setSoundVolume(soundVolume);
        inWorldScreen.setHideInner(hideInner);
        inWorldScreen.setSize(videoWidth, videoHeight);
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

    private static @NotNull LongSet borderInvolved(BlockPos leftBottom, BlockPos leftTop, BlockPos rightBottom, BlockPos rightTop) {
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

    private static @NotNull LongSet areaInvolved(@NotNull AABB aabb) {
        LongSet blocks = new LongOpenHashSet();

        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX) - 1;
        int maxY = (int) Math.floor(aabb.maxY) - 1;
        int maxZ = (int) Math.floor(aabb.maxZ) - 1;

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            blocks.add(pos.asLong());
        }

        return blocks;
    }
}
