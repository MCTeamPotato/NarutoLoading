package me.kall.narutoloading.impl;

import java.util.function.BooleanSupplier;

public final class Substitution {
    private static final Substitution INSTANCE = new Substitution();

    private final BooleanSupplier running = ClientUiControl::running;
    private final BooleanSupplier videoPresent = ClientUiControl::videoPresent;
    private final Runnable substitute = ClientUiControl::render;

    private boolean tickConsumed = false;

    public static boolean check() {
        return INSTANCE.running.getAsBoolean() && INSTANCE.videoPresent.getAsBoolean();
    }

    public static boolean checkAndRun() {
        boolean running = INSTANCE.running.getAsBoolean();
        if (running) {
            if (!INSTANCE.tickConsumed) {
                INSTANCE.substitute.run();
                INSTANCE.tickConsumed = true;
            }
        }
        return running && INSTANCE.videoPresent.getAsBoolean();
    }

    public static void tick() {
        INSTANCE.tickConsumed = false;
    }
}
