package me.kall.narutoloading.client.ui;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.core.util.GamePaths;
import me.kall.narutoloading.config.NarutoConfig;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

final class HelpPage {
    private HelpPage() {}

    static void build(@NotNull MuiSettingsPage page) {
        page.shimmerAll();

        page
                .section("narutoloading.help.section.about", section -> section
                        .note("narutoloading.help.about.purpose", "narutoloading.help.about.purpose.desc")
                        .note("narutoloading.help.about.when", "narutoloading.help.about.when.desc")
                        .note("narutoloading.help.about.open", "narutoloading.help.about.open.desc"))
                .section("narutoloading.help.section.sources", section -> section
                        .note("narutoloading.help.sources.folder", "narutoloading.help.sources.folder.desc")
                        .note("narutoloading.help.sources.pair", "narutoloading.help.sources.pair.desc")
                        .note("narutoloading.help.sources.formats", "narutoloading.help.sources.formats.desc")
                        .note("narutoloading.help.sources.mode", "narutoloading.help.sources.mode.desc"))
                .section("narutoloading.help.section.downloads", section -> section
                        .note("narutoloading.help.download.install", "narutoloading.help.download.install.desc")
                        .note("narutoloading.help.download.pick", "narutoloading.help.download.pick.desc")
                        .note("narutoloading.help.download.name", "narutoloading.help.download.name.desc")
                        .note("narutoloading.help.download.reload", "narutoloading.help.download.reload.desc"))
                .section("narutoloading.help.section.ffmpeg", section -> section
                        .note("narutoloading.help.ffmpeg.why", "narutoloading.help.ffmpeg.why.desc")
                        .note("narutoloading.help.ffmpeg.both", "narutoloading.help.ffmpeg.both.desc")
                        .note("narutoloading.help.ffmpeg.where", "narutoloading.help.ffmpeg.where.desc")
                        .note("narutoloading.help.ffmpeg.restart", "narutoloading.help.ffmpeg.restart.desc")
                        .note("narutoloading.help.ffmpeg.trouble", "narutoloading.help.ffmpeg.trouble.desc"))
                .section("narutoloading.help.section.walls", section -> section
                        .note("narutoloading.help.walls.build", "narutoloading.help.walls.build.desc")
                        .note("narutoloading.help.walls.config", "narutoloading.help.walls.config.desc")
                        .note("narutoloading.help.walls.media", "narutoloading.help.walls.media.desc")
                        .note("narutoloading.help.walls.server", "narutoloading.help.walls.server.desc"))
                .section("narutoloading.help.section.paths", section -> section
                        .value("narutoloading.help.configFile", FMLPaths.CONFIGDIR.get().resolve("narutoloading.properties").toString())
                        .value("narutoloading.help.sources", sourcesDirectory().toString())
                        .value("narutoloading.help.version", modVersion())
                        .value("narutoloading.help.environment", SharedConstants.getCurrentVersion().getName()));
    }

    private static @NotNull Path sourcesDirectory() {
        return GamePaths.resolve(NarutoConfig.sourcesDir());
    }

    private static @NotNull String modVersion() {
        return ModList.get().getModContainerById(NarutoLoading.MOD_ID).map(container -> container.getModInfo().getVersion().toString()).orElse("?");
    }
}
