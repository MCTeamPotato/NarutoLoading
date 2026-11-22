package me.kall.narutoloading.client;

public final class Substitution {
    private static final Substitution INSTANCE = new Substitution();

    private boolean tickConsumed = false;

    public static boolean check() {
        return ClientUiControl.running() && ClientUiControl.videoPresent();
    }

    public static boolean checkAndRun() {
        boolean running = ClientUiControl.running();

        if (running) {
            if (!INSTANCE.tickConsumed) {
                ClientUiControl.render();
                INSTANCE.tickConsumed = true;
            }
        }

        return running && ClientUiControl.videoPresent();
    }

    public static void tick() {
        INSTANCE.tickConsumed = false;
    }
}
