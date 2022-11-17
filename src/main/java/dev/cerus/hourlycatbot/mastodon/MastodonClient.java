package dev.cerus.hourlycatbot.mastodon;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cerus.hourlycatbot.mastodon.entity.Application;
import dev.cerus.hourlycatbot.mastodon.entity.Error;
import dev.cerus.hourlycatbot.mastodon.entity.Token;
import dev.cerus.hourlycatbot.mastodon.model.Status;
import dev.cerus.hourlycatbot.util.RequestBodyUtil;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Represents a client that communicates with a Mastodon instance
 */
public class MastodonClient {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ClientConfig config;
    private Token token;

    public MastodonClient(final ClientConfig config) {
        this.config = config;
    }

    /**
     * Authorize ourselves with an existing token
     *
     * @param token
     */
    public void authorize(final Token token) {
        this.token = token;
    }

    /**
     * Authorize ourselves by fetching a new token
     *
     * @param scopes The scopes of the token
     *
     * @return A result containing the token
     *
     * @throws IOException
     */
    public Result<Token> authorize(final String... scopes) throws IOException {
        final Result<Token> result = this.runAndParse(Token.class, this.prepareTokenCall(scopes));
        result.optionalData().ifPresent(t -> {
            this.token = t;
        });
        return result;
    }

    /**
     * Verify our token
     *
     * @return A result containing an Application entity
     *
     * @throws IOException
     */
    public Result<Application> verify() throws IOException {
        return this.runAndParse(Application.class, this.prepareVerifyCall());
    }

    /**
     * Upload media to the Mastodon instance
     *
     * @param mediaType The type of the media
     * @param data      The media data
     * @param desc      The media description
     *
     * @return A result containing the media id
     *
     * @throws IOException
     */
    public Result<String> submitMedia(final MediaType mediaType, final byte[] data, final String desc) throws IOException {
        final Result<JsonObject> result = this.runAndParse(JsonObject.class, this.prepareMediaCall(mediaType, data, desc));
        return result.map(object -> object.get("id").getAsString());
    }

    /**
     * Post a new status
     *
     * @param status The status to post
     *
     * @return A result
     *
     * @throws IOException
     */
    public Result<Void> publishStatus(final Status status) throws IOException {
        return this.runAndParse(JsonElement.class, this.prepareNewStatusCall(status)).map(o -> null);
    }

    private Call prepareMediaCall(final MediaType mediaType, final byte[] data, final String desc) {
        return this.httpClient.newCall(new Request.Builder()
                .post(new MultipartBody.Builder()
                        .addFormDataPart("file", "name", RequestBodyUtil.create(mediaType, data))
                        .addFormDataPart("description", desc)
                        .build())
                .url(this.apiUrl("v1", "media", Map.of()))
                .header("Authorization", "Bearer " + this.token.getAccessToken())
                .build());
    }

    private Call prepareNewStatusCall(final Status status) {
        return this.httpClient.newCall(new Request.Builder()
                .post(status.toFormData())
                .url(this.apiUrl("v1", "statuses", Map.of()))
                .header("Authorization", "Bearer " + this.token.getAccessToken())
                .build());
    }

    private Call prepareVerifyCall() {
        return this.httpClient.newCall(new Request.Builder()
                .url(this.apiUrl("v1", "apps/verify_credentials", Map.of()))
                .header("Authorization", "Bearer " + (this.token == null ? "" : this.token.getAccessToken()))
                .build());
    }

    private Call prepareTokenCall(final String[] scopes) {
        final MultipartBody.Builder builder = new MultipartBody.Builder()
                .addFormDataPart("client_id", this.config.clientId())
                .addFormDataPart("client_secret", this.config.clientSecret())
                .addFormDataPart("redirect_uri", this.config.redirectUri())
                .addFormDataPart("grant_type", this.config.grantType())
                .addFormDataPart("scope", String.join(" ", scopes));
        if (this.config.userCode() != null) {
            builder.addFormDataPart("code", this.config.userCode());
        }
        return this.httpClient.newCall(new Request.Builder()
                .post(RequestBody.create(new byte[0]))
                .post(builder.build())
                .url(this.url("oauth/token", Map.of()))
                .build());
    }

    private <T> Result<T> runAndParse(final Class<T> cls, final Call call) throws IOException {
        try (final Response response = this.run(call)) {
            final String responseBody = response.body().string();
            final JsonElement responseElement = JsonParser.parseString(responseBody);
            if (responseElement.isJsonObject() && responseElement.getAsJsonObject().has("error")) {
                return Result.failed(new Gson().fromJson(responseElement, Error.class));
            }
            return Result.successful(new Gson().fromJson(responseElement, cls));
        }
    }

    private Response run(final Call call) throws IOException {
        return call.execute();
    }

    private String apiUrl(final String ver, final String path, final Map<String, String> params) {
        return this.url("api/" + ver + "/" + path, params);
    }

    private String url(final String path, final Map<String, String> params) {
        return this.config.mastodonInstance() + "/" + path + (params.isEmpty() ? "" : "?" + params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&")));
    }

    public Token getToken() {
        return this.token;
    }

}
