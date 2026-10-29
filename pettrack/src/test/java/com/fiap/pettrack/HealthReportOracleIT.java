package com.fiap.pettrack;

import com.fiap.pettrack.dto.HealthContextDTO;
import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.repository.HealthReportRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="PETTRACK_ORACLE_HEALTH_REPORT_TESTS", matches="true")
class HealthReportOracleIT {
    @Test void migraV5EValidaClobFusoConstraintsEConsultasNoOracle() throws Exception {
        var properties=new Properties();
        try(var in=getClass().getResourceAsStream("/application.properties")){ properties.load(in); }
        var env=new StandardEnvironment();env.getPropertySources().addLast(new PropertiesPropertySource("application",properties));
        String url=env.getRequiredProperty("spring.datasource.url");
        String user=env.getRequiredProperty("spring.datasource.username");
        String password=env.getRequiredProperty("spring.datasource.password");
        var flyway=Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration").target("5").load();
        var pending=flyway.info().pending();
        assertTrue(Arrays.stream(pending).allMatch(m->m.getVersion()!=null && "5".equals(m.getVersion().getVersion())),
                "Abortado: existem migracoes anteriores pendentes; nao executar seeds ou DDL antigo automaticamente");
        assertTrue(Arrays.stream(flyway.info().applied()).anyMatch(m->m.getVersion()!=null && "4".equals(m.getVersion().getVersion())),
                "Abortado: banco deve estar migrado ate V4");
        flyway.migrate();
        assertTrue(Arrays.stream(flyway.info().applied()).anyMatch(m->m.getVersion()!=null && "5".equals(m.getVersion().getVersion())));
        var connectionProperties=new Properties();connectionProperties.setProperty("user",user);connectionProperties.setProperty("password",password);
        connectionProperties.setProperty("oracle.net.CONNECT_TIMEOUT","5000");connectionProperties.setProperty("oracle.jdbc.ReadTimeout","15000");
        try(var connection=DriverManager.getConnection(url,connectionProperties)) {
            connection.setAutoCommit(false);
            try {
                var ds=new SingleConnectionDataSource(connection,true);var jdbc=new JdbcTemplate(ds);
                Long pet=jdbc.queryForObject("SELECT ID_PET FROM TB_PET ORDER BY ID_PET FETCH FIRST 1 ROWS ONLY",Long.class);
                Long usuario=jdbc.queryForObject("SELECT ID_USUARIO FROM TB_USUARIO ORDER BY ID_USUARIO FETCH FIRST 1 ROWS ONLY",Long.class);
                var repo=new HealthReportRepository(new NamedParameterJdbcTemplate(ds),HealthReportFixtures.mapper());
                var contextJson=HealthReportFixtures.mapper().valueToTree(HealthReportFixtures.contexto());
                ((com.fasterxml.jackson.databind.node.ObjectNode)contextJson.get("pet")).put("id",pet);
                var context=HealthReportFixtures.mapper().treeToValue(contextJson,HealthContextDTO.class);
                var r=HealthReportFixtures.resultado();
                var report=new HealthReportDTO(r.idGeracao(),pet,r.geradoEm(),r.contextoGeradoEm(),"mock","simulacao",r.versaoPrompt(),true,true,r.relatorio());
                var salvo=OffsetDateTime.parse("2026-10-28T23:00:00.123456-03:00").truncatedTo(ChronoUnit.MICROS);
                repo.salvar(report,context,usuario,salvo);
                var detail=repo.buscar(pet,report.idGeracao()).orElseThrow();
                assertEquals(report,detail.relatorio());assertEquals(context,detail.contexto());assertEquals(salvo.toInstant(),detail.salvoEm().toInstant());
                assertTrue(repo.listar(pet,0,50).total()>0);assertTrue(repo.ultimo(pet).isPresent());
                assertTrue(repo.buscar(-1L,report.idGeracao()).isEmpty());
                assertThrows(org.springframework.dao.DuplicateKeyException.class,()->repo.salvar(report,context,usuario,salvo));
            } finally { connection.rollback(); }
        }
    }
}
