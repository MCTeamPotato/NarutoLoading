package me.kall.narutoloading.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Displayer extends Block {
    public Displayer() {
        super(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion());
    }

    private static final DoubleSupplier OFFSET = () -> ThreadLocalRandom.current().nextBoolean() ? ThreadLocalRandom.current().nextGaussian() : -ThreadLocalRandom.current().nextGaussian();

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + OFFSET.getAsDouble(), pos.getY() + OFFSET.getAsDouble(), pos.getZ() + OFFSET.getAsDouble(), 0.0, 0.0, 0.0);
        }
    }
}
