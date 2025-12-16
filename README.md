# NarutoLoading

[中文版本](https://github.com/MCTeamPotato/NarutoLoading/blob/1201/README-ZH.md)

_The **dumbest** video & audio player mod for Minecraft._

Q: I cannot enter my MC world, why?

A: Because I just failed to stop admiring my loading background. It will be shutdown after entering the world and I will cry.
## What does it do?
Play video and audio during loading overlays and screens and replace the vanilla panorama and dirt background. Also stop rendering all the gui elements after your mouse remains idle for a while.
## How to use it?
1. Download `FFmpeg`: https://github.com/BtbN/FFmpeg-Builds/releases (Not a hard requirement)
2. After installing it, tweak the `ffmpegExePath` and `ffprobeExePath` in `config/narutoloading.json` to their actual file paths on your system. (Not a hard requirement)
3. Put the video and audio that you want to play into config directory, and then tweak the `videoFileName` and `audioFileName` in `narutoloading.json` to their file names.
4. Launch your game and enjoy it!
## About FFmpeg
### Auto Detection
- You can simply install FFmpeg into your Minecraft instance directory (file directory structure requirement: .minecraft/ffmpeg/bin/ffmpeg.exe, or .minecraft/versions/YOUR_GAME_VERSION/ffmpeg/bin/ffmpeg.exe), and NarutoLoading will auto-detect it regardless of the config options.
- The `ffmpeg` directory can also be named as `ffmpeg-win` / `ffmpeg-linux` / `ffmpeg-mac` and we will select the corresponding one for the current OS on runtime.
- If you want to redistribute this mod in your modpack, you do need to consider including these three FFmpeg files in your modpack. It's up to you, though, as these files are not light and make your zip big.
### Auto Download
If there are no usable FFmpeg in your runtime environment, NarutoLoading will auto download FFmpeg in a background thread according to the download links in the config and put the downloaded files into your MC instance directory. Before the download task is completed, all the loading screens and backgrounds will remain vanilla. 
## Notes
- If you leave the `audioFileName` config property empty, we will try to use the video's sound.
- You can press `F12` to reload the video and audio during your game. By default, the reloadKey is set to F12. To change it to another key like R, set the value to 82. (See the `keys.txt` in this repo for complete reference of key mappings)
- The audio volume can be controlled by MC's `Video Settings` -> `Music & Sounds` -> `Master Volume` (**NOT THE `Music` !!!**).
- [Duplicationless](https://www.curseforge.com/minecraft/mc-mods/duplicationless) is required.
## Memory Cost
The `videoFrameStorageBufferSize` config property means the size of our buffer list, which contains the images parsed from the video frame by frame and queued for rendering.

If you increase this config value, our ability to compete against lag spike during video playing will be better. For example, if your video is 30 fps and this config value is 60, our video executor can compete against lag spike up to 2 seconds. We can catch up with the audio within those 2 seconds and preserve the viewing experience. Additionally, if the lag spike is longer than 2 seconds and we failed to resolve the audio-video desynchronization even after clearing the buffer list, we will attempt to restart the whole video player based on the elapsed time of the audio.

**However, increasing this config value does mean the total amount of stored images in your game is increased, and so your game's required memory will be higher.**

Some data, as for images' memory cost:

1350 * 720 width & height, 60 images, memory cost: 222 MB

2560 * 1440 width & height, 60 images, memory cost: 844 MB

The width and height is also limited by the `maxResolutionWidth` and `maxResolutionHeight` config property. So generally this mod's memory cost increasingly depends on `videoFrameStorageBufferSize`, `maxResolutionWidth` and `maxResolutionHeight`.
## Why is it the dumbest?
- I just violently create thread executors to parse the video and audio in `FFmpeg`, using `ProcessBuilder` and reading the video frame by frame and pixel by pixel in one single thread to build `NativeImage` just for admiring the Naruto Tenth Anniversary Login CG. 
- ~~(Well, in the beginning, I even didn't really decide to release this mod publicly and just wanted to use it privately as it's undoubtedly too silly)~~
- ~~(Well though that's violent, its performance should perhaps probably maybe not a big deal as long as your PC is not a potato.)~~
## Credits
- Deepseek (**MY LIFESAVER**) for fixing the lag of parsing video frames in high resolution and suggesting the synchronization of our NarutoAudioExecutor with the OpenAL context in MC's SoundEngine to resolve various sound issues.
- ChatGPT, Grok & Gemini for providing various "creative" solutions that definitely never worked properly but froze the video or crashed the game, forcing me to think of real solutions myself. Thanks for the emotional value though!
## Videos for reference
(p2 & p3 in one videopod)
#### [Naruto Tenth Anniversary Login CG](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=3)
### [Bad Apple](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=2)
___