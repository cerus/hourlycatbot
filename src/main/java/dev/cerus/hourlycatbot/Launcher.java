package dev.cerus.hourlycatbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.cerus.hourlycatbot.catsource.CatImage;
import dev.cerus.hourlycatbot.catsource.CatSource;
import dev.cerus.hourlycatbot.catsource.CataasDotComSource;
import dev.cerus.hourlycatbot.catsource.RandomDotCatSource;
import dev.cerus.hourlycatbot.catsource.TheCatApiDotComSource;
import dev.cerus.hourlycatbot.mastodon.ClientConfig;
import dev.cerus.hourlycatbot.mastodon.MastodonClient;
import dev.cerus.hourlycatbot.mastodon.Result;
import dev.cerus.hourlycatbot.mastodon.entity.Application;
import dev.cerus.hourlycatbot.mastodon.entity.Token;
import dev.cerus.hourlycatbot.mastodon.model.StatusBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    private static final Logger LOGGER = LoggerFactory.getLogger("HourlyCatBot");
    private static final File TOKEN_FILE = new File("./token.json");

    public static void main(final String[] args) throws IOException {
        // Instantiate a new client
        final ClientConfig clientConfig = new ClientConfig(
                System.getenv("INSTANCE"),
                System.getenv("CLIENTID"),
                System.getenv("CLIENTSECRET"),
                "urn:ietf:wg:oauth:2.0:oob",
                "authorization_code",
                System.getenv("CODE")
        );
        final MastodonClient mastodonClient = new MastodonClient(clientConfig);
        if (!attemptToAuthorize(mastodonClient, "read", "write:media", "write:statuses")) {
            return;
        }

        // Instantiate our cat sources
        final OkHttpClient sourceClient = new OkHttpClient();
        final CatSource[] catSources = new CatSource[] {
                new RandomDotCatSource(),
                new TheCatApiDotComSource(),
                new CataasDotComSource()
        };
        for (final CatSource source : catSources) {
            source.setup(sourceClient);
        }

        // Run the status post task
        // TODO: Move into separate class
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            // Fetch a cat image
            CatImage catImage = null;
            byte[] actualCatImage = null;
            while (catImage == null) {
                final int idx = ThreadLocalRandom.current().nextInt(0, catSources.length);
                try {
                    catImage = catSources[idx].fetch();
                } catch (final Throwable t) {
                    LOGGER.error("Failed to fetch cat image from " + catSources[idx].getClass().getSimpleName(), t);
                    continue;
                }

                try {
                    final Response response = sourceClient.newCall(new Request.Builder()
                            .url(catImage.url())
                            .build()).execute();
                    actualCatImage = response.body().bytes();
                } catch (final Throwable t) {
                    catImage = null;
                    LOGGER.error("Failed to fetch actual cat image from " + catSources[idx].getClass().getSimpleName(), t);
                }
            }

            String mediaType = "image/png";
            if (catImage.url().substring(catImage.url().lastIndexOf('/')).contains(".")) {
                mediaType = "image/" + catImage.url().substring(catImage.url().lastIndexOf('.') + 1);
            }

            // Post the image
            LOGGER.info("Selected " + catImage.url());
            try {
                final Result<String> mediaResult = mastodonClient.submitMedia(MediaType.get(mediaType), actualCatImage, catImage.description());
                if (mediaResult.isErroneous()) {
                    LOGGER.error("Failed to submit media: " + mediaResult.getError());
                    return;
                }
                final String mediaId = mediaResult.getData();
                LOGGER.info("Image was uploaded as #" + mediaResult.getData());
                final Result<Void> statusResult = mastodonClient.publishStatus(new StatusBuilder()
                        .setContent("Via " + catImage.source())
                        .setMediaIds(mediaId)
                        .createStatus());
                if (statusResult.isErroneous()) {
                    LOGGER.error("Failed to submit status: " + statusResult.getError());
                    return;
                }
                LOGGER.info("Status was published");
            } catch (final Throwable t) {
                LOGGER.error("Failed to post status", t);
            }
        }, 0, 3, TimeUnit.HOURS);

        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
    }

    private static boolean attemptToAuthorize(final MastodonClient client, final String... scopes) throws IOException {
        final Token savedToken = readSavedToken();
        if (savedToken != null) {
            // Found a token
            LOGGER.info("Reading saved token");
            client.authorize(savedToken);
            if (client.verify().isErroneous()) {
                // Token is invalid
                LOGGER.warn("Saved token is invalid, re-authenticating");
                client.authorize(scopes).optionalError().ifPresent(error ->
                        LOGGER.error("Authorize call failed: " + error));
            }
        } else {
            // No token found
            LOGGER.info("Fetching new token");
            client.authorize(scopes).optionalError().ifPresent(error ->
                    LOGGER.error("Authorize call failed: " + error));
        }

        final Result<Application> verifyResult = client.verify();
        if (verifyResult.isErroneous()) {
            // Unable to continue
            LOGGER.error("Unable to authenticate with Mastodon instance");
            LOGGER.error(verifyResult.getError().toString());
            return false;
        }

        LOGGER.info("Saving token");
        saveToken(client.getToken());
        return true;
    }

    private static Token readSavedToken() {
        if (TOKEN_FILE.exists()) {
            try (final FileInputStream in = new FileInputStream(TOKEN_FILE);
                 final InputStreamReader reader = new InputStreamReader(in)) {
                return new Gson().fromJson(JsonParser.parseReader(reader), Token.class);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static void saveToken(final Token token) {
        try (final FileOutputStream out = new FileOutputStream(TOKEN_FILE)) {
            out.write(new GsonBuilder().setPrettyPrinting().create().toJson(token).getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
