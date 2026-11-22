package me.kall.narutoloading.media;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class MediaSelector {
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private MediaSelector() {}

    public static <T> @Nullable T pick(@NotNull List<T> candidates, @NotNull Function<T, String> namer, @NotNull MediaRepository.Mode mode, @Nullable String fixedName, @Nullable String lastName, boolean warnMissingFixed) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        if (mode == MediaRepository.Mode.FIXED) {
            if (fixedName != null && !fixedName.isBlank()) {
                for (T candidate : candidates) {
                    if (namer.apply(candidate).equalsIgnoreCase(fixedName.trim())) return candidate;
                }

                if (warnMissingFixed && WARNED.add("fixed:" + fixedName)) {
                    System.err.println("[NarutoLoading] fixedName '" + fixedName + "' was not found in sources, falling back to random selection.");
                }
            }
            mode = MediaRepository.Mode.RANDOM;
        }

        if (mode == MediaRepository.Mode.SEQUENTIAL) {
            int next = 0;
            for (int i = 0; i < candidates.size(); i++) {
                if (namer.apply(candidates.get(i)).equalsIgnoreCase(lastName)) {
                    next = (i + 1) % candidates.size();
                    break;
                }
            }
            return candidates.get(next);
        }

        List<T> pool = new ArrayList<>(candidates.size());
        for (T candidate : candidates) {
            if (!namer.apply(candidate).equalsIgnoreCase(lastName)) pool.add(candidate);
        }

        if (pool.isEmpty()) pool = candidates;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
