package dev.cerus.hourlycatbot.catsource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CataasDotComSource implements CatSource {

    private static final String API_URL = "https://cataas.com/cat?json=true";
    private static final String SOURCE = "https://cataas.com";

    private OkHttpClient httpClient;

    @Override
    public void setup(final OkHttpClient client) {
        this.httpClient = client;
    }

    @Override
    public CatImage fetch() throws IOException {
        final Call call = this.httpClient.newCall(new Request.Builder()
                .get()
                .url(API_URL)
                .build());
        try (final Response response = call.execute()) {
            final JsonObject object = JsonParser.parseString(response.body().string()).getAsJsonObject();
            final String imageUrl = SOURCE + object.get("url").getAsString();
            final String desc = "Image #" + object.get("_id").getAsString();
            return new CatImage(imageUrl, desc, SOURCE);
        }
    }

}
