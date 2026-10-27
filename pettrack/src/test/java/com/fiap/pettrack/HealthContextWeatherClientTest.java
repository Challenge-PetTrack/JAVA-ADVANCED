package com.fiap.pettrack;

import com.fiap.pettrack.service.HealthContextWeatherClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HealthContextWeatherClientTest {
    private final Instant agora = Instant.parse("2026-10-26T15:00:00Z");
    private final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private HealthContextWeatherClient client;

    @BeforeEach void setup() {
        builder = RestClient.builder().baseUrl("https://api.openweathermap.org");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HealthContextWeatherClient(builder.build(), clock, "chave-ficticia-teste", "Sao Paulo,BR");
    }

    @Test void semConfiguracaoNaoFazRequisicao() {
        var result = new HealthContextWeatherClient(builder.build(), clock, "", "").buscar();
        assertEquals("NAO_CONFIGURADO", result.status());
        assertNull(result.temperaturaCelsius());
        server.verify();
    }

    @Test void interpretaRespostaValidaComUnidadeELocalidadeExplicitas() {
        server.expect(queryParam("units", "metric")).andExpect(queryParam("q", "Sao%20Paulo,BR"))
                .andRespond(withSuccess(resposta(agora.getEpochSecond()), MediaType.APPLICATION_JSON));
        var result = client.buscar();
        assertEquals("DISPONIVEL", result.status());
        assertEquals(32.4, result.temperaturaCelsius());
        assertEquals(55.0, result.umidadePercentual());
        assertEquals("Sao Paulo,BR", result.localidade());
        server.verify();
    }

    @Test void erroDoProvedorNaoDerrubaContextoNemExibeChave() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        var result = client.buscar();
        assertEquals("FALHA_PROVEDOR", result.status());
        assertFalse(result.toString().contains("chave-ficticia"));
        server.verify();
    }

    @Test void camposAusentesNaoSaoConvertidosEmZero() {
        server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var result = client.buscar();
        assertEquals("RESPOSTA_INVALIDA", result.status());
        assertNull(result.temperaturaCelsius());
        server.verify();
    }

    @Test void climaAntigoNaoETratadoComoAtual() {
        server.expect(anything()).andRespond(withSuccess(resposta(agora.minusSeconds(14400).getEpochSecond()), MediaType.APPLICATION_JSON));
        assertEquals("DADO_DESATUALIZADO", client.buscar().status());
        server.verify();
    }

    private String resposta(long instante) {
        return "{\"dt\":" + instante + ",\"main\":{\"temp\":32.4,\"humidity\":55},\"weather\":[{\"description\":\"ceu limpo\"}]}";
    }
}
