package me.kall.narutoloading.mixin;

import me.kall.narutoloading.bootstrap.Integration;
import me.kall.narutoloading.wall.client.light.WallLightHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class NarutoMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("NarutoLoading");

    private static final String LIGHT_EMISSION = "getLightEmission";
    private static final String LIGHT_EMISSION_DESC = "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I";
    private static final String WALL_LIGHTS = "me/kall/narutoloading/wall/client/light/WallLights";
    private static final String MIXIN_MERGED = "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;";

    @Override
    public void onLoad(String s) {
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.endsWith("MixinBlurHandler")) return Integration.MODERN_UI;
        if (mixinClassName.endsWith("MixinUIManager")) return Integration.MODERN_UI;
        if (mixinClassName.endsWith("MixinMuiConfigFragment")) return Integration.NARUTO_UI;
        if (mixinClassName.endsWith("MixinBlockOcclusionCache")) return Integration.EMBEDDIUM;
        return true;
    }

    @Override public String getRefMapperConfig() { return ""; }
    @Override public void acceptTargets(Set<String> set, Set<String> set1) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override public void preApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo iMixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode classNode, @NotNull String mixinClassName, IMixinInfo iMixinInfo) {
        if (!mixinClassName.endsWith("MixinBlockState")) return;

        MethodNode merged = classNode.methods.stream()
                .filter(method -> LIGHT_EMISSION.equals(method.name) && LIGHT_EMISSION_DESC.equals(method.desc))
                .findFirst()
                .orElse(null);

        boolean applied = merged != null && callsWallLights(merged);

        WallLightHook.active(applied);

        LOGGER.info("[NarutoLoading] BlockState#getLightEmission owner={}, wallLightHook={}", mergedOwner(merged), applied);
        if (!applied) LOGGER.warn("[NarutoLoading] BlockState#getLightEmission is already provided by another mod, wall light is disabled.");
    }

    private static String mergedOwner(@Nullable MethodNode method) {
        if (method == null) return "<missing>";

        if (method.visibleAnnotations == null) return "<vanilla>";

        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (!MIXIN_MERGED.equals(annotation.desc) || annotation.values == null) continue;

            List<Object> values = annotation.values;
            for (int index = 0; index + 1 < values.size(); index += 2) {
                if ("mixin".equals(values.get(index))) return String.valueOf(values.get(index + 1));
            }
        }

        return "<unknown>";
    }

    private static boolean callsWallLights(@NotNull MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == Opcodes.INVOKESTATIC && instruction instanceof MethodInsnNode call && call.owner.equals(WALL_LIGHTS) && call.name.equals("emissionAt")) return true;
        }
        return false;
    }
}
