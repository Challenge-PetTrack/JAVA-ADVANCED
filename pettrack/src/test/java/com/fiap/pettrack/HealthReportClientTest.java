package com.fiap.pettrack;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiap.pettrack.service.HealthReportClient;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HealthReportClientTest {
    private MockRestServiceServer server;
    private HealthReportClient client;
    private ValidatorFactory validators;
    private RestClient.Builder builder;
    private final String token = "token-ficticio-para-integracao-123456";

    @BeforeEach void setup() {
        builder = RestClient.builder().baseUrl("http://localhost:8000");
        server = MockRestServiceServer.bindTo(builder).build();
        validators = Validation.buildDefaultValidatorFactory();
        client = new HealthReportClient(builder.build(), HealthReportFixtures.mapper(), validators.getValidator(), token);
    }
    @AfterEach void fechar() { validators.close(); }

    @Test void enviaContextoAutenticadoEValidaResposta() throws Exception {
        server.expect(requestTo("http://localhost:8000/health-report"))
                .andExpect(header("Authorization", "Bearer " + token))
                .andExpect(content().json(HealthReportFixtures.mapper().writeValueAsString(HealthReportFixtures.contexto())))
                .andRespond(withSuccess(HealthReportFixtures.mapper().writeValueAsString(HealthReportFixtures.resultado()), MediaType.APPLICATION_JSON));
        var response = client.gerar(HealthReportFixtures.contexto());
        assertEquals(1L, response.idPet());
        assertFalse(response.persistido());
        assertFalse(response.simulado());
        server.verify();
    }

    @Test void tokenAusenteNaoFazChamada() {
        client = new HealthReportClient(builder.build(), HealthReportFixtures.mapper(), validators.getValidator(), "");
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> client.gerar(HealthReportFixtures.contexto())).getStatusCode().value());
        server.verify();
    }

    @ParameterizedTest @CsvSource({"401,502", "429,429", "503,503", "504,504", "302,502", "413,413", "500,502"})
    void traduzErrosSemPropagarCorpoDoServico(int original, int esperado) {
        server.expect(anything()).andRespond(withRawStatus(original).body("SEGREDO_NAO_PROPAGAR"));
        var ex = assertThrows(ResponseStatusException.class, () -> client.gerar(HealthReportFixtures.contexto()));
        assertEquals(esperado, ex.getStatusCode().value());
        assertFalse(ex.getMessage().contains("SEGREDO"));
        server.verify();
    }

    @ParameterizedTest @CsvSource({"idPet,2", "persistido,true", "simulado,true", "versaoPrompt,invalido", "modelo,''"})
    void rejeitaIdentidadeEContratoInconsistentes(String campo, String valor) throws Exception {
        ObjectNode body = HealthReportFixtures.mapper().valueToTree(HealthReportFixtures.resultado());
        if (campo.equals("idPet")) body.put(campo, Long.parseLong(valor));
        else if (campo.equals("simulado") || campo.equals("persistido")) body.put(campo, Boolean.parseBoolean(valor));
        else body.put(campo, valor);
        rejeita(body.toString(), MediaType.APPLICATION_JSON);
    }

    @Test void rejeitaContextoDeOutroInstante() {
        ObjectNode body = HealthReportFixtures.mapper().valueToTree(HealthReportFixtures.resultado());
        body.put("contextoGeradoEm", "2020-01-01T00:00:00Z");
        rejeita(body.toString(), MediaType.APPLICATION_JSON);
    }

    @Test void rejeitaHtmlDeLogin() { rejeita("<html>Login</html>", MediaType.TEXT_HTML); }
    @Test void rejeitaJsonIncompleto() { rejeita("{}", MediaType.APPLICATION_JSON); }
    @Test void rejeitaRespostaExcessiva() { rejeita(" ".repeat(262145), MediaType.APPLICATION_JSON); }

    @Test void timeoutRetorna504() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("timeout")));
        assertEquals(504, assertThrows(ResponseStatusException.class, () -> client.gerar(HealthReportFixtures.contexto())).getStatusCode().value());
    }

    private void rejeita(String body, MediaType tipo) {
        server.expect(anything()).andRespond(withSuccess(body, tipo));
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> client.gerar(HealthReportFixtures.contexto())).getStatusCode().value());
        server.verify();
    }
}
