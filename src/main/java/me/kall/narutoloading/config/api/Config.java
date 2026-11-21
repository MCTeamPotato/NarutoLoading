package me.kall.narutoloading.config.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

/**
 * 零依赖的 .properties 配置库，支持链式调用，并与 System 属性双向同步。
 *
 * <p>只使用 JDK 8 的基础 API（无 lambda / stream / 日志框架），
 * 因此可以在 Java Agent 的 premain 阶段安全使用。</p>
 *
 * <pre>{@code
 * Config cfg = Config.builder()
 *         .file("config/app.properties")
 *         .prefix("app.")
 *         .defaultValue("port", 8080)
 *         .defaultValue("debug", false)
 *         .autoCreate(true)
 *         .build();
 *
 * int port = cfg.getInt("port");
 * cfg.set("debug", true).set("name", "demo").save();
 * }</pre>
 *
 * <h3>读取优先级</h3>
 * 开启同步时：System 属性（如 -Dapp.port=1） &gt; 配置文件 &gt; 默认值。
 * 调用 {@link Builder#fileOverridesSystem(boolean)} 可让文件值在加载时覆盖已有的 System 属性。
 *
 * <h3>同步规则</h3>
 * <ul>
 *   <li>加载时：文件/默认值中的每个键会写入 System 属性（键为 prefix + key）。</li>
 *   <li>读取时：优先读 System 属性，所以其他代码调用 System.setProperty 也能立即生效。</li>
 *   <li>set / remove 时：同时更新内部存储与 System 属性。</li>
 *   <li>save 只保存内部存储，不会把 -D 临时覆盖值写入文件。</li>
 * </ul>
 */
public final class Config {
    private final Path file;
    private final String prefix;
    private final boolean syncSystem;
    private final boolean fileOverridesSystem;
    private final boolean autoCreate;
    private final String header;
    private final Map<String, String> defaults;
    private final Map<String, String> values = new LinkedHashMap<>();

    private Config(@NotNull Builder builder) {
        this.file = builder.file;
        this.prefix = builder.prefix;
        this.syncSystem = builder.syncSystem;
        this.fileOverridesSystem = builder.fileOverridesSystem;
        this.autoCreate = builder.autoCreate;
        this.header = builder.header;
        this.defaults = new LinkedHashMap<>(builder.defaults);
    }

