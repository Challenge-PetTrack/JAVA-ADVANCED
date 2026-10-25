package com.fiap.pettrack;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

// Teste explicito: instala apenas a migracao pendente e verifica os objetos.
@EnabledIfEnvironmentVariable(named = "PETTRACK_ORACLE_TESTS", matches = "true")
class PetTrackFlywayOracleIT {
    @Test void migracaoV4ValidaHistoricoEInstalaPackagesSemErros() throws Exception {
        Properties p = new Properties();
        try (var in = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(in);
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        Flyway flyway = Flyway.configure().dataSource(p.getProperty("spring.datasource.url"),
                p.getProperty("spring.datasource.username"), p.getProperty("spring.datasource.password"))
                .baselineOnMigrate(true).locations("classpath:db/migration").load();
        flyway.migrate();
        assertEquals("4", flyway.info().current().getVersion().getVersion());
        try (Connection c = flyway.getConfiguration().getDataSource().getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM USER_ERRORS WHERE NAME LIKE 'PKG_PETTRACK_%' OR NAME='TRG_AUDITORIA_PET'")) {
            assertTrue(r.next()); assertEquals(0, r.getInt(1));
        }
    }
}
