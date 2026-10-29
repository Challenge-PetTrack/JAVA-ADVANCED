package com.fiap.pettrack.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.pettrack.dto.HealthContextDTO;
import com.fiap.pettrack.dto.HealthReportDTO;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Component
public class HealthReportClient {
    private static final int MAX_BYTES = 262144;
    private final RestClient client;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final String token;

    public HealthReportClient(@Qualifier("healthReportRestClient") RestClient client, ObjectMapper mapper,
            Validator validator, @Value("${pettrack.health-report.service-token:${HEALTH_REPORT_SERVICE_TOKEN:}}") String token) {
        this.client = client;
        this.mapper = mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.validator = validator;
        this.token = token;
    }

    public HealthReportDTO gerar(HealthContextDTO contexto) {
        if (token == null || token.length() < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Integracao de IA nao configurada");
        }
        try {
            byte[] body = mapper.writeValueAsBytes(contexto);
            if (body.length > MAX_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Contexto excede limite de geracao");
            }
            return client.post().uri("/health-report").contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token).body(body)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status != 200) {
                            HttpStatus destino = switch (status) {
                                case 429 -> HttpStatus.TOO_MANY_REQUESTS;
                                case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
                                case 504 -> HttpStatus.GATEWAY_TIMEOUT;
                                case 413 -> HttpStatus.PAYLOAD_TOO_LARGE;
                                default -> HttpStatus.BAD_GATEWAY;
                            };
                            throw new ResponseStatusException(destino, "Servico de IA nao concluiu a geracao");
                        }
                        var tipo = response.getHeaders().getContentType();
                        if (tipo == null || !MediaType.APPLICATION_JSON.isCompatibleWith(tipo)) throw invalida();
                        byte[] resultado = response.getBody().readNBytes(MAX_BYTES + 1);
                        if (resultado.length > MAX_BYTES) throw invalida();
                        HealthReportDTO dto;
                        try {
                            dto = mapper.readValue(new String(resultado, StandardCharsets.UTF_8), HealthReportDTO.class);
                        } catch (IOException | IllegalArgumentException ex) {
                            throw invalida();
                        }
                        if (dto == null || !validator.validate(dto).isEmpty()
                                || Boolean.TRUE.equals(dto.persistido())
                                || !dto.idPet().equals(contexto.pet().id())
                                || !dto.contextoGeradoEm().toInstant().equals(contexto.geradoEm().toInstant())
                                || dto.simulado() != "mock".equals(dto.provedor())) throw invalida();
                        return dto;
                    });
        } catch (ResourceAccessException ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Tempo de geracao esgotado");
                }
                cause = cause.getCause();
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servico de IA indisponivel");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha no contrato de geracao");
        }
    }

    private ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta de IA invalida");
    }
}
