package me.kall.narutoloading.inworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

@ParametersAreNonnullByDefault
public class Displayer extends Block {
    public Displayer() {
        super(BlockBehaviour.Properties.of(new Material.Builder(MaterialColor.COLOR_BLUE).build()).strength(3.0F).sound(SoundType.METAL).noOcclusion());
    }

    private static final DoubleSupplier OFFSET = () -> ThreadLocalRandom.current().nextBoolean() ? ThreadLocalRandom.current().nextGaussian() : -ThreadLocalRandom.current().nextGaussian();

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.PORTAL, true, pos.getX() + OFFSET.getAsDouble(), pos.getY() + OFFSET.getAsDouble(), pos.getZ() + OFFSET.getAsDouble(), 0.0, 0.0, 0.0);
        }
    }
}