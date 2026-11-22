package me.kall.narutoloading.media;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MediaNaming {
    static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mkv", "webm", "mov", "m4v", "avi", "flv");
    static final List<String> AUDIO_EXTENSIONS = List.of("ogg", "opus", "mp3", "flac", "wav", "m4a", "aac");

    public static final String AUDIO_SUFFIX = ".audio";

    private static final String[] FILE_FILTERS = buildFileFilters();

    private MediaNaming() {}

    public static boolean isMediaFile(@NotNull String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        return VIDEO_EXTENSIONS.contains(extension) || AUDIO_EXTENSIONS.contains(extension);
    }

    public static @NotNull String[] fileFilters() {
        return FILE_FILTERS.clone();
    }

    public static @NotNull String mediaName(@NotNull String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        return AUDIO_EXTENSIONS.contains(extension) ? stripAudioSuffix(baseName) : baseName;
    }

    public static boolean wouldOverwrite(@NotNull String name, @NotNull String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);

        if (VIDEO_EXTENSIONS.contains(extension)) return baseName.equalsIgnoreCase(name.trim());
        if (AUDIO_EXTENSIONS.contains(extension)) return baseName.equalsIgnoreCase(name.trim() + AUDIO_SUFFIX);

        return false;
    }

    private static @NotNull String stripAudioSuffix(@NotNull String baseName) {
        int at = baseName.length() - AUDIO_SUFFIX.length();
        if (at <= 0) return baseName;
        if (!baseName.regionMatches(true, at, AUDIO_SUFFIX, 0, AUDIO_SUFFIX.length())) return baseName;
        return baseName.substring(0, at);
    }

    private static String @NotNull [] buildFileFilters() {
        List<String> extensions = new ArrayList<>(VIDEO_EXTENSIONS.size() + AUDIO_EXTENSIONS.size());
        extensions.addAll(VIDEO_EXTENSIONS);
        extensions.addAll(AUDIO_EXTENSIONS);

        String[] patterns = new String[extensions.size()];
        for (int index = 0; index < patterns.length; index++) patterns[index] = "*." + extensions.get(index);
        return patterns;
    }
}
