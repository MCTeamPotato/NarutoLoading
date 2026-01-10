package me.kall.narutoloading.mixin.inworld;

import me.kall.narutoloading.inworld.ext.ScreenLevel;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public class MixinServerLevel implements ScreenLevel {
    @Unique private boolean naruto$isClearingScreens = false;

    @Override
    public boolean naruto$isClearingScreens() {
        return this.naruto$isClearingScreens;
    }

    @Override
    public void naruto$setClearingScreens(boolean isClearingScreens) {
        this.naruto$isClearingScreens = isClearingScreens;
    }
}
