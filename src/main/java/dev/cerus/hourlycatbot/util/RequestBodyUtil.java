package dev.cerus.hourlycatbot.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class RequestBodyUtil {

    public static RequestBody create(final MediaType mediaType, final byte[] data) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                return data.length;
            }

            @Override
            public void writeTo(final BufferedSink sink) throws IOException {
                try (final ByteArrayInputStream in = new ByteArrayInputStream(data);
                     final Source source = Okio.source(in)) {
                    sink.writeAll(source);
                }
            }
        };
    }

}
