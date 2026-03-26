package me.kall.narutoloading.app;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Executable {
    public static @NotNull String executeCommand(@NotNull String[] command) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                output.append(line.trim()).append("\n");
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (exitCode != 0) {
            throw new RuntimeException("IOException: Command exited with code " + exitCode + ": " + Arrays.toString(command));
        }

        return output.toString();
    }
}
