package com.devflow.service;

import com.devflow.config.TokenStore;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpService {

    private static final HttpService INSTANCE = new HttpService();
    private final HttpClient client;
    // Per-request retry guard could leak across concurrent calls when stored on the singleton.
    // Use an AtomicBoolean so token refresh races never trigger more than one refresh attempt.
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private HttpService() {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public static HttpService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<HttpResponse<String>> get(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET();
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> post(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> put(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> delete(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .DELETE();
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> postNoAuth(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return safeSend(request);
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        TokenStore store = TokenStore.getInstance();
        if (store.hasAuthToken()) {
            builder.header("Authorization", "Bearer " + store.getAuthToken().getAccessToken());
        }
    }

    private CompletableFuture<HttpResponse<String>> sendWithRetry(HttpRequest request) {
        return safeSend(request)
                .thenCompose(response -> {
                    if (response.statusCode() == 401 && TokenStore.getInstance().hasAuthToken()) {
                        if (refreshing.compareAndSet(false, true)) {
                            return tryRefreshAndRetry(request);
                        }
                        // A refresh is already in flight from another request; just bubble the 401
                        // so the caller can decide what to do. The retry loop will recover on next call.
                    }
                    return CompletableFuture.completedFuture(response);
                });
    }

    /** Wraps sendAsync so caller chains see network failures as a failed future, never an unchecked throw. */
    private CompletableFuture<HttpResponse<String>> safeSend(HttpRequest request) {
        try {
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    private CompletableFuture<HttpResponse<String>> tryRefreshAndRetry(HttpRequest originalRequest) {
        String refreshToken = TokenStore.getInstance().getAuthToken().getRefreshToken();
        if (refreshToken == null) {
            refreshing.set(false);
            TokenStore.getInstance().clearAuthToken();
            return CompletableFuture.failedFuture(new RuntimeException("No refresh token"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);
        String apiUrl = com.devflow.config.AppConfig.API_URL;

        HttpRequest refreshRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/auth/refresh"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return safeSend(refreshRequest)
                .whenComplete((r, t) -> refreshing.set(false))
                .thenCompose(refreshResponse -> {
                    if (refreshResponse.statusCode() == 200) {
                        var newToken = JsonUtil.fromJson(refreshResponse.body(),
                                com.devflow.model.AuthToken.class);
                        TokenStore.getInstance().setAuthToken(newToken);

                        HttpRequest.Builder retryBuilder = HttpRequest.newBuilder()
                                .uri(originalRequest.uri())
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + newToken.getAccessToken());
                        if (originalRequest.method().equals("GET")) {
                            retryBuilder.GET();
                        } else {
                            retryBuilder.method(originalRequest.method(),
                                    originalRequest.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
                        }

                        return safeSend(retryBuilder.build());
                    } else {
                        TokenStore.getInstance().clearAuthToken();
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Token refresh failed"));
                    }
                });
    }
}
