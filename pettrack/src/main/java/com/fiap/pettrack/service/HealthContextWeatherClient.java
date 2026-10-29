package com.fiap.pettrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.pettrack.dto.HealthContextDTO.Clima;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class HealthContextWeatherClient {
    private final RestClient client;
    private final String apiKey;
    private final String city;
    private final Clock clock;

    public HealthContextWeatherClient(@Qualifier("healthContextWeatherRestClient") RestClient client, Clock clock,
            @Value("${pettrack.health-context.weather.api-key:${OPENWEATHER_API_KEY:}}") String apiKey,
            @Value("${pettrack.health-context.weather.city:${OPENWEATHER_CITY:}}") String city) {
        this.client = client;
        this.apiKey = apiKey;
        this.city = city;
        this.clock = clock;
    }

    public Clima buscar() {
        if (apiKey.isBlank() || city.isBlank()) return Clima.indisponivel("NAO_CONFIGURADO", city);
        try {
            JsonNode body = client.get().uri(uri -> uri.path("/data/2.5/weather")
                    .queryParam("q", city).queryParam("appid", apiKey)
                    .queryParam("units", "metric").queryParam("lang", "pt_br").build())
                    .retrieve().body(JsonNode.class);
            if (body == null || !body.path("dt").canConvertToLong()
                    || !body.path("main").path("temp").isNumber()
                    || !body.path("main").path("humidity").isNumber()
                    || !body.path("weather").path(0).path("description").isTextual()) {
                return Clima.indisponivel("RESPOSTA_INVALIDA", city);
            }
            double temp = body.path("main").path("temp").asDouble();
            double umidade = body.path("main").path("humidity").asDouble();
            Instant observado = Instant.ofEpochSecond(body.path("dt").asLong());
            if (!Double.isFinite(temp) || !Double.isFinite(umidade) || umidade < 0 || umidade > 100) {
                return Clima.indisponivel("RESPOSTA_INVALIDA", city);
            }
            if (observado.isBefore(clock.instant().minus(Duration.ofHours(3)))
                    || observado.isAfter(clock.instant().plus(Duration.ofMinutes(5)))) {
                return Clima.indisponivel("DADO_DESATUALIZADO", city);
            }
            return new Clima("DISPONIVEL", "OpenWeatherMap", city,
                    OffsetDateTime.ofInstant(observado, ZoneOffset.UTC), temp, umidade,
                    body.path("weather").path(0).path("description").asText());
        } catch (RuntimeException ex) {
            return Clima.indisponivel("FALHA_PROVEDOR", city);
        }
    }
}
