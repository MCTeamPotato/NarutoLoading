package me.kall.narutoloading.playback.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL32C.*;

public final class YuvTextures implements AutoCloseable {
    private final int width;
    private final int height;
    private final int[] textures = new int[3];
    private final int[] buffers = new int[6];

    private long frameCount;

    public YuvTextures(int width, int height) {
        this.width = width;
        this.height = height;

        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int previousBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);

        glGenTextures(this.textures);
        createTexture(this.textures[0], width, height);
        createTexture(this.textures[1], width / 2, height / 2);
        createTexture(this.textures[2], width / 2, height / 2);

        int ySize = width * height;
        int uvSize = (width / 2) * (height / 2);

        glGenBuffers(this.buffers);
        for (int i = 0; i < 2; i++) {
            allocate(this.buffers[i], ySize);
            allocate(this.buffers[2 + i], uvSize);
            allocate(this.buffers[4 + i], uvSize);
        }

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, previousBuffer);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    public boolean isEmpty() {
        return this.frameCount <= 0L;
    }

    public int texture(int plane) {
        return this.textures[plane];
    }

    public void upload(ByteBuffer frame) {
        int ySize = this.width * this.height;
        int uvSize = (this.width / 2) * (this.height / 2);

        int slot = (int) (this.frameCount & 1L);

        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int previousBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        int previousAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        int previousRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
        int previousSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS);
        int previousSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

        stage(this.buffers[slot], frame, 0, ySize);
        stage(this.buffers[2 + slot], frame, ySize, uvSize);
        stage(this.buffers[4 + slot], frame, ySize + uvSize, uvSize);

        uploadTexture(this.textures[0], this.width, this.height, this.buffers[slot]);
        uploadTexture(this.textures[1], this.width / 2, this.height / 2, this.buffers[2 + slot]);
        uploadTexture(this.textures[2], this.width / 2, this.height / 2, this.buffers[4 + slot]);

        glPixelStorei(GL_UNPACK_ALIGNMENT, previousAlignment);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, previousRowLength);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, previousSkipPixels);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, previousSkipRows);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, previousBuffer);
        glBindTexture(GL_TEXTURE_2D, previousTexture);

        this.frameCount++;
    }

    @Override
    public void close() {
        glDeleteTextures(this.textures);
        glDeleteBuffers(this.buffers);
        this.frameCount = 0L;
    }

    private static void createTexture(int texture, int width, int height) {
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private static void allocate(int buffer, long size) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, size, GL_STREAM_DRAW);
    }

    private static void stage(int buffer, ByteBuffer frame, int offset, int length) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer);

        ByteBuffer mapped = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0L, length, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
        if (mapped == null) return;

        MemoryUtil.memCopy(MemoryUtil.memSlice(frame, offset, length), mapped);
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
    }

    private static void uploadTexture(int texture, int width, int height, int buffer) {
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RED, GL_UNSIGNED_BYTE, 0L);
    }
}
