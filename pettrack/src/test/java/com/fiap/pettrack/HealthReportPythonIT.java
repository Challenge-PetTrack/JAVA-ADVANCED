package com.fiap.pettrack;

import com.fiap.pettrack.config.HealthReportConfig;
import com.fiap.pettrack.service.HealthReportClient;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PETTRACK_HEALTH_REPORT_SMOKE", matches = "true")
class HealthReportPythonIT {
    @Test void javaEnviaContratoRealAoPythonERecebeRelatorioValidado() {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var rest = new HealthReportConfig().healthReportRestClient(RestClient.builder(), "http://127.0.0.1:18080");
            var client = new HealthReportClient(rest, HealthReportFixtures.mapper(), validators.getValidator(),
                    "token-de-integracao-ficticio-com-32-caracteres");
            var report = client.gerar(HealthReportFixtures.contexto());
            assertTrue(report.simulado());
            assertFalse(report.persistido());
            assertEquals("mock", report.provedor());
            assertEquals(1L, report.idPet());
            assertEquals("INDETERMINADO", report.relatorio().nivelAtencao().name());
        }
    }
}
