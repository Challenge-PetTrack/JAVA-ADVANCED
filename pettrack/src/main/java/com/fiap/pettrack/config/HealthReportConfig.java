package com.fiap.pettrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Configuration
public class HealthReportConfig {
    @Bean
    public RestClient healthReportRestClient(RestClient.Builder builder,
            @Value("${pettrack.health-report.api-url:${HEALTH_REPORT_API_URL:http://127.0.0.1:8000}}") String url) {
        URI uri = URI.create(url);
        boolean local = "http".equals(uri.getScheme()) && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost() == null ? "" : uri.getHost());
        if (uri.getHost() == null || (!"https".equals(uri.getScheme()) && !local)
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Health Report exige HTTPS ou HTTP em loopback, sem credenciais na URL");
        }
        var factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(40));
        return builder.baseUrl(url).requestFactory(factory).build();
    }
}
