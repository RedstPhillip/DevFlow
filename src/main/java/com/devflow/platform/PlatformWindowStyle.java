package com.devflow.platform;

import javafx.stage.StageStyle;

public final class PlatformWindowStyle {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    private PlatformWindowStyle() {
    }

    public static StageStyle appStageStyle() {
        return WINDOWS ? StageStyle.UNDECORATED : StageStyle.TRANSPARENT;
    }

    public static boolean usesOpaqueFramelessWindow() {
        return WINDOWS;
    }
}
