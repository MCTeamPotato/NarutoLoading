package me.kall.narutoloading.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.kall.narutoloading.Integration;
import me.kall.narutoloading.NarutoLoading;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoLoading.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NarutoKeys {
    public static final KeyMapping OPEN_SCREEN = new KeyMapping("key.narutoloading.open_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.narutoloading");

    public static final KeyMapping TOGGLE_COMPAT_MODE = new KeyMapping("key.narutoloading.toggle_compat_mode", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, "key.categories.narutoloading");

    private NarutoKeys() {}

    @SubscribeEvent
    public static void register(@NotNull RegisterKeyMappingsEvent event) {
        if (Integration.NARUTO_UI) event.register(OPEN_SCREEN);
        event.register(TOGGLE_COMPAT_MODE);
    }
}
