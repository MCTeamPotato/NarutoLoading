package me.kall.narutoloading.common.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonConfig {

    private final Path configPath;
    private final Map<String, Object> configMap = new LinkedHashMap<>();

    private JsonConfig(@NotNull Path configPath, String version) {
        this.configPath = configPath;
        this.put("Version", version);
    }

    @Contract("_, _ -> new")
    public static @NotNull JsonConfig create(Path configPath, String version) {
        return new JsonConfig(configPath, version);
    }

    @Contract("_, _ -> new")
    public static @NotNull JsonConfig create(String modID, String version) {
        return create(Path.of("config").resolve(modID + ".json"), version);
    }

    public JsonConfig initialize() {
        if (Files.exists(this.configPath)) {
            read();
        } else {
            create();
        }
        return this;
    }

    private void read() {
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath.toFile()))) {

            String content = reader.lines().collect(Collectors.joining());
            Map<String, Object> fileConfig = parseObject(content);

            Map<String, Object> defaultConfig = new LinkedHashMap<>(this.configMap);

            this.configMap.clear();
            this.configMap.putAll(fileConfig);

            for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
                this.configMap.putIfAbsent(entry.getKey(), entry.getValue());
            }

            Object fileVersion = fileConfig.get("Version");
            Object defaultVersion = defaultConfig.get("Version");

            if (!Objects.equals(fileVersion, defaultVersion)) {
                this.configMap.put("Version", defaultVersion);
                saveToFile();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configPath, e);
        }
    }

    private void create() {
        try {
            Files.createDirectories(this.configPath.getParent());
            saveToFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file: " + configPath, e);
        }
    }

    public void saveToFile() {
        try (Writer writer = new FileWriter(configPath.toFile())) {
            writer.write(toJson(configMap, 0));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file: " + configPath, e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public JsonConfig put(String key, Object value) {
        this.configMap.put(key, value);
        return this;
    }

    private Object get(String key) {
        return this.configMap.get(key);
    }

    public int getInt(String key) {
        return ((Number) get(key)).intValue();
    }

    public double getDouble(String key) {
        return ((Number) get(key)).doubleValue();
    }

    public float getFloat(String key) {
        return ((Number) get(key)).floatValue();
    }

    public long getLong(String key) {
        return ((Number) get(key)).longValue();
    }

    public boolean getBoolean(String key) {
        return (Boolean) get(key);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public <T> Stream<T> getStream(String key, @NotNull Class<T> valueType) {
        return ((List<?>) get(key)).stream().map(valueType::cast);
    }

    public <T> List<T> getList(String key, @NotNull Class<T> valueType) {
        return getStream(key, valueType).collect(Collectors.toList());
    }

    public <T> Set<T> getSet(String key, @NotNull Class<T> valueType) {
        return getStream(key, valueType).collect(Collectors.toSet());
    }

    // ========================= JSON 写入 =========================

    private String toJson(Object obj, int indent) {
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (var entry : map.entrySet()) {
                indent(sb, indent + 1);
                sb.append("\"").append(entry.getKey()).append("\": ");
                sb.append(toJson(entry.getValue(), indent + 1));
                if (i++ < map.size() - 1) sb.append(",");
                sb.append("\n");
            }
            indent(sb, indent);
            sb.append("}");
            return sb.toString();
        }

        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJson(list.get(i), indent));
                if (i < list.size() - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof String s) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }

        return String.valueOf(obj);
    }

    private void indent(StringBuilder sb, int indent) {
        sb.append("  ".repeat(indent));
    }

    // ========================= JSON 解析（简化版） =========================

    private Map<String, Object> parseObject(String json) {
        json = json.trim();
        if (json.isEmpty() || json.equals("{}")) return new LinkedHashMap<>();

        Map<String, Object> map = new LinkedHashMap<>();
        json = json.substring(1, json.length() - 1).trim();

        int i = 0;
        while (i < json.length()) {

            // key
            int keyStart = json.indexOf('"', i) + 1;
            int keyEnd = json.indexOf('"', keyStart);
            String key = json.substring(keyStart, keyEnd);

            // value
            int colon = json.indexOf(':', keyEnd);
            i = colon + 1;

            ParseResult result = parseValue(json, i);
            map.put(key, result.value);
            i = result.nextIndex;

            if (i < json.length() && json.charAt(i) == ',') i++;
        }

        return map;
    }

    private ParseResult parseValue(String json, int index) {
        while (Character.isWhitespace(json.charAt(index))) index++;

        char c = json.charAt(index);

        // String
        if (c == '"') {
            int end = json.indexOf('"', index + 1);
            return new ParseResult(json.substring(index + 1, end), end + 1);
        }

        // Array
        if (c == '[') {
            List<Object> list = new ArrayList<>();
            index++;

            while (json.charAt(index) != ']') {
                ParseResult r = parseValue(json, index);
                list.add(r.value);
                index = r.nextIndex;
                if (json.charAt(index) == ',') index++;
            }

            return new ParseResult(list, index + 1);
        }

        // Number / boolean
        int end = index;
        while (end < json.length() && ",}]".indexOf(json.charAt(end)) == -1) end++;

        String token = json.substring(index, end).trim();

        Object value;
        if ("true".equals(token)) value = true;
        else if ("false".equals(token)) value = false;
        else if (token.contains(".")) value = Double.parseDouble(token);
        else value = Long.parseLong(token);

        return new ParseResult(value, end);
    }

    private static class ParseResult {
        Object value;
        int nextIndex;

        ParseResult(Object value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}