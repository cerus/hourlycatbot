package dev.cerus.hourlycatbot.mastodon.entity;

import com.google.gson.annotations.SerializedName;

public class Error {

    @SerializedName("error")
    private String name;
    @SerializedName("error_description")
    private String description;

    public Error(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return this.getName() + ": " + this.getDescription();
    }

}
