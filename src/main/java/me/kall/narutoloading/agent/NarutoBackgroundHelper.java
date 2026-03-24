package me.kall.narutoloading.agent;

import me.kall.narutoloading.common.LifetimeController;
import me.kall.narutoloading.common.env.BaseEnv;
import me.kall.narutoloading.common.env.ffmpeg.VideoArgReader;
import me.kall.narutoloading.common.executor.EarlyVideoExecutor;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL32C.*;

public final class NarutoBackgroundHelper {
    private static int prog    = 0;
    private static int vao     = 0;
    private static int vbo     = 0;
    private static int texture = 0;

    private static EarlyVideoExecutor videoExecutor = null;
    private static LifetimeController lifetime      = null;
    private static double fps                       = 0;
    private static int    texWidth                  = 0;
    private static int    texHeight                 = 0;

    private static boolean glInitialized = false;
    private static boolean failed        = false;

    private static final String VERT_SRC = String.join("\n",
            "#version 330 core",
            "layout(location = 0) in vec2 aPos;",
            "out vec2 vUv;",
            "void main() {",
            "    vUv         = aPos * 0.5 + 0.5;",
            "    vUv.y       = 1.0 - vUv.y;",
            "    gl_Position = vec4(aPos, 0.0, 1.0);",
            "}"
    );

    private static final String FRAG_SRC = String.join("\n",
            "#version 330 core",
            "in  vec2      vUv;",
            "out vec4      fragColor;",
            "uniform sampler2D uTex;",
            "void main() {",
            "    fragColor = texture(uTex, vUv);",
            "}"
    );

    private NarutoBackgroundHelper() {}

    public static void render() {
        if (failed) {
            throw new RuntimeException("[NarutoBackgroundHelper] Already failed, skipping");
        }

        if (!BaseEnv.available()) {
            BaseEnv.setupEnv(true);
        }

        if (!glInitialized) {
            System.out.println("[NarutoBackgroundHelper] initGL...");
            initGL();
            if (failed) {
                throw new RuntimeException("[NarutoBackgroundHelper] initGL failed");
            }
        }

        ensureVideoExecutor();
        if (videoExecutor == null || lifetime == null) {
            throw new RuntimeException("[NarutoBackgroundHelper] videoExecutor or lifetime is null");
        }

        lifetime.lagSpikeRestart();
        lifetime.endRestart();

        if (lifetime.shouldUpdateFrame(fps)) {
            ByteBuffer frame = videoExecutor.fetchFrame(lifetime.elapsedSeconds());
            if (frame != null) {
                glBindTexture(GL_TEXTURE_2D, texture);

                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

                int expectedSize = texWidth * texHeight * 3;
                if (frame.remaining() >= expectedSize) {
                    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, texWidth, texHeight, GL_RGB, GL_UNSIGNED_BYTE, frame);
                } else {
                    throw new RuntimeException("[NarutoLoading] Frame buffer too small! Expected: " + expectedSize + " Got: " + frame.remaining());
                }

                glBindTexture(GL_TEXTURE_2D, 0);
            }
        }

        int prevProg = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao  = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevTex  = glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean wasBlend = glIsEnabled(GL_BLEND);

        glDisable(GL_BLEND);
        glUseProgram(prog);
        glUniform1i(glGetUniformLocation(prog, "uTex"), 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindVertexArray(prevVao);
        glBindTexture(GL_TEXTURE_2D, prevTex);
        glUseProgram(prevProg);
        if (wasBlend) glEnable(GL_BLEND);
    }

    private static void initGL() {
        try {
            prog    = buildShaderProgram();
            vao     = buildQuadVao();
            texture = glGenTextures();
            glInitialized = true;
        } catch (Exception e) {
            cleanupGL();
            failed = true;
            throw new RuntimeException(e);
        }
    }

    private static void ensureVideoExecutor() {
        if (videoExecutor != null) return;
        try {
            String videoPath  = BaseEnv.getNarutoConfig().absoluteVideoPath;
            String ffprobePath = BaseEnv.getFfmpegProvider().absoluteFFprobe;
            String ffmpegPath  = BaseEnv.getFfmpegProvider().absoluteFFmpeg;

            if (videoPath == null  || videoPath.isBlank())  return;
            if (ffprobePath == null || ffprobePath.isBlank()) return;
            if (ffmpegPath == null  || ffmpegPath.isBlank())  return;

            VideoArgReader reader = new VideoArgReader(videoPath, ffprobePath);
            fps       = reader.fps();
            long duration = reader.duration();

            texWidth  = BaseEnv.getNarutoConfig().width();
            texHeight = BaseEnv.getNarutoConfig().height();

            if (texWidth <= 0 || texHeight <= 0) return;

            allocTexture(texWidth, texHeight);

            final String fVideoPath  = videoPath;
            final String fFfmpegPath = ffmpegPath;
            final int    fWidth      = texWidth;
            final int    fHeight     = texHeight;
            final double fFps        = fps;

            videoExecutor = new EarlyVideoExecutor(
                    () -> () -> {
                        if (lifetime != null) lifetime.lagSpikeDetected = true;
                    },
                    () -> fFfmpegPath,
                    () -> fVideoPath,
                    () -> fWidth,
                    () -> fHeight,
                    () -> fFps,
                    () -> BaseEnv.getNarutoConfig().bufferSize,
                    () -> BaseEnv.getNarutoConfig().debug
            );

            lifetime = new LifetimeController(
                    duration,
                    System.nanoTime(),
                    () -> () -> {
                        BaseEnv.setupEnv(true);
                        restartExecutor();
                    },
                    () -> (elapsedSeconds) -> {
                        if (videoExecutor != null) {
                            videoExecutor.shutdown();
                            videoExecutor.setup(elapsedSeconds);
                        }
                    },
                    () -> false
            );

            videoExecutor.setup();
            lifetime.start();
        } catch (Exception e) {
            videoExecutor = null;
            lifetime      = null;
            throw new RuntimeException(e);
        }
    }

    private static void restartExecutor() {
        if (videoExecutor != null) {
            videoExecutor.shutdown();
            videoExecutor = null;
        }
        lifetime = null;
    }

    private static void allocTexture(int w, int h) {
        glBindTexture(GL_TEXTURE_2D, texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private static int buildShaderProgram() {
        int vert = compileShader(GL_VERTEX_SHADER,   VERT_SRC);
        int frag = compileShader(GL_FRAGMENT_SHADER, FRAG_SRC);

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

    private static int compileShader(int type, String src) {
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

    private static int buildQuadVao() {
        float[] verts = {-1f, -1f,  1f, -1f,  -1f, 1f,  1f, 1f};

        int quadVao = glGenVertexArrays();
        int quadVbo = glGenBuffers();
        vbo = quadVbo;

        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);

        FloatBuffer buf = MemoryUtil.memAllocFloat(verts.length);
        try {
            buf.put(verts).flip();
            glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buf);
        }

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        return quadVao;
    }

    private static void cleanupGL() {
        if (texture != 0) { glDeleteTextures(texture); texture = 0; }
        if (vbo     != 0) { glDeleteBuffers(vbo);       vbo     = 0; }
        if (vao     != 0) { glDeleteVertexArrays(vao);  vao     = 0; }
        if (prog    != 0) { glDeleteProgram(prog);       prog    = 0; }
    }
}