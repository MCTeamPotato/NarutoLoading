package me.kall.narutoloading.media;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL32C.*;

/**
 * Draws a yuv420p frame as a full-screen quad, using the viewport that is already current.
 * <p>
 * The early window renders into a vertically flipped framebuffer, so {@code flipY} is true there;
 * the game's main framebuffer needs {@code flipY == false}.
 */
public final class YuvPresenter implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 150 core

            in vec2 Position;
            uniform float uFlipY;
            out vec2 vTex;

            void main() {
                vTex = vec2(Position.x * 0.5 + 0.5, mix(0.5 - Position.y * 0.5, 0.5 + Position.y * 0.5, uFlipY));
                gl_Position = vec4(Position, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 150 core

            uniform sampler2D uTexY;
            uniform sampler2D uTexU;
            uniform sampler2D uTexV;
            in vec2 vTex;
            out vec4 fragColor;

            void main() {
                float y = texture(uTexY, vTex).r;
                float u = texture(uTexU, vTex).r;
                float v = texture(uTexV, vTex).r;

                float c = y - 16.0 / 255.0;
                float d = u - 128.0 / 255.0;
                float e = v - 128.0 / 255.0;

                float r = 1.164 * c + 1.596 * e;
                float g = 1.164 * c - 0.392 * d - 0.813 * e;
                float b = 1.164 * c + 2.017 * d;

                fragColor = vec4(clamp(vec3(r, g, b), 0.0, 1.0), 1.0);
            }
            """;

    private final boolean flipY;

    private int program = -1;
    private int vao;
    private int vbo;
    private int[] textures;
    private int[] pboArray;
    private int flipUniform;

    private int width;
    private int height;
    private long frameCount;

    public YuvPresenter(boolean flipY) {
        this.flipY = flipY;
    }

    public void setup(int width, int height) {
        if (this.program != -1 && this.width == width && this.height == height) return;

        this.close();

        this.width = width;
        this.height = height;
        this.frameCount = 0L;

        this.program = createProgram();
        this.flipUniform = glGetUniformLocation(this.program, "uFlipY");

        int[] textures = new int[3];
        glGenTextures(textures);
        createTexture(textures[0], width, height);
        createTexture(textures[1], width / 2, height / 2);
        createTexture(textures[2], width / 2, height / 2);
        glBindTexture(GL_TEXTURE_2D, 0);
        this.textures = textures;

        int[] pbos = new int[6];
        glGenBuffers(pbos);
        long ySize = (long) width * height;
        long uvSize = (long) (width / 2) * (height / 2);
        for (int i = 0; i < 2; i++) {
            allocateBuffer(pbos[i], ySize);
            allocateBuffer(pbos[2 + i], uvSize);
            allocateBuffer(pbos[4 + i], uvSize);
        }
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        this.pboArray = pbos;

        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        glBindVertexArray(this.vao);
        glBindBuffer(GL_ARRAY_BUFFER, this.vbo);

        float[] vertices = {-1F, -1F, 1F, -1F, -1F, 1F, 1F, 1F};
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        try {
            buffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void upload(ByteBuffer frame) {
        if (this.program == -1 || frame == null) return;

        int ySize = this.width * this.height;
        int uvSize = (this.width / 2) * (this.height / 2);

        int slot = (int) (this.frameCount & 1L);
        int uploadSlot = this.frameCount == 0L ? slot : 1 - slot;

        int previousAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        int previousBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        stage(this.pboArray[slot], frame, 0, ySize);
        stage(this.pboArray[2 + slot], frame, ySize, uvSize);
        stage(this.pboArray[4 + slot], frame, ySize + uvSize, uvSize);

        uploadTexture(this.textures[0], this.width, this.height, this.pboArray[uploadSlot]);
        uploadTexture(this.textures[1], this.width / 2, this.height / 2, this.pboArray[2 + uploadSlot]);
        uploadTexture(this.textures[2], this.width / 2, this.height / 2, this.pboArray[4 + uploadSlot]);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, previousBuffer);
        glPixelStorei(GL_UNPACK_ALIGNMENT, previousAlignment);
        glBindTexture(GL_TEXTURE_2D, 0);

        this.frameCount++;
    }

    public void present() {
        this.present(this.flipY);
    }

    public void present(boolean flip) {
        if (this.program == -1 || this.frameCount == 0L) return;

        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int previousActive = glGetInteger(GL_ACTIVE_TEXTURE);

        glActiveTexture(GL_TEXTURE0);
        int previousTex0 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE1);
        int previousTex1 = glGetInteger(GL_TEXTURE_BINDING_2D);
        glActiveTexture(GL_TEXTURE2);
        int previousTex2 = glGetInteger(GL_TEXTURE_BINDING_2D);

        boolean blend = glIsEnabled(GL_BLEND);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);

        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_SCISSOR_TEST);
        glDepthMask(false);

        glUseProgram(this.program);
        glUniform1f(this.flipUniform, flip ? 1F : 0F);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, this.textures[0]);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, this.textures[1]);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, this.textures[2]);

        glBindVertexArray(this.vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, previousTex0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, previousTex1);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, previousTex2);
        glActiveTexture(previousActive);

        glBindVertexArray(previousVao);
        glUseProgram(previousProgram);

        glDepthMask(depthMask);
        if (cullFace) glEnable(GL_CULL_FACE);
        if (scissor) glEnable(GL_SCISSOR_TEST);
        if (depthTest) glEnable(GL_DEPTH_TEST);
        if (blend) glEnable(GL_BLEND);
    }

    @Override
    public void close() {
        if (this.textures != null) {
            glDeleteTextures(this.textures);
            this.textures = null;
        }

        if (this.pboArray != null) {
            glDeleteBuffers(this.pboArray);
            this.pboArray = null;
        }

        if (this.vbo != 0) {
            glDeleteBuffers(this.vbo);
            this.vbo = 0;
        }

        if (this.vao != 0) {
            glDeleteVertexArrays(this.vao);
            this.vao = 0;
        }

        if (this.program != -1) {
            glDeleteProgram(this.program);
            this.program = -1;
        }

        this.frameCount = 0L;
    }

    private static int createProgram() {
        int vertex = compileShader(GL_VERTEX_SHADER, VERTEX_SOURCE);
        int fragment = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);

        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glBindAttribLocation(program, 0, "Position");
        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new IllegalStateException("YUV program link failed: " + log);
        }

        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "uTexY"), 0);
        glUniform1i(glGetUniformLocation(program, "uTexU"), 1);
        glUniform1i(glGetUniformLocation(program, "uTexV"), 2);
        glUseProgram(0);

        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("YUV shader compile failed: " + log);
        }

        return shader;
    }

    private static void createTexture(int texture, int width, int height) {
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private static void allocateBuffer(int pbo, long size) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, size, GL_STREAM_DRAW);
    }

    private static void stage(int pbo, ByteBuffer frame, int offset, int length) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);

        ByteBuffer mapped = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0L, length, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
        if (mapped == null) return;

        MemoryUtil.memCopy(MemoryUtil.memSlice(frame, offset, length), mapped);
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
    }

    private static void uploadTexture(int texture, int width, int height, int pbo) {
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RED, GL_UNSIGNED_BYTE, 0L);
    }
}