    /** 创建一个新的配置构建器。 */
    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * 从配置文件加载值，补入缺失的默认值，并按配置同步 System 属性。
     *
     * @return 当前配置实例
     * @throws UncheckedIOException 配置文件读取失败时抛出
     */
    public synchronized Config load() {
        boolean fileExisted = this.file != null && Files.isRegularFile(this.file);
        this.values.clear();

        if (fileExisted) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read config file: " + this.file, e);
            }
            List<String> keys = new ArrayList<>(p.stringPropertyNames());
            Collections.sort(keys);
            for (String k : keys) {
                this.values.put(k, p.getProperty(k));
            }
        }

        for (Map.Entry<String, String> e : this.defaults.entrySet()) {
            if (!this.values.containsKey(e.getKey())) {
                this.values.put(e.getKey(), e.getValue());
            }
        }

        if (this.syncSystem) {
            for (Map.Entry<String, String> e : this.values.entrySet()) {
                if (this.fileOverridesSystem || this.readSys(e.getKey()) == null) {
                    this.writeSys(e.getKey(), e.getValue());
                }
            }
        }

        return !fileExisted && this.autoCreate && this.file != null ? this.save() : this;
    }

    /**
     * 将内部存储的值保存到配置文件。
     *
     * @return 当前配置实例
     * @throws IllegalStateException 未配置文件路径时抛出
     * @throws UncheckedIOException 写入配置文件失败时抛出
     */
    public synchronized Config save() {
        if (this.file == null) {
            throw new IllegalStateException("No file configured; use Builder.file(...)");
        }
        StringBuilder out = new StringBuilder();
        if (this.header != null && !this.header.isEmpty()) {
            for (String line : this.header.split("\\r?\\n", -1)) {
                out.append("# ").append(line).append('\n');
            }
        }
        for (Map.Entry<String, String> e : this.values.entrySet()) {
            out.append(encodeEntry(e.getKey(), e.getValue()));
        }

        Path abs = this.file.toAbsolutePath();
        Path tmp = null;
        try {
            Path parent = abs.getParent();
            Files.createDirectories(parent);
            tmp = Files.createTempFile(parent, ".config", ".tmp");
            Files.writeString(tmp, out.toString());
            try {
                Files.move(tmp, abs, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, abs, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write config file: " + this.file, e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响结果
                }
            }
        }
        return this;
    }

    /** 借助 Properties.store 做转义，保证与标准格式完全兼容；去掉它自带的时间戳注释行。 */
    private static @NotNull String encodeEntry(String key, String value) {
        Properties tmp = new Properties();
        tmp.setProperty(key, value);
        StringWriter sw = new StringWriter();
        try {
            tmp.store(sw, null);
            BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
            br.readLine(); // 跳过 "#<date>"
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e); // StringWriter 不会真的抛出
        }
    }

    // ------------------------------------------------------------------ 写入（链式）

    /**
     * 设置配置值；对象会通过 {@link String#valueOf(Object)} 转为字符串，{@code null} 等同于移除键。
     *
     * @param key 配置键
     * @param value 配置值
     * @return 当前配置实例
     * @throws IllegalArgumentException 键为 {@code null} 或空字符串时抛出
     */
    public synchronized Config set(String key, Object value) {
        checkKey(key);
        if (value == null) {
            return this.remove(key);
        }
        String s = String.valueOf(value);
        this.values.put(key, s);
        if (this.syncSystem) {
            this.writeSys(key, s);
        }
        return this;
    }

    /**
     * 仅当键不存在时设置配置值。
     *
     * @param key 配置键
     * @param value 配置值
     * @return 当前配置实例
     */
    public synchronized Config setIfAbsent(String key, Object value) {
        return this.has(key) ? this : this.set(key, value);
    }

    /**
     * 移除配置键及其对应的 System 属性。
     *
     * @param key 配置键
     * @return 当前配置实例
     * @throws IllegalArgumentException 键为 {@code null} 或空字符串时抛出
     */
    public synchronized Config remove(String key) {
        checkKey(key);
        this.values.remove(key);
        if (this.syncSystem) {
            try {
                System.clearProperty(this.prefix + key);
            } catch (SecurityException ignored) {
                // 无权限则忽略
            }
        }
        return this;
    }

    // ------------------------------------------------------------------ 读取

    /**
     * 检查配置中是否存在指定键。
     *
     * @param key 配置键
     * @return 键存在时为 {@code true}
     */
    public synchronized boolean has(String key) {
        return this.getString(key) != null;
    }

    /**
     * 获取字符串值；若开启 System 属性同步，则优先返回对应的 System 属性。
     *
     * @param key 配置键
     * @return 配置值，不存在时返回 {@code null}
     */
    public synchronized String getString(String key) {
        checkKey(key);
        if (this.syncSystem) {
            String s = this.readSys(key);
            if (s != null) {
                return s;
            }
        }
        return this.values.get(key);
    }

    /**
     * 获取字符串值；值不存在时返回指定默认值。
     *
     * @param key 配置键
     * @param def 值不存在时返回的默认值
     * @return 配置值或默认值
     */
    public String getString(String key, String def) {
        String v = this.getString(key);
        return v != null ? v : def;
    }

    /**
     * 获取整数值；缺失或格式错误时返回指定默认值。
     *
     * @param key 配置键
     * @param def 缺失或格式错误时返回的默认值
     * @return 解析出的整数或默认值
     */
    public int getInt(String key, int def) {
        String v = this.trimmed(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 获取长整数值；缺失或格式错误时返回指定默认值。
     *
     * @param key 配置键
     * @param def 缺失或格式错误时返回的默认值
     * @return 解析出的长整数或默认值
     */
    public long getLong(String key, long def) {
        String v = this.trimmed(key);
        if (v == null) {
            return def;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 获取双精度浮点值；缺失或格式错误时返回指定默认值。
     *
     * @param key 配置键
     * @param def 缺失或格式错误时返回的默认值
     * @return 解析出的浮点值或默认值
     */
    public double getDouble(String key, double def) {
        String v = this.trimmed(key);
        if (v == null) {
            return def;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 获取布尔值，接受 true/false、yes/no、on/off、1/0（不区分大小写）。
     *
     * @param key 配置键
     * @param def 缺失或格式错误时返回的默认值
     * @return 解析出的布尔值或默认值
     */
    public boolean getBoolean(String key, boolean def) {
        Boolean b = parseBoolean(this.trimmed(key));
        return b != null ? b : def;
    }

    /**
     * 获取必需的字符串值。
     *
     * @param key 配置键
     * @return 配置值
     * @throws NoSuchElementException 键不存在时抛出
     */
    public @NotNull String requireString(String key) {
        String v = this.getString(key);
        if (v == null) {
            throw new NoSuchElementException("Missing config key: " + key);
        }
        return v;
    }

    /**
     * 获取必需的整数值。
     *
     * @param key 配置键
     * @return 解析出的整数
     * @throws NoSuchElementException 键不存在时抛出
     * @throws IllegalStateException 值不是有效整数时抛出
     */
    public int getInt(String key) {
        String v = this.requireString(key).trim();
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config '" + key + "' is not an int: " + v, e);
        }
    }

    /**
     * 获取必需的长整数值。
     *
     * @param key 配置键
     * @return 解析出的长整数
     * @throws NoSuchElementException 键不存在时抛出
     * @throws IllegalStateException 值不是有效长整数时抛出
     */
    public long getLong(String key) {
        String v = this.requireString(key).trim();
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config '" + key + "' is not a long: " + v, e);
        }
    }

    /**
     * 获取必需的双精度浮点值。
     *
     * @param key 配置键
     * @return 解析出的浮点值
     * @throws NoSuchElementException 键不存在时抛出
     * @throws IllegalStateException 值不是有效浮点数时抛出
     */
    public double getDouble(String key) {
        String v = this.requireString(key).trim();
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config '" + key + "' is not a double: " + v, e);
        }
    }

    /**
     * 获取必需的布尔值，接受 true/false、yes/no、on/off、1/0（不区分大小写）。
     *
     * @throws IllegalStateException 值不是有效布尔值时抛出
     */
    public boolean getBoolean(String key) {
        String v = this.requireString(key).trim();
        Boolean b = parseBoolean(v);
        if (b == null) {
            throw new IllegalStateException("Config '" + key + "' is not a boolean: " + v);
        }
        return b;
    }

    /** 获取当前内部存储的所有键的不可修改副本，按插入顺序排列。 */
    public synchronized @NotNull @UnmodifiableView Set<String> keys() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.values.keySet()));
    }

    /** 获取当前内部存储的独立 {@link Properties} 副本。 */
    public synchronized @NotNull Properties toProperties() {
        Properties p = new Properties();
        for (Map.Entry<String, String> e : this.values.entrySet()) {
            p.setProperty(e.getKey(), e.getValue());
        }
        return p;
    }

    /** 获取配置文件路径；仅内存配置返回 {@code null}。 */
    public Path getFile() {
        return this.file;
    }

    // ------------------------------------------------------------------ 内部工具

    private @Nullable String trimmed(String key) {
        String v = this.getString(key);
        return v == null ? null : v.trim();
    }

    private static Boolean parseBoolean(String v) {
        if (v == null) {
            return null;
        }
        String s = v.toLowerCase(java.util.Locale.ROOT);
        if (s.equals("true") || s.equals("yes") || s.equals("on") || s.equals("1")) {
            return Boolean.TRUE;
        }
        if (s.equals("false") || s.equals("no") || s.equals("off") || s.equals("0")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private @Nullable String readSys(String key) {
        try {
            return System.getProperty(this.prefix + key);
        } catch (SecurityException e) {
            return null;
        }
    }

    private void writeSys(String key, String value) {
        try {
            System.setProperty(this.prefix + key, value);
        } catch (SecurityException ignored) {
            // 无权限则仅保留内部存储
        }
    }

    private static void checkKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Config key must not be null or empty");
        }
    }

    /** 返回包含配置文件、前缀和内部键集合的诊断字符串。 */
    @Override
    public synchronized String toString() {
        return "Config{file=" + this.file + ", prefix='" + this.prefix + "', keys=" + this.values.keySet() + '}';
    }

    // ------------------------------------------------------------------ Builder

    /**
     * 用于配置文件路径、默认值和 System 属性同步选项的构建器。
     *
     * <p>所有设置方法均返回当前构建器，以支持链式调用。</p>
     */
    public static final class Builder {
        private Path file;
        private String prefix = "";
        private boolean syncSystem = true;
        private boolean fileOverridesSystem = false;
        private boolean autoCreate = false;
        private String header;
        private final Map<String, String> defaults = new LinkedHashMap<>();

        private Builder() {}

        /**
         * 设置配置文件路径；相对路径相对于进程工作目录。
         *
         * @param path 文件路径；传入 {@code null} 表示仅在内存中工作
         * @return 当前构建器
         */
        public Builder file(String path) {
            this.file = path == null ? null : Paths.get(path);
            return this;
        }

        /**
         * 设置配置文件路径。
         *
         * @param path 文件路径；传入 {@code null} 表示仅在内存中工作
         * @return 当前构建器
         */
        public Builder file(Path path) {
            this.file = path;
            return this;
        }

        /**
         * 设置 System 属性键前缀；文件中的键不带此前缀，默认为空。
         *
         * @param prefix 属性前缀；传入 {@code null} 时使用空前缀
         * @return 当前构建器
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix == null ? "" : prefix;
            return this;
        }

        /**
         * 设置是否与 System 属性同步，默认为 {@code true}。
         *
         * @param sync 是否同步
         * @return 当前构建器
         */
        public Builder syncSystem(boolean sync) {
            this.syncSystem = sync;
            return this;
        }

        /**
         * 设置加载时文件值是否覆盖已存在的 System 属性（如 -D 参数），默认为 {@code false}。
         *
         * @param override 是否由文件值覆盖已有属性
         * @return 当前构建器
         */
        public Builder fileOverridesSystem(boolean override) {
            this.fileOverridesSystem = override;
            return this;
        }

        /**
         * 设置文件不存在时是否在加载后自动创建文件，默认为 {@code false}。
         *
         * @param create 是否自动创建
         * @return 当前构建器
         */
        public Builder autoCreate(boolean create) {
            this.autoCreate = create;
            return this;
        }

        /**
         * 设置保存时写在文件开头的注释，支持多行。
         *
         * @param header 注释内容；传入 {@code null} 表示不写注释
         * @return 当前构建器
         */
        public Builder header(String header) {
            this.header = header;
            return this;
        }

        /**
         * 注册配置默认值；对象会通过 {@link String#valueOf(Object)} 转为字符串。
         *
         * @param key 配置键
         * @param value 默认值；传入 {@code null} 时不注册默认值
         * @return 当前构建器
         * @throws IllegalArgumentException 键为 {@code null} 或空字符串时抛出
         */
        public Builder defaultValue(String key, Object value) {
            checkKey(key);
            if (value != null) {
                this.defaults.put(key, String.valueOf(value));
            }
            return this;
        }

        /**
         * 构建配置并立即加载文件及默认值。
         *
         * @return 已加载的配置实例
         * @throws UncheckedIOException 加载配置文件失败时抛出
         */
        public Config build() {
            return new Config(this).load();
        }
    }
}