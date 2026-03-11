package me.kall.narutoloading.mixin.inworld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.kall.narutoloading.inworld.ext.Leaving;
import me.kall.narutoloading.inworld.ext.ScreenLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

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

    @WrapOperation(method = "removeEntityComplete", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false))
    private boolean onRemoveEvent(IEventBus eventBus, Event event, Operation<Boolean> original, @Local(argsOnly = true) Entity entity, @Local(argsOnly = true) boolean keepData) {
        ((Leaving)entity).naruto$setKeepData(keepData);
        return original.call(eventBus, event);
    }
}
