package dev.cerus.hourlycatbot.openai.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record ChatCompletion(
        String id,
        long created,
        String model,
        Usage usage,
        List<MessageHolder> choices
) {

    public record Usage(
            @SerializedName("prompt_tokens") int promptTokens,
            @SerializedName("completion_tokens") int completionTokens,
            @SerializedName("total_tokens") int totalTokens
    ) {
    }

    public record MessageHolder(
            Message message,
            @SerializedName("finish_reason") String finishReason,
            int index
    ) {

    }

    public record Message(
            String role,
            String content
    ) {
    }

}
