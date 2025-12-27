package me.kall.narutoloading.gui;

import me.kall.narutoloading.core.detection.inworld.InWorldScreen;
import me.kall.narutoloading.data.NarutoConfig;

public class InWorldSelectScreen extends SourcesSelectionScreen {
    private final InWorldScreen inWorldScreen;

    public InWorldSelectScreen(InWorldScreen inWorldScreen) {
        super(null);
        this.inWorldScreen = inWorldScreen;
    }

    @Override
    protected void onDone() {
        String video = this.videoBox.getValue();
        String audio = this.audioBox.getValue();
        this.inWorldScreen.video = NarutoConfig.toPath(video);
        this.inWorldScreen.audio = NarutoConfig.toPath(audio);
    }
}