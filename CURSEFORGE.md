# NarutoLoading

**Video loading screens and in-world video walls for Minecraft.**

NarutoLoading replaces the screens you stare at while the game starts — the splash, the main menu, the world-loading screen — with a video of your choice, sound included. It also adds video walls: build a frame out of blocks, mark two corners, and the frame starts playing.

Despite the name, nothing here is Naruto-specific. Any video works.

---

## What it does

### Video loading screens

- Replaces the early splash window, the main-menu backdrop and the world-loading screen with your video. The vanilla progress bar stays on top, so you can still see what is loading.
- The same clip keeps playing from the splash into the menu instead of restarting, so there is no black gap between the two.
- Plays with sound and loops while no world is loaded. Vanilla music is muted automatically while the video has an audio track (optional).
- The frame limiter is raised to the video's frame rate while it plays, so a 60 fps clip is not cut down to 30.
- Stop moving the mouse and the interface fades away after a few seconds; any input brings it back. Timing and speed are configurable.
- Pick the next clip at random, in order, or pin one by name.

### Video walls

- Build a rectangular frame from an allowed block (stone, stone bricks, quartz, glass, iron blocks and a few more by default), hold a stick, sneak, and right-click the two bottom corners of the frame. The inside becomes a screen.
- Sneak and right-click a finished wall with an empty hand to open its own editor: choose the clip, set its volume (0–200%), enable glow, restart it, clear the blocks inside (they are destroyed and drop as items), or delete the wall. Changes are sent to the server as you make them — there is no Save button.
- A wall can pin one clip or follow whatever the media library picked last.
- The picture fills the frame, so match the frame's proportions to the video if you do not want it stretched.
- Walls decode only when they are close and in view (48 blocks and 4 walls at once by default), pause when you look away, and resume as if they never stopped.
- Glow lights up nearby blocks with the brightness of the current frame. It is a client-side visual effect.
- Audio is limited to the nearest walls: distance falloff, a global gain, and only the closest 2 walls audible by default.
- Walls are stored in the world per dimension and synced to every player who has the mod. Structure blocks copy the wall together with the frame.

---

## Quick start

1. Install NarutoLoading. Naruto UI and Modern UI are required on the client and are installed with it by the CurseForge app.
2. Install FFmpeg (see **Requirements**) and make sure both `ffmpeg` and `ffprobe` are available.
3. Drop a video into `narutoloading/` inside your game directory, or import it from the settings screen. A video alone plays muted; add an audio file with the same name for sound.
4. Optional, for the very first splash: add `-javaagent:<full path to the NarutoLoading jar>` to your JVM arguments (instance settings → JVM arguments in most launchers; quote the path if it contains spaces). Without it, the early splash stays vanilla and the video starts at the main menu.
5. Open the settings with **F12** or **Mods → NarutoLoading → Config**, and press **Ctrl+V** anywhere without a text field focused to open the download card with the clipboard link.

---

## The media library

The loading screen and the world walls share one folder of media. By default it is `narutoloading/` next to your `mods` folder; the path can be changed, and a relative path is resolved against the game directory.

- **One name, one entry.** `naruto.mp4` and `naruto.m4a` are paired automatically into a single entry with picture and sound. A video without a partner plays muted. A separate audio file is always optional.
- **Formats.** Video: `mp4`, `mkv`, `webm`, `mov`, `m4v`, `avi`, `flv`. Audio: `ogg`, `opus`, `mp3`, `flac`, `wav`, `m4a`, `aac`. What actually decodes depends on your FFmpeg build; the common ones are fine.
- **Import.** “Import a local file…” copies a video or audio file into the folder and selects it.
- **Download.** Paste a link into the download card and yt-dlp fetches it for you — video and audio as separate buttons. The video is saved as `<name>.<ext>` and the audio as `<name>.audio.<ext>`, and the two are paired into one entry when the download finishes. One download at a time; playlists are ignored.
- **No media is bundled.** Bring your own files.

---

## Settings

Everything is in one screen, grouped by what it affects:

- **Media** — the shared library, import, download, folder, and how the next clip is chosen.
- **Loading screen** — max resolution, volume, mute vanilla music, idle fade.
- **World walls** — the selected wall, its clip and volume, glow, plus rendering, performance and audio defaults.
- **General** — theme, shimmer, FFmpeg and yt-dlp status, debug timing, reset.

Settings save as you change them, with an Undo button in the bottom bar. No Apply button and no save prompt.

---

## Commands

Server commands need permission level 2. The client-side `/narutoloading wall debug` prints the active walls and their buffered frames.

