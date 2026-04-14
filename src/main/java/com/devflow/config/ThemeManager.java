package com.devflow.config;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.devflow.util.PlatformUtil;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static final ThemeManager INSTANCE = new ThemeManager();

    private final Path themeFile;
    private final Set<Scene> scenes = new HashSet<>();
    private Theme theme;

    private ThemeManager() {
        this.themeFile = PlatformUtil.getAppDataDir().resolve("theme");
        this.theme = load();
    }

    public static ThemeManager getInstance() { return INSTANCE; }

    public Theme getTheme() { return theme; }

    public boolean isDark() { return theme == Theme.DARK; }

    public void registerScene(Scene scene) {
        scenes.add(scene);
        applyTo(scene);
    }

    public void unregisterScene(Scene scene) {
        scenes.remove(scene);
    }

    public void toggle() {
        setTheme(theme == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    public void setTheme(Theme newTheme) {
        if (this.theme == newTheme) return;
        this.theme = newTheme;
        save();
        Application.setUserAgentStylesheet(newTheme == Theme.DARK
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        for (Scene scene : scenes) {
            applyTo(scene);
        }
    }

    public void applyInitial() {
        Application.setUserAgentStylesheet(theme == Theme.DARK
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
    }

    private void applyTo(Scene scene) {
        Parent root = scene.getRoot();
        if (root == null) return;
        root.getStyleClass().remove("light-theme");
        if (theme == Theme.LIGHT) {
            root.getStyleClass().add("light-theme");
        }
    }

    private Theme load() {
        try {
            if (Files.exists(themeFile)) {
                String value = Files.readString(themeFile).trim();
                if ("LIGHT".equalsIgnoreCase(value)) return Theme.LIGHT;
            }
        } catch (IOException ignored) {}
        return Theme.DARK;
    }

    private void save() {
        try {
            Files.createDirectories(themeFile.getParent());
            Files.writeString(themeFile, theme.name());
        } catch (IOException ignored) {}
    }
}
