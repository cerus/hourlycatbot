package dev.cerus.hourlycatbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.cerus.hourlycatbot.catsource.CatSource;
import dev.cerus.hourlycatbot.catsource.CataasDotComSource;
import dev.cerus.hourlycatbot.catsource.RandomDotCatSource;
import dev.cerus.hourlycatbot.catsource.TheCatApiDotComSource;
import dev.cerus.hourlycatbot.mastodon.ClientConfig;
import dev.cerus.hourlycatbot.mastodon.MastodonClient;
import dev.cerus.hourlycatbot.mastodon.Result;
import dev.cerus.hourlycatbot.mastodon.entity.Application;
import dev.cerus.hourlycatbot.mastodon.entity.Token;
import dev.cerus.hourlycatbot.task.StatusPostTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    public static final Logger LOGGER = LoggerFactory.getLogger("HourlyCatBot");
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
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new StatusPostTask(mastodonClient, sourceClient, catSources), 0, 1, TimeUnit.MINUTES);

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
