package com.fiap.pettrack;

import com.fiap.pettrack.config.HealthReportConfig;
import com.fiap.pettrack.service.HealthReportClient;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PETTRACK_GEMINI_LIVE_TESTS", matches = "true")
class HealthReportGeminiIT {
    @Test void javaPythonGeminiGeramRelatorioRealComDadosFicticios() throws Exception {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            String token = System.getenv("HEALTH_REPORT_SERVICE_TOKEN");
            assertNotNull(token, "Configurar token de servico no ambiente");
            var rest = new HealthReportConfig().healthReportRestClient(RestClient.builder(), "http://127.0.0.1:18081");
            var client = new HealthReportClient(rest, HealthReportFixtures.mapper(), validators.getValidator(), token);
            var report = client.gerar(HealthReportFixtures.contexto());
            assertFalse(report.simulado());
            assertFalse(report.persistido());
            assertEquals("gemini", report.provedor());
            assertEquals(1L, report.idPet());
            assertFalse(report.relatorio().resumo().isBlank());
            assertFalse(report.relatorio().motivos().isEmpty());
            assertFalse(report.relatorio().recomendacoes().isEmpty());
            assertTrue(report.relatorio().limitacoes().stream().anyMatch(s -> s.contains("Dados sinteticos")));
            String output = System.getProperty("pettrack.health-report.evidence");
            if (output != null) {
                Path path = Path.of(output);
                Files.createDirectories(path.toAbsolutePath().getParent());
                HealthReportFixtures.mapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), report);
            }
        }
    }
}
