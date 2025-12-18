# 1.3.1
Add SourceRoller to randomly select video and audio source in a source list.

You can use `Left Shift` + `F12` (yeah the F12 here is definitely the reloadKey in our config so you can customize it too) to trigger the random selection in game.

How to make it work:
1. You need rename your video and audio files to `video.mp4` and `audio.mp3`.
2. create a directory called `narutoloading-sources` in your game config directory.
3. Create subdirectories in this sources directory for each pair of video and audio. The names of subdirectories do not matter.
4. put the video and audio pair into each subdirectory.
5. We will still try to use the sound in video.mp4 if audio.mp3 is not present.

Example directory structure:
- config/narutoloading-sources/naruto/video.mp4 without audio.mp3 is valid
- config/narutoloading-sources/badapple/video.mp4 and config/narutoloading-sources/badapple/audio.mp3 are valid as a pair.