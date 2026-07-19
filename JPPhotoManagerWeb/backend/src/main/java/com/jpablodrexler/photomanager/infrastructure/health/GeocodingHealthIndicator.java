package com.jpablodrexler.photomanager.infrastructure.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Component("geocoding")
@Slf4j
public class GeocodingHealthIndicator implements HealthIndicator {

    static final Status DEGRADED = new Status("DEGRADED");

    private final String geocodingBaseUrl;

    public GeocodingHealthIndicator(
            @Value("${photomanager.geocoding-base-url}") String geocodingBaseUrl) {
        this.geocodingBaseUrl = geocodingBaseUrl;
    }

    @Override
    public Health health() {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(geocodingBaseUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestProperty("User-Agent", "jp-photo-manager-healthcheck/1.0");
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return Health.up().withDetail("url", geocodingBaseUrl).build();
            }
            return Health.status(DEGRADED)
                    .withDetail("url", geocodingBaseUrl)
                    .withDetail("httpStatus", responseCode)
                    .build();
        } catch (IOException e) {
            log.debug("Geocoding health check unreachable: {}", e.getMessage());
            return Health.status(DEGRADED)
                    .withDetail("url", geocodingBaseUrl)
                    .withDetail("reason", e.getMessage())
                    .build();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
