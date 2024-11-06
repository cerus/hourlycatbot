package dev.cerus.hourlycatbot.openai;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.cerus.hourlycatbot.util.JsonBuilder;

public final class VisionPayload {

    private static final String VISION_MODEL = "gpt-4o";

    private VisionPayload() {
    }

    public static JsonObject createPayload(final String text, final String imageUrl, final int maxTokens, final Detail detail) {
        return JsonBuilder.newObjectBuilder()
                .set("model", new JsonPrimitive(VISION_MODEL))
                .set("messages", JsonBuilder.newArrayBuilder()
                        .add(JsonBuilder.newObjectBuilder()
                                .set("role", new JsonPrimitive("user"))
                                .set("content", JsonBuilder.newArrayBuilder()
                                        .add(JsonBuilder.newObjectBuilder()
                                                .set("type", new JsonPrimitive("text"))
                                                .set("text", new JsonPrimitive(text)))
                                        .add(JsonBuilder.newObjectBuilder()
                                                .set("type", new JsonPrimitive("image_url"))
                                                .set("image_url", JsonBuilder.newObjectBuilder()
                                                        .set("url", new JsonPrimitive(imageUrl))
                                                        .set("detail", new JsonPrimitive(detail.name().toLowerCase())))))))
                .set("max_tokens", new JsonPrimitive(maxTokens))
                .build();
    }

    public enum Detail {
        AUTO,
        HIGH,
        LOW
    }

}
