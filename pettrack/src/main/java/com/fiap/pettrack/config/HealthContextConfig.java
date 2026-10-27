package com.fiap.pettrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;
import java.time.Duration;

@Configuration
public class HealthContextConfig {
    @Bean
    public RestClient healthContextWeatherRestClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder.baseUrl("https://api.openweathermap.org").requestFactory(factory).build();
    }

    @Bean
    public Clock healthContextClock(@Value("${pettrack.health-context.zone:America/Sao_Paulo}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
