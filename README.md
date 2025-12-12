# NarutoLoading
## EN
_The **dumbest** video & audio player mod for Minecraft._
### What does it do?
Play video and audio during loading overlays and screens and replace the vanilla panorama and dirt background. Also stop rendering all the gui elements after your mouse keeps unmoved for a while.
### How to use it?
1. Download `FFmpeg`: https://github.com/BtbN/FFmpeg-Builds/releases
2. After installing it, tweak the `ffmpegExePath` and `ffprobeExePath` in `config/narutoloading.json` to their corresponding paths.
3. Put the video and audio that you want to play into config directory, and then tweak the `videoFileName` and `audioFileName` in `narutoloading.json` to their file names.
4. Launch your game and enjoy it!
### Notes
- If you leave the `audioFileName` config property empty, we will try to use the video's sound.
- You can press `F12` to reload the video and audio during your game. The numeric value of our `reloadKey` config property is the very F12. If you want to tweak it to another key like `R`, write `82` there. (See the `keys.txt` in this repo for complete reference of key mappings)
- The audio this mod plays is not controlled by MC's SoundEngine, so if you want to change its volume, please tweak the speaker of your PC or the `audioVolume` in our config.
- [Duplicationless](https://www.curseforge.com/minecraft/mc-mods/duplicationless) is required.
### Why is it the dumbest?
- I never consider and never want to consider its compatibility with other mods. So if you're using mods that customize loading screens like `FancyMenu` or `DrippyLoadingScreen`, our Mixin may crash your game. ~~Not really real, I considered the compatibility with `Modern UI` mod when writing gui elements hiding feature. And of course you can report any compatibility issues you encounter, and I'll try to resolve them~~~~
- I just violently create thread executors to parse the video and audio in `FFmpeg`, using `ProcessBuilder` and reading the video frame by frame and pixel by pixel in one single thread to build `NativeImage` just for admiring the Naruto Tenth Anniversary Login CG. (Well, in the beginning, I even didn't really decide to release this mod publicly and just wanted to use it privately as it's undoubtedly too silly) (Well though that's violent, its performance should perhaps probably maybe not a big deal as long as your PC is not a potato.)
### Credits
- Deepseek (**MY LIFESAVER**) for fixing the lag of parsing video frames in high resolution.
- **ChatGPT, Grok & Gemini** — for providing various "creative" solutions that never worked properly but froze the video or crashed the game, forcing me to think of real solutions myself. Thanks for the emotional value though!
### Videos for reference
#### [Naruto Tenth Anniversary Login CG](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=3)
#### [Bad Apple](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=2)
___
## ZH
_Minecraft **最愚蠢的**视频 & 音频播放器。_
### 干啥的？
在加载界面里放视频，放音频，取代全景图和泥土界面，以及一段时间鼠标不动后停止渲染除视频之外的所有 GUI 元素。
### 怎么用？
1. 下个 `FFmpeg`：https://github.com/BtbN/FFmpeg-Builds/releases 。
2. 安装完之后把 `config/narutoloading.json` 里的 `ffmpegExePath` 和 `ffprobeExePath` 改成你电脑里的对应路径。
3. 把你要播放的视频和音频放进 config 文件夹，然后再把 `narutoloading.json` 里的 `videoFileName` 和 `audioFileName` 改成视频和音频的文件名。
4. 大功告成，启动游戏吧！
### 注意事项
- 音频的配置项若留空则会使用视频里的声音。
- 游戏里按 `F12` 可以重载，config 里 `reloadKey` 那个值就是 `F12`。如果想改成别的比如 `R` 键，就把值改成 `82`（本仓库的 `keys.txt` 文件里有完整对应表）。
- 音频不归 MC 的 SoundEngine 管的，你要调音量的话得调自己电脑扬声器的音量或者改 config 里的 `audioVolume`。
- 需要 [Duplicationless](https://www.curseforge.com/minecraft/mc-mods/duplicationless) 作为前置。
### 为啥说蠢？
- 作者完全没有也不想考虑兼容性，因此如果你装了 `FancyMenu` / `DrippyLoadingScreen` 之类的自定义加载页面模组，有可能会炸 Mixin。~~骗你的，其实在做隐藏 GUI 元素的时候考虑了跟 `Modern UI` 的兼容。你当然也可以向我报告你遇到的兼容性问题，我会修复它们的。~~
- 作者直接暴力开线程用 `FFmpeg` 解析视频和音频，写个 `ProcessBuilder `就摁造，在单个线程里逐像素读取并构建 `NativeImage` 只为在 MC 看火影十周年的登录 CG（嗯，其实一开始甚至不打算公开发布的，自己用用爽爽得了，毕竟这实现实在是太神秘了）（不过虽然暴力，性能上面应该大概可能也许不是什么大问题——除非你是土豆机）。
### 鸣谢
- DeepSeek （**我的救星**）：感谢它修复了高分辨率下的视频解析卡顿。
- ChatGPT、Grok、Gemini：感谢它们成天左右脑互搏似地提供问题修复方案，虽然一次都没有修复成功只会让视频卡住或者游戏崩溃，到最后全是由作者自己想思路自己解决但还是谢谢它们添乱路上提供的情绪价值。
### 演示视频
#### [火影忍者十周年登录 CG](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=3)
#### [Bad Apple 视频](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=2)
