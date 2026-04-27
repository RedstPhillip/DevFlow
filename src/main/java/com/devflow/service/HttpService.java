package com.devflow.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.devflow.config.ConnectionState;
import com.devflow.config.TokenStore;
import com.devflow.util.JsonUtil;
import com.google.gson.JsonObject;

public class HttpService {

    private static final HttpService INSTANCE = new HttpService();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private final HttpClient client;

    /**
     * In-flight token-refresh future. While non-null every request that hits
     * a 401 chains onto it instead of starting its own refresh — that's the
     * "401-swarm" guard: when the access token expires while N concurrent
     * polls are running we want exactly ONE refresh and N retries, not N
     * refreshes (which would race the refresh-token rotation and most likely
     * fail with "refresh token reused").
     *
     * <p>Synchronisation: the field is read+written under {@link #refreshLock}.
     * Once the refresh future completes (success or failure) it is cleared
     * back to {@code null} inside a {@code whenComplete} handler.</p>
     */
    private CompletableFuture<TokenStore> refreshInFlight;
    private final Object refreshLock = new Object();

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
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .GET();
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> post(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> put(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> delete(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .DELETE();
        addAuthHeader(builder);
        return sendWithRetry(builder.build());
    }

    public CompletableFuture<HttpResponse<String>> postNoAuth(String url, Object body) {
        String json = body instanceof String ? (String) body : JsonUtil.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
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
                        // Coalesce concurrent 401s onto a single refresh —
                        // every request gets retried with the new token once
                        // the refresh resolves. See refreshInFlight comment.
                        return sharedRefresh().thenCompose(tokens -> retryWithFreshToken(request));
                    }
                    return CompletableFuture.completedFuture(response);
                });
    }

    /**
     * Wrap {@code sendAsync} so caller chains see network failures as a
     * failed future, never an unchecked throw. Also flips
     * {@link ConnectionState} based on the outcome: any successful response
     * (including 4xx/5xx) means the backend is reachable; an exception
     * (UnknownHostException, ConnectException, timeout, …) means we lost it.
     */
    private CompletableFuture<HttpResponse<String>> safeSend(HttpRequest request) {
        try {
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            ConnectionState.getInstance().markOffline();
                        } else {
                            ConnectionState.getInstance().markOnline();
                        }
                    });
        } catch (Throwable t) {
            ConnectionState.getInstance().markOffline();
            return CompletableFuture.failedFuture(t);
        }
    }

    /**
     * Returns the in-flight refresh future, kicking off a new one if none is
     * running. Concurrent 401s all observe the same future and therefore
     * trigger exactly one refresh round-trip.
     */
    private CompletableFuture<TokenStore> sharedRefresh() {
        synchronized (refreshLock) {
            if (refreshInFlight != null) return refreshInFlight;
            refreshInFlight = doRefresh().whenComplete((r, t) -> {
                synchronized (refreshLock) { refreshInFlight = null; }
            });
            return refreshInFlight;
        }
    }

    /**
     * Hit {@code POST /auth/refresh} with the current refresh token. On
     * success the new tokens are written into {@link TokenStore}; on failure
     * the auth token is cleared (caller will see a 401 next time it tries
     * an authenticated request). Returns a future that resolves with the
     * (now-updated) {@link TokenStore} for chaining.
     */
    private CompletableFuture<TokenStore> doRefresh() {
        if (!TokenStore.getInstance().hasAuthToken()) {
            return CompletableFuture.failedFuture(new RuntimeException("No auth token"));
        }
        String refreshToken = TokenStore.getInstance().getAuthToken().getRefreshToken();
        if (refreshToken == null) {
            TokenStore.getInstance().clearAuthToken();
            return CompletableFuture.failedFuture(new RuntimeException("No refresh token"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);
        String apiUrl = com.devflow.config.AppConfig.API_URL;

        HttpRequest refreshRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/auth/refresh"))
            .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return safeSend(refreshRequest)
                .thenCompose(refreshResponse -> {
                    if (refreshResponse.statusCode() == 200) {
                        var newToken = JsonUtil.fromJson(refreshResponse.body(),
                                com.devflow.model.AuthToken.class);
                        TokenStore.getInstance().setAuthToken(newToken);
                        return CompletableFuture.completedFuture(TokenStore.getInstance());
                    }
                    TokenStore.getInstance().clearAuthToken();
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Token refresh failed: HTTP " + refreshResponse.statusCode()));
                });
    }

    /**
     * Re-dispatch the original request with the freshly-rotated bearer token.
     * Method/body are preserved; only the Authorization header changes.
     */
    private CompletableFuture<HttpResponse<String>> retryWithFreshToken(HttpRequest originalRequest) {
        String accessToken = TokenStore.getInstance().hasAuthToken()
                ? TokenStore.getInstance().getAuthToken().getAccessToken()
                : null;
        if (accessToken == null) {
            return CompletableFuture.failedFuture(new RuntimeException("No access token after refresh"));
        }
        HttpRequest.Builder retryBuilder = HttpRequest.newBuilder()
                .uri(originalRequest.uri())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken);
        if (originalRequest.method().equals("GET")) {
            retryBuilder.GET();
        } else {
            retryBuilder.method(originalRequest.method(),
                    originalRequest.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
        }
        return safeSend(retryBuilder.build());
    }
}
