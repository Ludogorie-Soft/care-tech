package com.techstore.service.seo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaShellService — index.html от frontend контейнера")
class SpaShellServiceTest {

    private HttpServer server;
    private volatile String body = "<html><head></head><body>v1</body></html>";
    private volatile String etag = "\"v1\"";
    private final List<String> ifNoneMatch = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/index.html", exchange -> {
            String sent = exchange.getRequestHeaders().getFirst("If-None-Match");
            ifNoneMatch.add(sent);
            if (etag.equals(sent)) {
                exchange.sendResponseHeaders(304, -1);
            } else {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("ETag", etag);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/index.html";
    }

    @Test
    @DisplayName("Пита с If-None-Match; при 304 връща вече взетото копие")
    void revalidatesWithEtag() {
        SpaShellService service = new SpaShellService(url());

        assertThat(service.indexHtml()).contains(body);
        assertThat(service.indexHtml()).contains(body);
        assertThat(ifNoneMatch).containsExactly(null, "\"v1\"");
    }

    @Test
    @DisplayName("Нов деплой на frontend-а се вижда веднага")
    void picksUpANewDeploy() {
        SpaShellService service = new SpaShellService(url());
        service.indexHtml();

        body = "<html><head></head><body>v2</body></html>";
        etag = "\"v2\"";

        assertThat(service.indexHtml()).contains(body);
    }

    @Test
    @DisplayName("Frontend-ът спре → последното копие")
    void keepsTheLastCopyWhenTheFrontendIsDown() {
        SpaShellService service = new SpaShellService(url());
        String first = service.indexHtml().orElseThrow();

        server.stop(0);

        assertThat(service.indexHtml()).contains(first);
    }

    @Test
    @DisplayName("Frontend-ът никога не е отговорил → празно (контролерът връща 503)")
    void emptyWhenNeverReachable() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }

        assertThat(new SpaShellService("http://127.0.0.1:" + freePort + "/index.html").indexHtml()).isEmpty();
    }
}
