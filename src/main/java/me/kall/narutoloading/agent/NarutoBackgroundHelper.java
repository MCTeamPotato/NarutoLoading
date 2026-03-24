package me.kall.narutoloading.agent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL32C.*;

public final class NarutoBackgroundHelper {
    public static final String IMAGE_PATH_PROP = "naruto.bg.image";

    private static final Logger LOGGER = LogManager.getLogger(NarutoBackgroundHelper.class);

    private static int prog    = 0;
    private static int vao     = 0;
    private static int vbo     = 0;
    private static int texture = 0;

    private static boolean initialized = false;
    private static boolean failed      = false;

    private static final String VERT_SRC = String.join("\n",
            "#version 330 core",
            "layout(location = 0) in vec2 aPos;",
            "out vec2 vUv;",
            "void main() {",
            "    vUv        = aPos * 0.5 + 0.5;",
            "    vUv.y      = 1.0 - vUv.y;",
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
        if (failed) return;
        if (!initialized) {
            doInit();
            if (failed) return;
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

    private static void doInit() {
        String path = System.getProperty(IMAGE_PATH_PROP);
        if (path == null || path.isBlank()) {
            LOGGER.warn("[NarutoAgent] Background image path not set. "
                    + "Use -D{}=<path>", IMAGE_PATH_PROP);
            failed = true;
            return;
        }

        try {
            prog    = buildShaderProgram();
            texture = loadTexture(path);
            vao     = buildQuadVao();

            initialized = true;
            LOGGER.info("[NarutoAgent] Background image ready: {}", path);
        } catch (Exception e) {
            LOGGER.error("[NarutoAgent] Background init failed", e);
            cleanup();
            failed = true;
        }
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
            throw new RuntimeException("Shader program link failed: " + log);
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
        float[] verts = {-1f, -1f, 1f, -1f, -1f,  1f, 1f,  1f};

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

    private static int loadTexture(String path) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        ByteBuffer raw = MemoryUtil.memAlloc(fileBytes.length);
        try {
            raw.put(fileBytes).flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(false);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(raw, w, h, c, 4);
                if (pixels == null) {
                    throw new IOException("STBImage failed: " + STBImage.stbi_failure_reason());
                }
                try {
                    int tex = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, tex);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glBindTexture(GL_TEXTURE_2D, 0);
                    return tex;
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            }
        } finally {
            MemoryUtil.memFree(raw);
        }
    }

    private static void cleanup() {
        if (texture != 0) { glDeleteTextures(texture); texture = 0; }
        if (vbo     != 0) { glDeleteBuffers(vbo);       vbo     = 0; }
        if (vao     != 0) { glDeleteVertexArrays(vao);  vao     = 0; }
        if (prog    != 0) { glDeleteProgram(prog);       prog    = 0; }
    }
}