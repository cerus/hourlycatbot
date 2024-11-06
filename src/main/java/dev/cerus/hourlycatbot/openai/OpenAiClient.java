package dev.cerus.hourlycatbot.openai;

import com.google.gson.JsonObject;
import dev.cerus.hourlycatbot.mastodon.ClientConfig;
import dev.cerus.hourlycatbot.net.BaseClient;
import dev.cerus.hourlycatbot.net.Result;
import dev.cerus.hourlycatbot.openai.model.ChatCompletion;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OpenAiClient extends BaseClient {

    private static final String API_URL = "https://api.openai.com/v1";
    private static final int MAX_TOKENS = 250;

    private final OkHttpClient httpClient = new OkHttpClient(new OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS));
    private final ClientConfig config;
    private final VisionPayload.Detail detail;

    public OpenAiClient(final ClientConfig config) {
        this.config = config;
        this.detail = VisionPayload.Detail.valueOf(config.openAiDetail());
    }

    public Result<ChatCompletion> vision(final String inputText, final String inputImageUrl) throws IOException {
        return this.runAndParse(ChatCompletion.class, this.prepareVisionCall(inputText, inputImageUrl));
    }

    private Call prepareVisionCall(final String inputText, final String inputImageUrl) {
        final JsonObject payload = VisionPayload.createPayload(inputText, inputImageUrl, MAX_TOKENS, this.detail);
        return this.httpClient.newCall(new Request.Builder()
                .url(API_URL + "/chat/completions")
                .header("Authorization", "Bearer " + this.config.openAiToken())
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                .build());
    }


}
