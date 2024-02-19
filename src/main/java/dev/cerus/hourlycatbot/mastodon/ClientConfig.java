package dev.cerus.hourlycatbot.mastodon;

/**
 * Config for MastodonClients
 */
public record ClientConfig(
        String mastodonInstance,
        String clientId,
        String clientSecret,
        String redirectUri,
        String grantType,
        String userCode,
        String openAiToken,
        String openAiDetail
) {
}
