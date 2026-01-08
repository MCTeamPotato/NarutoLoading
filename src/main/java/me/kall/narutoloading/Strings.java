package me.kall.narutoloading;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Strings {
    @Contract(pure = true)
    public static boolean isBlank(@NotNull String s) {
        if (s.isEmpty()) return true;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
