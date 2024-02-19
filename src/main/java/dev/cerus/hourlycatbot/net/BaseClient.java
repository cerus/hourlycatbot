package dev.cerus.hourlycatbot.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Response;

public abstract class BaseClient {

    protected <T> Result<T> runAndParse(final Class<T> cls, final Call call) throws IOException {
        try (final Response response = this.run(call)) {
            final String responseBody = response.body().string();
            final JsonElement responseElement = JsonParser.parseString(responseBody);
            if (responseElement.isJsonObject() && responseElement.getAsJsonObject().has("error")) {
                return Result.failed(new Gson().fromJson(responseElement, Error.class));
            }
            return Result.successful(new Gson().fromJson(responseElement, cls));
        }
    }

    protected Response run(final Call call) throws IOException {
        return call.execute();
    }

}
