package me.kall.narutoloading.mixin.inworld;

import me.kall.narutoloading.inworld.ext.Leaving;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class MixinEntity implements Leaving {
    @Unique private boolean naruto$keepData;

    @Override
    public boolean naruto$keepData() {
        return this.naruto$keepData;
    }

    @Override
    public void naruto$setKeepData(boolean keepData) {
        this.naruto$keepData = keepData;
    }
}
