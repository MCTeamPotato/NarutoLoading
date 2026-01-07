# Setup Guide
## Knowledge
- We will auto-download ffmpeg and yt-dlp for you. So unless your Minecraft or pc cannot link to GitHub, there is no need to manually download them.
- We will create resourcepacks using your selected audio source if you enable "Local Sound" in the in-world setting screen, so if you invalidate the resourcepack that our audio player is using in your resourcepack selection gui, things may not work well.
## Keys
| Key                              | Action                                    | Type                                     |
|----------------------------------|-------------------------------------------|------------------------------------------|
| <code>F12</code>                 | Reload current video                      | No-world only                            |
| <code>Shift + F12</code>         | Roll random source                        | No-world only                            |
| <code>Ctrl + F12</code>          | Open source selection GUI                 | No-world only                            |
| <code>Shift + Right Mouse</code> | Clear text in focused edit box (in GUI)   | Universal for both no-world and in-world |
## Single Video & Audio
1. Put the video and audio files into the config directory of your game.
2. Edit the narutoloading.json and adjust the videoFileName and audioFileName values to the files' names.
3. Launch your game and enjoy!
## Multiple Videos & Audios
1. Create a directory called "narutoloading-sources" in the config folder of your game
2. Create sub-directories (name them as you like) in the "narutoloading-sources"
3. Each sub-directory contains a video file and audio file, and their names must be "video"/"audio".
4. Launch your game and enjoy!
## URL Support
1. Adjust the enableUrlForSourceSelection config option to true
2. Then we will automatically download yt-dlp during the launch of your game and you can enter URL in the Video and Audio edit boxes in our source selection screens.
## In-World Screen Construction
Built with Displayer blocks.  Width: 16/32/48/.../1600.  Height: 9/18/27/.../900. But Minecraft building height limit exists. It is MC's limit lol, not us.

### Commands for Building a Huge Screen Rapidly
Make sure your render distance and simulation distance is big enough (32 for example) before using these.
```
/gamerule commandModificationBlockLimit 9999999
```
```
/fill ~ ~ ~ ~ ~224 ~399 narutoloading:displayer
```
After you complete the whole building of it, sneak and right click the two displayers at the bottom corners and we will detect the whole screen automatically based on these two corners.

Right click at any displayer to open its in-world setting gui.
