# NarutoLoading
_Minecraft 最愚蠢的视频 & 音频播放器。_

### 干啥的？
在加载界面里放视频，放音频，取代全景图和泥土界面，以及一段时间鼠标不动后停止渲染除视频之外的所有 GUI 元素。
### 怎么用？
1. 下个 FFmpeg：https://github.com/BtbN/FFmpeg-Builds/releases 。
2. 安装完之后把 config/narutoloading.json 里的 ffmpegExePath 和 ffprobeExePath 改成你电脑里的对应路径。
3. 把你要播放的视频和音频放进 config 文件夹，然后再把 narutoloading.json 里的 videoFileName 改成那个视频的文件名。
4. 大功告成，启动游戏吧！
### 注意事项
- 音频若留空则会使用视频里的声音，或者你自己改成你 mp3 的文件名。
- 游戏里按 F12 可以重载， reloadKey 那个值就是 F12。如果想改成别的比如 R 键，就把值改成 82（本仓库的 keys.txt 文件里有完整对应表）。
- 音频不归 MC 的 SoundEngine 管的，你要调音量的话得调自己电脑扬声器的音量或者改 config 里的 audioVolume。
- maxWidthForExecution 和 maxHeightForExecution 默认是 1280×720，太高会导致解码卡顿进而视频停滞，太低画质会变差，**请务必根据自己电脑配置调整**。
- 需要 [Duplicationless](https://www.curseforge.com/minecraft/mc-mods/duplicationless) 作为前置。
### 为啥说蠢？
- 作者完全没有也不想考虑兼容性，因此如果你装了 FancyMenu / DrippyLoadingScreen 之类的自定义加载页面模组，有可能会炸 Mixin。~~骗你的，其实在做隐藏 GUI 元素的时候考虑了跟 Modern UI 的兼容性~~
- 作者直接暴力开线程用 FFmpeg 解析视频和音频，写个 ProcessBuilder 就摁造，逐像素读取并构建 NativeImage 只为在 MC 看火影十周年的登录 CG（嗯，其实一开始甚至不打算公开发布的，自己用用爽爽得了）。
- 你可以想象每次你改 MC 的分辨率或者切出切回 MC 的窗口，都会让本模组的各种进程几乎完全重启吗？
- 总之，土豆机请小心使用本模组。
### 演示视频
#### 火影忍者十周年登录动画
<video controls src="naruto.mp4" title="NarutoLoading"></video>

#### Bad Apple 视频
<video controls src="badapple.mp4" title="BadAppleLoading"></video>