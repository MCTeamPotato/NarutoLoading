package me.kall.narutoloading.inworld.block;

import com.google.common.collect.Lists;
import me.kall.duplicationless.config.JsonConfig;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Displayer extends Block {
    private static final JsonConfig CONFIG = JsonConfig.create("naruto_displayers", "1")
            .put("Displayers", Lists.newArrayList("narutoloading:displayer", "minecraft:nether_portal", "minecraft:end_portal", "twilightforest:twilight_portal"))
            .initialize();

    public static final Set<ResourceLocation> DISPLAYERS = CONFIG.getStream("Displayers", String.class).map(ResourceLocation::parse).collect(Collectors.toSet());

    public Displayer() {
        super(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion());
    }

    private static final DoubleSupplier OFFSET = () -> ThreadLocalRandom.current().nextBoolean() ? ThreadLocalRandom.current().nextGaussian() : -ThreadLocalRandom.current().nextGaussian();

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.PORTAL, true, pos.getX() + OFFSET.getAsDouble(), pos.getY() + OFFSET.getAsDouble(), pos.getZ() + OFFSET.getAsDouble(), 0.0, 0.0, 0.0);
        }
    }
}