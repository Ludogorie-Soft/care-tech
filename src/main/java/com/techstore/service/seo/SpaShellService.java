package com.techstore.service.seo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * The frontend's index.html — the shell React boots from.
 *
 * Asked again on every call with If-None-Match, so a frontend deploy shows up at once (its index.html
 * points to new hashed bundles). When the frontend does not answer, the last good copy is used.
 */
@Service
@Slf4j
public class SpaShellService {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final URI indexUrl;

    private volatile Shell cached;

    private record Shell(String html, String etag) {}

    public SpaShellService(@Value("${app.spa-index-url}") String indexUrl) {
        this.indexUrl = URI.create(indexUrl);
    }

    /** The current index.html, or empty when the frontend has not answered since startup. */
    public Optional<String> indexHtml() {
        Shell current = cached;
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(indexUrl).timeout(TIMEOUT).GET();
            if (current != null && current.etag() != null) {
                request.header("If-None-Match", current.etag());
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                current = new Shell(response.body(), response.headers().firstValue("ETag").orElse(null));
                cached = current;
            } else if (response.statusCode() != 304) {
                log.warn("SPA index.html at {} answered {}", indexUrl, response.statusCode());
            }
        } catch (IOException e) {
            log.warn("SPA index.html at {} is unreachable: {}", indexUrl, e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.ofNullable(current).map(Shell::html);
    }
}
