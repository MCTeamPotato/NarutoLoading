package me.kall.narutoloading.inworld.block;

import me.kall.narutoloading.NarutoLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

@ParametersAreNonnullByDefault
public class Displayer extends Block {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(NarutoLoading.MOD_ID, "displayer");

    public Displayer() {
        super(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion().setId(ResourceKey.create(Registries.BLOCK, ID)));
    }

    private static final DoubleSupplier OFFSET = () -> ThreadLocalRandom.current().nextBoolean() ? ThreadLocalRandom.current().nextGaussian() : -ThreadLocalRandom.current().nextGaussian();

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + OFFSET.getAsDouble(), pos.getY() + OFFSET.getAsDouble(), pos.getZ() + OFFSET.getAsDouble(), 0.0, 0.0, 0.0);
        }
    }
}