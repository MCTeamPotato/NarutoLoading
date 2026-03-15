package me.kall.narutoloading.inworld.gui.util;

import me.kall.narutoloading.NarutoLoading;
import me.kall.narutoloading.inworld.core.ClientScreensRenderer;
import me.kall.narutoloading.inworld.core.NarutoInWorldRenderer;
import me.kall.narutoloading.inworld.network.ArgUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourceZipGenerator {
    private final String convertedAudioPath;
    private final String packName;
    public final String id;

    private static final String RESOURCE_PACKS = FMLLoader.getGamePath().resolve("resourcepacks").toAbsolutePath().toString();

    public ResourceZipGenerator(String convertedAudioPath) {
        this.convertedAudioPath = convertedAudioPath;
        this.id = Paths.get(this.convertedAudioPath).getParent().getFileName().toString();
        this.packName = "NarutoLoadingAudioSource-" + this.id + ".zip";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void generate() {
        try {
            File resourcePacksDir = new File(RESOURCE_PACKS);
            if (!resourcePacksDir.exists()) resourcePacksDir.mkdirs();

            File zipFile = new File(resourcePacksDir, packName);
            if (zipFile.exists()) {
                NarutoLoading.LOGGER.debug("{}Resource pack already exists: {}, skipping generation", NarutoLoading.info(), zipFile.getAbsolutePath());
                return;
            }

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                addPackMcmeta(zos);
                addAudioFiles(zos);
                NarutoLoading.LOGGER.debug("{}Successfully created resource pack: {}", NarutoLoading.info(), zipFile.getAbsolutePath());
            }
        } catch (Exception e) {
            NarutoLoading.LOGGER.error("Failed to generate resource pack", e);
        }
    }

    public void reload(NarutoInWorldRenderer renderer) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            try {
                PackRepository repository = minecraft.getResourcePackRepository();
                repository.reload();
                String packId = "file/" + this.packName;

                Collection<String> currentSelected = repository.getSelectedIds();

                ResourceLocation expectedSound = ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, this.id);

                if (currentSelected.contains(packId)) {
                    NarutoLoading.LOGGER.debug("{}Resource pack {} already active, skipping reload", NarutoLoading.info(), packId);
                    renderer.screen.setLocalSound(ResourceLocation.fromNamespaceAndPath(NarutoLoading.MOD_ID, this.id));
                    PacketDistributor.sendToServer(new ArgUpdatePacket(renderer.screen));
                    ClientScreensRenderer.reload();
                    return;
                }

                Pack pack = repository.getPack(packId);
                if (pack == null) {
                    NarutoLoading.LOGGER.warn("Could not find pack: {}", packId);
                    return;
                }

                Collection<String> selected = new ArrayList<>(currentSelected);
                selected.removeIf(s -> s.equals("file/" + this.packName));
                if (!selected.contains(packId)) selected.add(packId);

                repository.setSelected(selected);
                minecraft.options.resourcePacks = new ArrayList<>(selected);
                minecraft.options.save();

                renderer.screen.setLocalSound(expectedSound);
                minecraft.reloadResourcePacks();
                NarutoLoading.LOGGER.debug("{}Successfully activated resource pack: {}", NarutoLoading.info(), packId);

                PacketDistributor.sendToServer(new ArgUpdatePacket(renderer.screen));
            } catch (Exception e) {
                NarutoLoading.LOGGER.error("Error activating resource pack", e);
            }
        });
    }

    private void addPackMcmeta(@NotNull ZipOutputStream zos) throws Exception {
        ZipEntry entry = new ZipEntry("pack.mcmeta");
        zos.putNextEntry(entry);

        String mcmeta = """
                {
                  "pack": {
                    "pack_format": 9,
                    "description": "NarutoLoading Audio Sources"
                  }
                }""";

        zos.write(mcmeta.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void addAudioFiles(ZipOutputStream zos) throws Exception {
        File audioFile = new File(this.convertedAudioPath);
        if (!audioFile.exists() || !audioFile.isFile()) return;

        String entryPath = String.format("assets/%s/sounds/%s.ogg", NarutoLoading.MOD_ID, this.id);
        ZipEntry entry = new ZipEntry(entryPath);
        zos.putNextEntry(entry);

        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }

        zos.closeEntry();
        NarutoLoading.LOGGER.debug("{}Added audio file to resource pack: {}", NarutoLoading.info(), entryPath);

        addSoundsJson(zos);
    }

    private void addSoundsJson(@NotNull ZipOutputStream zos) throws Exception {
        ZipEntry entry = new ZipEntry(String.format("assets/%s/sounds.json", NarutoLoading.MOD_ID));
        zos.putNextEntry(entry);

        String json = String.format("""
                {
                  "%s": {
                    "sounds": [
                      "%s:%s"
                    ]
                  }
                }""", this.id, NarutoLoading.MOD_ID, this.id);

        zos.write(json.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
