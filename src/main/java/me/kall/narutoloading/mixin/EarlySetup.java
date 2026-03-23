package me.kall.narutoloading.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import me.kall.narutoloading.common.env.BaseEnv;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class EarlySetup implements IMixinConfigPlugin {
    @Override
    public void onLoad(String s) {
        MixinExtrasBootstrap.init();
        if (FMLLoader.getDist().isClient()) BaseEnv.setupEnv(true);
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return Lists.newArrayList();
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
