package com.devflow;

public class Launcher {
    public static void main(String[] args) {
        // Prism rendering quality — must be set before JavaFX toolkit starts.
        // These must be here (not in MainApp) because VS Code launches Launcher directly.
        System.setProperty("prism.lcdtext", "true");
        System.setProperty("prism.subpixeltext", "on");
        System.setProperty("prism.forceGPU", "true");
        System.setProperty("prism.order", "d3d,sw");
        MainApp.main(args);
    }
}
