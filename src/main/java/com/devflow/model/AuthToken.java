package com.devflow.model;

import com.google.gson.annotations.SerializedName;

public class AuthToken {

    @SerializedName(value = "accessToken", alternate = {"access_token"})
    private String accessToken;

    @SerializedName(value = "refreshToken", alternate = {"refresh_token"})
    private String refreshToken;

    @SerializedName(value = "expiresAt", alternate = {"expires_at"})
    private String expiresAt;

    private User user;

    public AuthToken() {}

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
