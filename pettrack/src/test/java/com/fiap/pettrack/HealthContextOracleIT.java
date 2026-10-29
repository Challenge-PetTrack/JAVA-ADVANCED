package com.fiap.pettrack;

import com.fiap.pettrack.repository.HealthContextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PETTRACK_ORACLE_CONTEXT_TESTS", matches = "true")
class HealthContextOracleIT {
    @Test void consultasExecutamNoOracleSemAlterarDados() throws Exception {
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        var env = new StandardEnvironment();
        env.getPropertySources().addLast(new PropertiesPropertySource("application", properties));
        var connectionProperties = new Properties();
        connectionProperties.setProperty("user", env.getRequiredProperty("spring.datasource.username"));
        connectionProperties.setProperty("password", env.getRequiredProperty("spring.datasource.password"));
        connectionProperties.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        connectionProperties.setProperty("oracle.jdbc.ReadTimeout", "10000");
        try (var connection = DriverManager.getConnection(env.getRequiredProperty("spring.datasource.url"), connectionProperties)) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION READ ONLY");
            }
            try {
                var ds = new SingleConnectionDataSource(connection, true);
                var jdbc = new JdbcTemplate(ds);
                var repository = new HealthContextRepository(new NamedParameterJdbcTemplate(ds));
                var ids = jdbc.queryForList("SELECT ID_PET FROM TB_PET ORDER BY ID_PET FETCH FIRST 1 ROWS ONLY", Long.class);
                assertFalse(ids.isEmpty(), "Banco precisa de ao menos um pet para esta verificacao");
                var fim = jdbc.queryForObject("SELECT SYSDATE FROM DUAL", Timestamp.class).toLocalDateTime();
                var iot = repository.buscarIot(ids.get(0), fim.minusHours(24), fim);
                assertNotNull(iot);
                assertTrue(iot.totalLeituras() >= iot.temperaturasValidas());
                assertTrue(repository.buscarUltimosBcs(ids.get(0), fim).size() <= 2);
                assertNotNull(repository.buscarHistorico(ids.get(0), fim));
            } finally {
                connection.rollback();
            }
        }
    }
}
