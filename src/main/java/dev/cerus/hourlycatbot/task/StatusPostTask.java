package dev.cerus.hourlycatbot.task;

import static dev.cerus.hourlycatbot.Launcher.LOGGER;
import dev.cerus.hourlycatbot.catsource.CatImage;
import dev.cerus.hourlycatbot.catsource.CatSource;
import dev.cerus.hourlycatbot.mastodon.MastodonClient;
import dev.cerus.hourlycatbot.mastodon.model.StatusBuilder;
import dev.cerus.hourlycatbot.net.Result;
import dev.cerus.hourlycatbot.openai.OpenAiClient;
import dev.cerus.hourlycatbot.openai.model.ChatCompletion;
import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StatusPostTask implements Runnable {

    private static final long MILLIS_POST_EPOCH = TimeUnit.HOURS.toMillis(3);
    private static final long MILLIS_RETRY_EPOCH = TimeUnit.MINUTES.toMillis(2);

    private static final String POST_CONTENT_DEFAULT = "Via %s\n#Bot #CatsOfMastodon #Catstodon #Cat";
    private static final String POST_CONTENT_CATURDAY = POST_CONTENT_DEFAULT + " #Caturday";
    private static final String POST_CONTENT_SUFFIX = " #AltText";

    private final MastodonClient mastodonClient;
    private final OpenAiClient openAiClient;
    private final OkHttpClient sourceClient;
    private final CatSource[] catSources;
    private long nextRun;

    public StatusPostTask(final MastodonClient mastodonClient, final OpenAiClient openAiClient, final OkHttpClient sourceClient, final CatSource[] catSources) {
        this.mastodonClient = mastodonClient;
        this.openAiClient = openAiClient;
        this.sourceClient = sourceClient;
        this.catSources = catSources;
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() < this.nextRun) {
            return;
        }

        // Fetch a cat image
        final CatImage catImage;
        final byte[] actualCatImage;

        final int idx = ThreadLocalRandom.current().nextInt(0, this.catSources.length);
        try {
            catImage = this.catSources[idx].fetch();
        } catch (final Throwable t) {
            LOGGER.error("Failed to fetch cat image from " + this.catSources[idx].getClass().getSimpleName(), t);
            this.nextRun = System.currentTimeMillis() + MILLIS_RETRY_EPOCH;
            return;
        }

        try {
            final Response response = this.sourceClient.newCall(new Request.Builder()
                    .url(catImage.url())
                    .build()).execute();
            actualCatImage = response.body().bytes();
        } catch (final Throwable t) {
            LOGGER.error("Failed to fetch actual cat image from " + this.catSources[idx].getClass().getSimpleName(), t);
            this.nextRun = System.currentTimeMillis() + MILLIS_RETRY_EPOCH;
            return;
        }

        String mediaType = "image/png";
        if (catImage.url().substring(catImage.url().lastIndexOf('/')).contains(".")) {
            mediaType = "image/" + catImage.url().substring(catImage.url().lastIndexOf('.') + 1);
        }

        // Post the image
        LOGGER.info("Selected " + catImage.url());
        try {
            final String altText;
            final Result<ChatCompletion> visionResult = this.openAiClient.vision("Generate an alt text for this image.", catImage.url());
            if (visionResult.isErroneous()) {
                LOGGER.warn("Failed to generate alt text for url {}: {}", catImage.url(), visionResult.getError());
                altText = catImage.description();
            } else {
                altText = visionResult.getData().choices().get(0).message().content(); // This is ugly
                LOGGER.info("Generated alt text for image {}: '{}'", catImage.url(), altText);
            }

            final Result<String> mediaResult = this.mastodonClient.submitMedia(MediaType.get(mediaType), actualCatImage, altText);
            if (mediaResult.isErroneous()) {
                LOGGER.error("Failed to submit media: " + mediaResult.getError());
                this.nextRun = System.currentTimeMillis() + MILLIS_RETRY_EPOCH;
                return;
            }
            final String mediaId = mediaResult.getData();
            LOGGER.info("Image was uploaded as #" + mediaResult.getData());
            final Result<Void> statusResult = this.mastodonClient.publishStatus(new StatusBuilder()
                    .setContent(this.getPostContent(catImage.source()))
                    .setMediaIds(mediaId)
                    .createStatus());
            if (statusResult.isErroneous()) {
                LOGGER.error("Failed to submit status: " + statusResult.getError());
                this.nextRun = System.currentTimeMillis() + MILLIS_RETRY_EPOCH;
                return;
            }
            LOGGER.info("Status was published");
            this.nextRun = System.currentTimeMillis() + MILLIS_POST_EPOCH;
        } catch (final Throwable t) {
            LOGGER.error("Failed to post status", t);
            this.nextRun = System.currentTimeMillis() + MILLIS_RETRY_EPOCH;
        }
    }

    private String getPostContent(final String imageSource) {
        return (this.isTodayCaturday() ? POST_CONTENT_CATURDAY : POST_CONTENT_DEFAULT).formatted(imageSource) + POST_CONTENT_SUFFIX;
    }

    private boolean isTodayCaturday() {
        final Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
    }

}
