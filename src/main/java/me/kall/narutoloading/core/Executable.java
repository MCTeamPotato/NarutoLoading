package me.kall.narutoloading.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class Executable {
    private Executable() {}

    public static @Nullable String runCommand(@Nullable Consumer<String> outputLineConsumer, @Nullable List<String> command) {
        if (command == null || command.isEmpty()) return null;

        Process process;

        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception starting command " + Arrays.toString(command.toArray()) + " : " + exception.getMessage());
            exception.printStackTrace(System.err);
            return null;
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            String line;
            while ((line = reader.readLine()) != null) {
                if (outputLineConsumer != null) outputLineConsumer.accept(line);
                output.append(line.trim()).append("\n");
            }
        } catch (IOException exception) {
            System.err.println("[NarutoLoading] Exception consuming input stream of command " + Arrays.toString(command.toArray()) + " : " + exception.getMessage());
            exception.printStackTrace(System.err);
            return null;
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("[NarutoLoading] Command " + Arrays.toString(command.toArray()) + " exited with code " + exitCode + ". Read log for details.");
                return null;
            }
        } catch (InterruptedException exception) {
            System.err.println("[NarutoLoading] The execution thread of command " + Arrays.toString(command.toArray()) + " is interrupted: " + exception.getMessage());
            exception.printStackTrace(System.err);
            Thread.currentThread().interrupt();
            return null;
        }

        return output.toString().trim();
    }

    public static @Nullable String runCommand(String @NotNull ... command) {
        return runCommand(null, command);
    }

    public static @Nullable String runCommand(@Nullable Consumer<String> outputLineConsumer, String @NotNull ... command) {
        if (command.length == 0) return null;
        return runCommand(outputLineConsumer, List.of(command));
    }
}
