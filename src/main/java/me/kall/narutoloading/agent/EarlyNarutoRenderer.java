package me.kall.narutoloading.agent;

import me.kall.narutoloading.core.NarutoTV;
import me.kall.narutoloading.core.executor.RestartExecutor;
import me.kall.narutoloading.core.executor.audio.EarlyAudioExecutor;
import me.kall.narutoloading.core.executor.video.EarlyVideoExecutor;
import me.kall.narutoloading.data.NarutoConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL32C.*;

public final class EarlyNarutoRenderer extends NarutoTV<ByteBuffer, Integer, Integer> {
    private static @Nullable EarlyNarutoRenderer INSTANCE = new EarlyNarutoRenderer();

    private int program = 0;
    private int vertexArray = 0;
    private int buffer = 0;

    @SuppressWarnings("unused")
    public static void render() {
        if (INSTANCE == null) return;
        INSTANCE.renderFrame();
    }

    @SuppressWarnings("unused")
    public static void restart(String seconds) {
        if (INSTANCE == null) return;
        INSTANCE.restartAt(seconds);
    }

    @SuppressWarnings("unused")
    public static void shutdown() {
        if (INSTANCE == null) return;
        INSTANCE.cleanup();
        INSTANCE = null;
    }

    private static final String VERT_SOURCE = String.join("\n",
            "#version 330 core",
            "layout(location = 0) in vec2 aPos;",
            "out vec2 vUv;",
            "void main() {",
            "    vUv         = aPos * 0.5 + 0.5;",
            "    gl_Position = vec4(aPos, 0.0, 1.0);",
            "}"
    );

    private static final String FRAG_SOURCE = String.join("\n",
            "#version 330 core",
            "in  vec2      vUv;",
            "out vec4      fragColor;",
            "uniform sampler2D uTex;",
            "void main() {",
            "    fragColor = texture(uTex, vUv);",
            "}"
    );

    @Override
    public boolean isRunnable() {
        return true;
    }

    @Override
    public void createVideo() {
        Runnable onLagSpike = () -> {
            if (this.lifetime != null) this.lifetime.lagSpikeDetected = true;
        };

        this.videoExecutor = new EarlyVideoExecutor(() -> onLagSpike, this.absoluteVideoPath(), () -> NarutoConfig.WIDTH, () -> NarutoConfig.HEIGHT, () -> this.fps);
    }

    @Override
    public void createAudio() {
        this.audioExecutor = new EarlyAudioExecutor(() -> () -> RestartExecutor.schedule(this::cleanup, this::init), this.absoluteVideoPath(), this.absoluteAudioPath());
    }

    @Override
    public void createTexture() {
        if (this.program == 0) {
            this.program = this.buildShaderProgram();
            this.vertexArray = this.buildQuadVao();
        }

        this.texture = glGenTextures();
        this.textureLocation = this.texture;

        glBindTexture(GL_TEXTURE_2D, this.texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, NarutoConfig.WIDTH, NarutoConfig.HEIGHT, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private int buildShaderProgram() {
        int vert = this.compileShader(GL_VERTEX_SHADER, VERT_SOURCE);
        int frag = this.compileShader(GL_FRAGMENT_SHADER, FRAG_SOURCE);

        int program = glCreateProgram();
        glAttachShader(program, vert);
        glAttachShader(program, frag);
        glLinkProgram(program);
        glDeleteShader(vert);
        glDeleteShader(frag);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Shader link failed: " + log);
        }
        return program;
    }

    private int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            throw new RuntimeException("Shader compile failed: " + log);
        }
        return id;
    }

    private int buildQuadVao() {
        float[] vertices = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};

        int quadVao = glGenVertexArrays();
        int quadVbo = glGenBuffers();
        this.buffer = quadVbo;

        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);

        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
        try {
            verticesBuffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(verticesBuffer);
        }

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        return quadVao;
    }

    @Override
    public void consumeFrame(@NotNull ByteBuffer frame, Integer texture) {
        glBindTexture(GL_TEXTURE_2D, texture);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        int expectedSize = NarutoConfig.WIDTH * NarutoConfig.HEIGHT * 3;
        if (frame.remaining() >= expectedSize) {
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, NarutoConfig.WIDTH, NarutoConfig.HEIGHT, GL_RGB, GL_UNSIGNED_BYTE, frame);
        } else {
            throw new RuntimeException("[NarutoLoading] Frame buffer too small! Expected: " + expectedSize + " Got: " + frame.remaining());
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void renderFrame(Integer textureLocation) {
        if (textureLocation == null) return;

        int prevProg = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean wasBlend = glIsEnabled(GL_BLEND);

        glDisable(GL_BLEND);
        glUseProgram(this.program);
        glUniform1i(glGetUniformLocation(this.program, "uTex"), 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureLocation);
        glBindVertexArray(this.vertexArray);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindVertexArray(prevVao);
        glBindTexture(GL_TEXTURE_2D, prevTex);
        glUseProgram(prevProg);
        if (wasBlend) glEnable(GL_BLEND);
    }

    @Override
    public void cleanupTexture() {
        if (this.texture != null) {
            glDeleteTextures(this.texture);
            this.texture = null;
        }

        if (this.buffer != 0) {
            glDeleteBuffers(this.buffer);
            this.buffer = 0;
        }

        if (this.vertexArray != 0) {
            glDeleteVertexArrays(this.vertexArray);
            this.vertexArray = 0;
        }

        if (this.program != 0) {
            glDeleteProgram(this.program);
            this.program = 0;
        }

        this.textureLocation = null;
    }
}