| Command | What it does |
| --- | --- |
| `/narutoloading wall list` | List every wall in this dimension |
| `/narutoloading wall info <pos>` | Show one wall's corners, clip, volume and size |
| `/narutoloading wall add <from> <to> [media]` | Create a wall from two corners, optionally with a clip |
| `/narutoloading wall remove <pos>` | Delete the wall definition (frame blocks stay) |
| `/narutoloading wall clear` | Delete every wall in this dimension |

---

## Requirements

- **FFmpeg and ffprobe** — required. They decode the picture and read duration and resolution. Both binaries must come from the same build and sit in one folder. The mod looks in your `PATH`, the game folder (up to four levels deep), and a folder you pick in General; press “Find again” after moving them.
- **yt-dlp** — optional, only for downloading from a link. It is a single executable with no installer: download it from the official release page, choose it in the mod, and the file is copied into your game folder.
- **Modern UI + Naruto UI** — required on the client. Naruto UI provides the settings screen and pulls Modern UI with it. On a dedicated server neither is needed.

---

## Performance

Frames are decoded by FFmpeg on the CPU and converted to a texture on the GPU. 1080p is cheap; 4K costs real CPU time, especially at 60 fps.

- The loading screen defaults to **1920×1080**; the settings screen allows up to **3840×2160**. Larger sources are scaled down, keeping the aspect ratio.
- Walls default to **1280×720**, up to **3840×2160** in the settings screen. Each active wall decodes its own stream, so resolution matters more when several are in view.
- If the game stutters, lower the resolution first, then “Walls decoding at once”, then the sleep distance.

---

## Compatibility and notes

- **Client and server.** The loading screens are client-side only. Walls need the mod on the server; installing it only on the client leaves the Walls page empty. Clients without the mod simply do not see the walls.
- **Everyone needs the file.** The server syncs only the media *name*. Every player who should see a wall needs a file with that name in their own media folder; otherwise the frame stays but the picture does not appear.
- **Shader packs.** Walls are drawn through a normal translucent entity layer, so shader packs composite them like other translucent blocks.
- **Embeddium.** Hidden faces behind a wall are also culled through Embeddium when it is installed.
- **Duplicationless (optional).** When it is installed, breaking a block in a wall's frame removes the wall immediately. Without it, the server notices on its next border check — a couple of seconds later for a loaded area.
- **Light mods.** Glow works by adding light emission to blocks. If another mod already overrides block light emission, the switch is greyed out and the mod says so in the log.
- **Structure blocks.** Saving a structure that contains a wall saves the wall too; placing the structure recreates it, clip name included.
- **Server owners.** Wall building and editing are open to players by default; only the commands need permission level 2. You can restrict which blocks count as a frame, the builder item, the maximum side and area, and the edit distance in `config/narutoloading-server.properties`.

---

## FAQ

**Q: FFmpeg is not found.**

A: Check that `ffmpeg` and `ffprobe` are in the same folder and from the same build, then press “Find again” in General. If you use a custom folder, it must be an absolute path.

**Q: The picture is black or transparent.**

A: Both the loading screen and the walls always render through a YUV shader. If the picture stays black, check FFmpeg and your GPU driver, then look for a shader compile error in the log.

**Q: There is no sound.**

A: A video file with an audio track plays its own sound; a video without one needs a same-named audio file. Only mono and stereo tracks are supported — 5.1 and other multi-channel files are ignored. Also check the volume slider and that the file is not muted by the video itself.

**Q: Other players cannot see my wall.**

A: They need the mod and a file with the same name in their own media folder. The server only sends the name, not the video.

**Q: The first splash is still the vanilla one.**

A: That part needs the `-javaagent` JVM argument from the Quick start. Everything else works without it.

**Q: Glow is greyed out.**

A: Another mod already controls block light emission, so the switch cannot take effect. The rest of the wall still works.

**Q: Walls are choppy.**

A: Lower the wall resolution, reduce “Walls decoding at once”, or shorten the sleep distance. A 4K60 clip is much heavier than 720p30.

---

## A note on media

NarutoLoading ships no videos and downloads nothing on its own. Everything plays from files on your disk; the only network access is yt-dlp, and only when you press a download button. Please respect the rights of the people who made the clips you use, and do not redistribute them with modpacks.

---

## Credits

- Authors: Kasualix, DeepSeek-V4.1-Flash
- License: LGPL-3.0
- Requires FFmpeg; the settings screen uses Modern UI and Naruto UI.
