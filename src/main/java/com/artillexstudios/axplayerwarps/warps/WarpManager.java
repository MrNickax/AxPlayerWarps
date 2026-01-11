package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;

import java.util.ArrayList;

public class WarpManager {
    private static final ArrayList<Warp> warps = new ArrayList<>();

    public static void reloadWarp(int id) {
        warps.removeIf(w -> w.getId() == id);
        AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().loadWarp(id));
    }

    public static void load() {
        AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().loadWarps());
    }

    public static ArrayList<Warp> getWarps() {
        return warps;
    }
}
