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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class UpdateService {

    private final HttpClient client = HttpClient.newHttpClient();

    public CompletableFuture<UpdateInfo> checkForUpdate() {
        String pat = TokenStore.getInstance().getGithubPat();
        if (pat == null || pat.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        String url = "https://api.github.com/repos/" + AppConfig.GITHUB_OWNER + "/"
                + AppConfig.GITHUB_REPO + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + pat)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("GitHub API returned " + response.statusCode());
                        return null;
                    }
                    JsonObject release = JsonUtil.gson().fromJson(response.body(), JsonObject.class);
                    String tagName = release.get("tag_name").getAsString();
                    String remoteVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                    if (isNewer(remoteVersion, AppConfig.APP_VERSION)) {
                        String releaseNotes = release.has("body") && !release.get("body").isJsonNull()
                                ? release.get("body").getAsString() : "";
                        String downloadUrl = null;

                        JsonArray assets = release.getAsJsonArray("assets");
                        if (assets != null) {
                            for (var asset : assets) {
                                String name = asset.getAsJsonObject().get("name").getAsString();
                                if (name.endsWith(".jar")) {
                                    downloadUrl = asset.getAsJsonObject()
                                            .get("browser_download_url").getAsString();
                                    break;
                                }
                            }
                        }
                        return new UpdateInfo(remoteVersion, releaseNotes, downloadUrl);
                    }
                    return null;
                });
    }

    public CompletableFuture<Path> downloadUpdate(String downloadUrl) {
        String pat = TokenStore.getInstance().getGithubPat();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Accept", "application/octet-stream")
                .GET();
        if (pat != null) {
            builder.header("Authorization", "token " + pat);
        }

        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
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

    public void applyUpdate(Path updateJar) throws IOException {
        Path currentJar = Path.of(System.getProperty("java.class.path").split(
                System.getProperty("path.separator"))[0]);
        Path targetJar = currentJar.toAbsolutePath();

        if (PlatformUtil.isWindows()) {
            Path script = PlatformUtil.getAppDataDir().resolve("update.cmd");
            String cmd = "@echo off\r\n"
                    + "timeout /t 3 /nobreak >nul\r\n"
                    + "copy /Y \"" + updateJar.toAbsolutePath() + "\" \"" + targetJar + "\"\r\n"
                    + "del \"" + updateJar.toAbsolutePath() + "\"\r\n"
                    + "start javaw -jar \"" + targetJar + "\"\r\n"
                    + "del \"%~f0\"\r\n";
            Files.writeString(script, cmd);
            new ProcessBuilder("cmd", "/c", "start", "/min", script.toString())
                    .inheritIO().start();
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

    private boolean isNewer(String remote, String current) {
        String[] r = remote.split("\\.");
        String[] c = current.split("\\.");
        for (int i = 0; i < Math.max(r.length, c.length); i++) {
            int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
            int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
            if (rv > cv) return true;
            if (rv < cv) return false;
        }
        return false;
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
