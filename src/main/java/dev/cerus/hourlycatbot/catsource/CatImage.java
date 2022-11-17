package dev.cerus.hourlycatbot.catsource;

/**
 * Simple model for cat images
 *
 * @param url         The url pointing to the image
 * @param description The description for the image
 * @param source      The source of the image
 */
public record CatImage(String url, String description, String source) {
}
