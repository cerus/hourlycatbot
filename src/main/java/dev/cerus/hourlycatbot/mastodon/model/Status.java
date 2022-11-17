package dev.cerus.hourlycatbot.mastodon.model;

import com.google.gson.annotations.SerializedName;
import okhttp3.MultipartBody;

public class Status {

    @SerializedName("status")
    private String content;
    @SerializedName("media_ids")
    private String[] mediaIds;
    @SerializedName("sensitive")
    private boolean sensitive;
    @SerializedName("spoiler_text")
    private String spoilerText;
    @SerializedName("visibility")
    private String visibility;
    @SerializedName("language")
    private String language;

    public Status(final String content, final String[] mediaIds, final boolean sensitive, final String spoilerText, final String visibility, final String language) {
        this.content = content;
        this.mediaIds = mediaIds;
        this.sensitive = sensitive;
        this.spoilerText = spoilerText;
        this.visibility = visibility;
        this.language = language;
    }

    public static StatusBuilder builder() {
        return new StatusBuilder();
    }

    public MultipartBody toFormData() {
        final MultipartBody.Builder builder = new MultipartBody.Builder();
        if (this.content != null) {
            builder.addFormDataPart("status", this.content);
        }
        if (this.mediaIds != null) {
            for (final String mediaId : this.mediaIds) {
                builder.addFormDataPart("media_ids[]", mediaId);
            }
        }
        builder.addFormDataPart("sensitive", String.valueOf(this.sensitive));
        if (this.spoilerText != null) {
            builder.addFormDataPart("spoiler_text", this.spoilerText);
        }
        if (this.visibility != null) {
            builder.addFormDataPart("visibility", this.visibility);
        }
        if (this.language != null) {
            builder.addFormDataPart("language", this.language);
        }
        return builder.build();
    }

    public String getContent() {
        return this.content;
    }

    public String[] getMediaIds() {
        return this.mediaIds;
    }

    public boolean isSensitive() {
        return this.sensitive;
    }

    public String getSpoilerText() {
        return this.spoilerText;
    }

    public String getVisibility() {
        return this.visibility;
    }

    public String getLanguage() {
        return this.language;
    }

}
