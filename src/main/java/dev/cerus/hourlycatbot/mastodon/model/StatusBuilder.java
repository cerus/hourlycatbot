package dev.cerus.hourlycatbot.mastodon.model;

public class StatusBuilder {

    private String content;
    private String[] mediaIds;
    private boolean sensitive;
    private String spoilerText;
    private String visibility;
    private String language;

    public StatusBuilder setContent(final String content) {
        this.content = content;
        return this;
    }

    public StatusBuilder setMediaIds(final String... mediaIds) {
        this.mediaIds = mediaIds;
        return this;
    }

    public StatusBuilder setSensitive(final boolean sensitive) {
        this.sensitive = sensitive;
        return this;
    }

    public StatusBuilder setSpoilerText(final String spoilerText) {
        this.spoilerText = spoilerText;
        return this;
    }

    public StatusBuilder setVisibility(final String visibility) {
        this.visibility = visibility;
        return this;
    }

    public StatusBuilder setLanguage(final String language) {
        this.language = language;
        return this;
    }

    public Status createStatus() {
        return new Status(this.content, this.mediaIds, this.sensitive, this.spoilerText, this.visibility, this.language);
    }

}