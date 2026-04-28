package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.config.TokenStore;
import com.devflow.util.JsonUtil;
import com.devflow.util.PlatformUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class UpdateService {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CompletableFuture<UpdateInfo> checkForUpdate() {
        String url = "https://api.github.com/repos/" + AppConfig.GITHUB_OWNER + "/"
                + AppConfig.GITHUB_REPO + "/releases";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DevFlow-Updater/" + AppConfig.APP_VERSION)
                .GET();
        addGithubAuthIfPresent(builder);

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("GitHub API returned " + response.statusCode());
                        return null;
                    }
                    JsonArray releases = JsonUtil.gson().fromJson(response.body(), JsonArray.class);
                    return newestAvailableUpdate(releases);
                });
    }

    public CompletableFuture<Path> downloadUpdate(String downloadUrl) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Accept", "application/octet-stream")
                .GET();
        addGithubAuthIfPresent(builder);

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new RuntimeException("Download failed: HTTP " + response.statusCode());
                    }
                    try {
                        Path updateDir = PlatformUtil.getAppDataDir().resolve("updates");
                        Files.createDirectories(updateDir);
                        Path target = updateDir.resolve("devflow-update.jar");
                        Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
                        return target;
                    } catch (IOException e) {
                        throw new RuntimeException("Download failed", e);
                    }
                });
    }

    private void addGithubAuthIfPresent(HttpRequest.Builder builder) {
        String pat = TokenStore.getInstance().getGithubPat();
        if (pat != null && !pat.isBlank()) {
            builder.header("Authorization", "token " + pat.trim());
        }
    }

    private UpdateInfo newestAvailableUpdate(JsonArray releases) {
        if (releases == null || releases.isEmpty()) return null;

        JsonObject newest = null;
        String newestVersion = null;
        for (var releaseElement : releases) {
            JsonObject release = releaseElement.getAsJsonObject();
            if (release.has("draft") && release.get("draft").getAsBoolean()) continue;
            String version = normalizeTag(release);
            if (version.isBlank() || !isNewer(version, AppConfig.APP_VERSION)) continue;
            if (newestVersion == null || isNewer(version, newestVersion)) {
                newest = release;
                newestVersion = version;
            }
        }

        if (newest == null) return null;
        String releaseNotes = newest.has("body") && !newest.get("body").isJsonNull()
                ? newest.get("body").getAsString() : "";
        return new UpdateInfo(newestVersion, releaseNotes, jarDownloadUrl(newest));
    }

    private String normalizeTag(JsonObject release) {
        if (release == null || !release.has("tag_name") || release.get("tag_name").isJsonNull()) {
            return "";
        }
        String tagName = release.get("tag_name").getAsString();
        return tagName.startsWith("v") ? tagName.substring(1) : tagName;
    }

    private String jarDownloadUrl(JsonObject release) {
        JsonArray assets = release.getAsJsonArray("assets");
        if (assets == null) return null;
        for (var asset : assets) {
            JsonObject obj = asset.getAsJsonObject();
            String name = obj.has("name") && !obj.get("name").isJsonNull()
                    ? obj.get("name").getAsString() : "";
            if (name.endsWith(".jar") && obj.has("browser_download_url")) {
                return obj.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    public void applyUpdate(Path updateJar) throws IOException {
        Path currentJar = Path.of(System.getProperty("java.class.path").split(
                System.getProperty("path.separator"))[0]);
        Path targetJar = currentJar.toAbsolutePath();

        if (PlatformUtil.isWindows()) {
            Path script = PlatformUtil.getAppDataDir().resolve("update.ps1");
            String ps = "$ErrorActionPreference = 'Stop'\r\n"
                    + "Start-Sleep -Seconds 3\r\n"
                    + "$source = '" + escapePowerShell(updateJar.toAbsolutePath().toString()) + "'\r\n"
                    + "$target = '" + escapePowerShell(targetJar.toString()) + "'\r\n"
                    + "Copy-Item -LiteralPath $source -Destination $target -Force\r\n"
                    + "Remove-Item -LiteralPath $source -Force -ErrorAction SilentlyContinue\r\n"
                    + "Start-Process -FilePath 'javaw' -ArgumentList @('-jar', $target)\r\n"
                    + "Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue\r\n";
            Files.writeString(script, ps, StandardCharsets.UTF_8);
            new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-File", script.toString()).start();
        } else {
            Path script = PlatformUtil.getAppDataDir().resolve("update.sh");
            String sh = "#!/bin/bash\n"
                    + "sleep 3\n"
                    + "cp \"" + updateJar.toAbsolutePath() + "\" \"" + targetJar + "\"\n"
                    + "rm \"" + updateJar.toAbsolutePath() + "\"\n"
                    + "java -jar \"" + targetJar + "\" &\n"
                    + "rm \"$0\"\n";
            Files.writeString(script, sh);
            script.toFile().setExecutable(true);
            new ProcessBuilder("bash", script.toString()).inheritIO().start();
        }
    }

    private String escapePowerShell(String value) {
        return value.replace("'", "''");
    }

    private boolean isNewer(String remote, String current) {
        String[] r = remote.split("\\.");
        String[] c = current.split("\\.");
        for (int i = 0; i < Math.max(r.length, c.length); i++) {
            int rv = i < r.length ? parseVersionPart(r[i]) : 0;
            int cv = i < c.length ? parseVersionPart(c[i]) : 0;
            if (rv > cv) return true;
            if (rv < cv) return false;
        }
        return false;
    }

    private int parseVersionPart(String part) {
        if (part == null) return 0;
        String digits = part.replaceFirst("[^0-9].*$", "");
        if (digits.isBlank()) return 0;
        return Integer.parseInt(digits);
    }

    public static class UpdateInfo {
        public final String version;
        public final String releaseNotes;
        public final String downloadUrl;

        public UpdateInfo(String version, String releaseNotes, String downloadUrl) {
            this.version = version;
            this.releaseNotes = releaseNotes;
            this.downloadUrl = downloadUrl;
        }
    }
}
