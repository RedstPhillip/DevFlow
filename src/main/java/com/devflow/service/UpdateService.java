package com.devflow.service;

import com.devflow.config.AppConfig;
import com.devflow.config.TokenStore;
import com.devflow.util.JsonUtil;
import com.devflow.util.PlatformUtil;
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

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CompletableFuture<UpdateInfo> checkForUpdate() {
        String url = "https://api.github.com/repos/" + AppConfig.GITHUB_OWNER + "/"
                + AppConfig.GITHUB_REPO + "/commits/" + AppConfig.GITHUB_BRANCH;

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
                    JsonObject commit = JsonUtil.gson().fromJson(response.body(), JsonObject.class);
                    return updateFromLatestCommit(commit);
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

    private UpdateInfo updateFromLatestCommit(JsonObject latest) {
        if (latest == null || !latest.has("sha") || latest.get("sha").isJsonNull()) return null;
        String latestSha = latest.get("sha").getAsString();
        String currentSha = AppConfig.APP_COMMIT == null ? "" : AppConfig.APP_COMMIT.trim();
        if (!currentSha.isBlank() && latestSha.equalsIgnoreCase(currentSha)) return null;
        if (!currentSha.isBlank() && latestSha.startsWith(currentSha)) return null;
        if (!currentSha.isBlank() && currentSha.length() >= 7 && currentSha.length() <= latestSha.length()
                && currentSha.equalsIgnoreCase(latestSha.substring(0, currentSha.length()))) {
            return null;
        }

        String shortSha = latestSha.length() > 12 ? latestSha.substring(0, 12) : latestSha;
        return new UpdateInfo("Commit " + shortSha, commitNotes(latest), null);
    }

    private String commitNotes(JsonObject latest) {
        JsonObject commit = latest.has("commit") && latest.get("commit").isJsonObject()
                ? latest.getAsJsonObject("commit") : null;
        String message = "";
        if (commit != null && commit.has("message") && !commit.get("message").isJsonNull()) {
            message = commit.get("message").getAsString();
        }
        String htmlUrl = latest.has("html_url") && !latest.get("html_url").isJsonNull()
                ? latest.get("html_url").getAsString() : "";
        StringBuilder notes = new StringBuilder("Neuer Commit auf ")
                .append(AppConfig.GITHUB_BRANCH)
                .append(" gefunden.");
        if (!message.isBlank()) notes.append("\n\n").append(message);
        if (!htmlUrl.isBlank()) notes.append("\n\n").append(htmlUrl);
        return notes.toString();
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
