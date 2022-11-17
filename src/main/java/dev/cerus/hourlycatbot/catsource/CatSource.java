package dev.cerus.hourlycatbot.catsource;

import java.io.IOException;
import okhttp3.OkHttpClient;

/**
 * Represents a source for cat images
 */
public interface CatSource {

    /**
     * Setup this source
     *
     * @param client The http client
     */
    void setup(OkHttpClient client);

    /**
     * Fetch a random cat image
     *
     * @return A cat image
     *
     * @throws IOException
     */
    CatImage fetch() throws IOException;

}
