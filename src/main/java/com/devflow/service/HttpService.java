package com.devflow.service;

import com.devflow.config.TokenStore;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpService {

    private static final HttpService INSTANCE = new HttpService();
    private final HttpClient client;
    private boolean retrying = false;

    private HttpService() {
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
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

    public CompletableFuture<HttpResponse<String>> postNoAuth(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        TokenStore store = TokenStore.getInstance();
        if (store.hasAuthToken()) {
            builder.header("Authorization", "Bearer " + store.getAuthToken().getAccessToken());
        }
    }

    private CompletableFuture<HttpResponse<String>> sendWithRetry(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 401 && !retrying && TokenStore.getInstance().hasAuthToken()) {
                        retrying = true;
                        return tryRefreshAndRetry(request);
                    }
                    retrying = false;
                    return CompletableFuture.completedFuture(response);
                });
    }

    private CompletableFuture<HttpResponse<String>> tryRefreshAndRetry(HttpRequest originalRequest) {
        String refreshToken = TokenStore.getInstance().getAuthToken().getRefreshToken();
        if (refreshToken == null) {
            retrying = false;
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

        return client.sendAsync(refreshRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(refreshResponse -> {
                    retrying = false;
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

                        return client.sendAsync(retryBuilder.build(), HttpResponse.BodyHandlers.ofString());
                    } else {
                        TokenStore.getInstance().clearAuthToken();
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Token refresh failed"));
                    }
                });
    }
}
