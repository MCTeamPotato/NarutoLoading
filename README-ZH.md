## NarutoLoading
_Minecraft **最愚蠢的**视频 & 音频播放器。_

问：为什么我 MC 进不去世界？

答：因为被加载界面硬控住了，不忍心 shutdown。

## 干啥的？
在加载界面和各种屏幕背景里放视频，放音频，取代全景图和泥土界面，并让一段时间鼠标不动后除视频之外的所有 GUI 元素停止渲染。
## 怎么用？
1. 下个 `FFmpeg`：https://github.com/BtbN/FFmpeg-Builds/releases 。（非必要）
2. 安装完之后把 `config/narutoloading.json` 里的 `ffmpegExePath` 和 `ffprobeExePath` 改成你电脑里的对应路径。（非必要）
3. 把你要播放的视频和音频放进 config 文件夹，然后再把 `narutoloading.json` 里的 `videoFileName` 和 `audioFileName` 改成视频和音频的文件名。
4. 大功告成，启动游戏吧！
## 关于 FFmpeg
### 自动检测
- 你可以把 FFmpeg 安装到你 MC 的实例文件夹（文件夹结构要求：.minecraft/ffmpeg/bin/ffmpeg.exe 或者 .minecraft/versions/YOUR_GAME_VERSION/ffmpeg/bin/ffmpeg.exe）里，本模组会自动检测它，这样你就不用管那俩文件路径的配置项了。
- 那个 `ffmepg` 文件夹也可以被命名为 `ffmpeg-win` / `ffmpeg-linux` / `ffmpeg-mac`，本模组会根据当前的操作系统自动选择对应的文件。
- 如果你想在整合包里使用这个模组，你就得在发布前考虑下包含那仨文件夹。但这取决于你，毕竟那些文件并不轻量，你的 zip 体积会变大。
### 自动下载
环境内没有可用的 FFmpeg 时，本模组会根据 config 里的下载链接在其他线程中自动下载 FFmpeg 并把它放到你的 MC 实例中，下载完成前，所有的加载页面和背景均保持原版逻辑。

**对国内玩家的特别提醒** ——配置文件里的下载链接是 GitHub，直连很可能连不上的，所以如果没有魔法的话，大概率还是得手动下的，可能有用的链接：[Bilibili](https://www.bilibili.com/opus/957681598556274693), [MCMOD](https://www.mcmod.cn/post/656.html)
## 注意事项
- 音频的配置项若留空则会使用视频里的声音。
- 游戏里按 `F12` 可以重载，config 里 `reloadKey` 那个值就是 `F12`。如果想改成别的比如 `R` 键，就把值改成 `82`（本仓库的 `keys.txt` 文件里有完整对应表）。
- 音频音量由 MC 视频设置`音乐与声音`里的那个`主音量`控制（**不是`音乐`！**）。
- 需要 [Duplicationless](https://www.curseforge.com/minecraft/mc-mods/duplicationless) 作为前置。
## 内存占用
配置文件里有一个 `videoFrameStorageBufferSize`，那个指的是咱缓冲列表的大小，列表里面存了最近从视频里面逐帧拆出来的图片。

这个容量越大，本模组能对抗的卡顿时间就越长，比如如果你是 30 帧/秒的视频，这个容量是 60，视频播放器最多就能对抗 2 秒的卡顿，在这 2 秒内把进度追上音频，直接把音画不同步给你救回来。另外，如果遇到了 2 秒以上的卡顿，那个缓冲列表全部过期了都救不回来音画不同步，本模组则会尝试直接按照当前音频的播放时间直接重启视频播放器。

**但是，增大容量就意味着你游戏内存里存的图片数量增加，内存占用也势必会提高。**

给点粗略的数据吧，就图片的内存占用而言：

1350 * 720 的宽高，60 张，内存占用大概 222 MB。

2560 * 1440 的宽高，60 张，内存占用大概 844 MB。

宽高也受 `maxResolutionWidth` 和 `maxResolutionHeight` 限制的，所以总体来说这个模组的内存占用取决于 `videoFrameStorageBufferSize`、`maxResolutionWidth` 和 `maxResolutionHeight` 这三个配置项，越高，内存需求越大。
## 为啥说蠢？
- 作者直接暴力开线程用 `FFmpeg` 解析视频和音频，写个 `ProcessBuilder` 就摁造，在单个线程里逐像素读取并构建 `NativeImage` 只为在 MC 看火影十周年的登录 CG
- ~~（嗯，其实一开始甚至不打算公开发布的，自己用用爽爽得了，毕竟这实现实在是太神秘了）~~
- ~~（不过虽然暴力，性能上面应该大概可能也许不是什么大问题——除非你是土豆机）~~
## 鸣谢
- DeepSeek （**我的救星**）：感谢它修复了高分辨率下的视频解析卡顿，并提供与 MC 本身声音引擎同步 OpenAL 上下文的思路来避免 NarutoAudioExecutor 产生各种问题
- ChatGPT、Grok、Gemini：感谢它们成天左右脑互搏似地提供问题修复方案，虽然真就一次都没有修复成功只会让视频卡住或者游戏崩溃，到最后全是由作者自己想思路自己解决但还是谢谢它们添乱路上提供的情绪价值。
## 演示视频
（一个视频合集里的 p2 和 p3）
### [火影忍者十周年登录 CG](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=3)
### [Bad Apple 视频](https://www.bilibili.com/video/BV11JmwBNEBr?vd_source=f683457ac8c6976686c5e1492e159005&spm_id_from=333.788.videopod.episodes&p=2)
