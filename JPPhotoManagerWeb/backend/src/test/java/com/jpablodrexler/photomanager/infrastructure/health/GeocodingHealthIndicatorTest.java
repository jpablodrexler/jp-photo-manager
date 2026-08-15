package com.jpablodrexler.photomanager.infrastructure.health;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingHealthIndicatorTest {

    @Test
    void health_unreachableUrl_returnsDegraded() {
        // Use a URL that will always fail (unreachable host)
        GeocodingHealthIndicator sut = new GeocodingHealthIndicator("http://localhost:19999/unreachable");

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(GeocodingHealthIndicator.DEGRADED);
    }

    @Test
    void health_invalidUrl_returnsDegraded() {
        GeocodingHealthIndicator sut = new GeocodingHealthIndicator("http://this.host.does.not.exist.invalid");

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(GeocodingHealthIndicator.DEGRADED);
    }

    @Test
    void health_serverRespondsWith2xx_returnsUp() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/";
            GeocodingHealthIndicator sut = new GeocodingHealthIndicator(url);

            Health result = sut.health();

            assertThat(result.getStatus()).isEqualTo(Status.UP);
            assertThat(result.getDetails()).containsEntry("url", url);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void health_serverRespondsWithNon2xx_returnsDegradedWithHttpStatusDetail() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/";
            GeocodingHealthIndicator sut = new GeocodingHealthIndicator(url);

            Health result = sut.health();

            assertThat(result.getStatus()).isEqualTo(GeocodingHealthIndicator.DEGRADED);
            assertThat(result.getDetails()).containsEntry("httpStatus", 503);
        } finally {
            server.stop(0);
        }
    }
}
