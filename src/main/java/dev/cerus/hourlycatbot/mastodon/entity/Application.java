package dev.cerus.hourlycatbot.mastodon.entity;

import com.google.gson.annotations.SerializedName;

public class Application {

    @SerializedName("name")
    private String name;
    @SerializedName("website")
    private String website;
    @SerializedName("vapid_key")
    private String vapidKey;

    public Application(final String name, final String website, final String vapidKey) {
        this.name = name;
        this.website = website;
        this.vapidKey = vapidKey;
    }

    public String getName() {
        return this.name;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getVapidKey() {
        return this.vapidKey;
    }

}
