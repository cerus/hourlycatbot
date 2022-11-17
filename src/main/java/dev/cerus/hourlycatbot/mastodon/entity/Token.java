package dev.cerus.hourlycatbot.mastodon.entity;

import com.google.gson.annotations.SerializedName;

public class Token {

    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("scope")
    private String scope;
    @SerializedName("created_at")
    private long createdAt;

    public Token(final String accessToken, final String tokenType, final String scope, final long createdAt) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.createdAt = createdAt;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public String getScope() {
        return this.scope;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

}
