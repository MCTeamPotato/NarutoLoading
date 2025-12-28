package me.kall.narutoloading.inworld.gui;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.data.ClientScreens;
import me.kall.narutoloading.inworld.data.Displayers;
import me.kall.narutoloading.inworld.init.NarutoPackets;
import me.kall.narutoloading.inworld.network.SelectionTrigger;
import me.kall.narutoloading.noworld.gui.SourcesSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class InWorldSelectionScreen extends SourcesSelectionScreen {
    private final ClientScreens.ClientScreen clientScreen;

    public InWorldSelectionScreen(Screen lastScreen, ClientScreens.ClientScreen clientScreen) {
        super(lastScreen);
        this.clientScreen = clientScreen;
    }

    @Override
    protected void onDone() {
        String video = this.videoBox.getValue();
        String audio = this.audioBox.getValue();
        this.clientScreen.screen().video = video;
        this.clientScreen.screen().audio = audio;
        this.clientScreen.renderer().shutdown();
        this.clientScreen.renderer().setup();
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    @Mod.EventBusSubscriber(modid = NarutoLoading.MOD_ID)
    public static final class Trigger {
        private static int screenTriggerable = 0;

        @SubscribeEvent
        public static void serverTick(TickEvent.@NotNull ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                if (screenTriggerable > 0) screenTriggerable--;
            }
        }

        @SubscribeEvent
        public static void rightClickScreen(PlayerInteractEvent.@NotNull RightClickBlock event) {
            BlockPos pos = event.getPos();
            if (event.getLevel() instanceof ServerLevel level && Displayers.isDisplayer(level, pos.asLong()) && event.getEntity() instanceof ServerPlayer player) {
                if (screenTriggerable > 0) return;
                NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SelectionTrigger(pos.asLong()));
                screenTriggerable = 20;
            }
        }
    }
}
