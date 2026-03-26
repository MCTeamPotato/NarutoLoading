package me.kall.narutoloading.app.ytdlp;

import java.nio.file.Path;

public class UrlDownloader {
    private final Path video, audio;

    public UrlDownloader(String url, Path outputDirectory, String outputName) {
        this.video = YtDlp.downloadVideo(url, outputDirectory, outputName);
        this.audio = YtDlp.downloadAudio(url, outputDirectory, outputName);
    }

    public Path video() {
        return this.video;
    }

    public Path audio() {
        return this.audio;
    }
}
