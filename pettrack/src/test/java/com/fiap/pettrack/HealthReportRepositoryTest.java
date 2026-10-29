package com.fiap.pettrack;

import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.repository.HealthReportRepository;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class HealthReportRepositoryTest {
    private SingleConnectionDataSource ds;
    private JdbcTemplate jdbc;
    private HealthReportRepository repository;
    private final OffsetDateTime salvo = OffsetDateTime.parse("2026-10-28T21:00:00.123456-03:00");

    @BeforeEach void setup() {
        ds = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=Oracle", "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__criar_tabelas_pettrack.sql")).execute(ds);
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE TB_USUARIO (ID_USUARIO NUMBER PRIMARY KEY)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__health_reports.sql")).execute(ds);
        jdbc.update("INSERT INTO TB_USUARIO VALUES (1)");
        jdbc.update("INSERT INTO TB_TUTOR (ID_TUTOR,NM_TUTOR,DS_EMAIL) VALUES (1,'Tutor','teste@example.invalid')");
        jdbc.update("INSERT INTO TB_CLINICA (ID_CLINICA,NM_CLINICA,NR_CNPJ) VALUES (1,'Clinica','00000000000000')");
        for (int id=1; id<=2; id++) jdbc.update("INSERT INTO TB_PET (ID_PET,NM_PET,DS_ESPECIE,ID_TUTOR,ID_CLINICA) VALUES (?,'Pet','Cao',1,1)",id);
        repository = new HealthReportRepository(new NamedParameterJdbcTemplate(ds), HealthReportFixtures.mapper());
    }

    @AfterEach void fechar() { ds.destroy(); }

    @Test void salvaClobsMetadadosEFusoSemPerderContexto() {
        var report = HealthReportFixtures.resultado().comoPersistido();
        repository.salvar(report, HealthReportFixtures.contexto(), 1L, salvo);
        var detail = repository.buscar(1L, report.idGeracao()).orElseThrow();
        assertEquals(report, detail.relatorio());
        assertEquals(HealthReportFixtures.contexto(), detail.contexto());
        assertEquals(salvo, detail.salvoEm());
        assertEquals(1L, detail.solicitadoPor());
        assertTrue(detail.relatorio().persistido());
    }

    @Test void paginaOrdenaComDesempateEstavelEIsolaPets() {
        var a = comId(1, "00000000-0000-0000-0000-000000000001");
        var b = comId(1, "00000000-0000-0000-0000-000000000002");
        var c = comId(1, "00000000-0000-0000-0000-000000000003");
        repository.salvar(a, HealthReportFixtures.contexto(),1L,salvo);
        repository.salvar(b, HealthReportFixtures.contexto(),1L,salvo);
        repository.salvar(c, HealthReportFixtures.contexto(),1L,salvo.plusSeconds(1));
        repository.salvar(comId(2, UUID.randomUUID().toString()),HealthReportFixtures.contexto(),1L,salvo.plusDays(1));
        var pagina=repository.listar(1L,0,2);
        assertEquals(3,pagina.total());
        assertEquals(java.util.List.of(c.idGeracao(),b.idGeracao()),pagina.itens().stream().map(r->r.idGeracao()).toList());
        assertEquals(a.idGeracao(),repository.listar(1L,1,2).itens().getFirst().idGeracao());
        assertEquals(c.idGeracao(),repository.ultimo(1L).orElseThrow().relatorio().idGeracao());
        assertTrue(repository.buscar(2L,a.idGeracao()).isEmpty());
    }

    @Test void duplicataNaoSobrescreveHistorico() {
        var report=HealthReportFixtures.resultado().comoPersistido();
        repository.salvar(report,HealthReportFixtures.contexto(),1L,salvo);
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->repository.salvar(report,HealthReportFixtures.contexto(),1L,salvo.plusHours(1)));
        assertEquals(salvo,repository.buscar(1L,report.idGeracao()).orElseThrow().salvoEm());
        assertEquals(1,repository.listar(1L,0,20).total());
    }

    @Test void falhaDeChaveEstrangeiraNaoCriaRegistroParcial() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->repository.salvar(HealthReportFixtures.resultado().comoPersistido(),HealthReportFixtures.contexto(),999L,salvo));
        assertEquals(0,repository.listar(1L,0,20).total());
    }

    @Test void rollbackDesfazGravacaoEConfirmacaoMantemRegistro() {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var report=HealthReportFixtures.resultado().comoPersistido();
        assertThrows(IllegalStateException.class,()->tx.execute(status->{repository.salvar(report,HealthReportFixtures.contexto(),1L,salvo);throw new IllegalStateException();}));
        assertTrue(repository.buscar(1L,report.idGeracao()).isEmpty());
        tx.executeWithoutResult(status->repository.salvar(report,HealthReportFixtures.contexto(),1L,salvo));
        assertTrue(repository.buscar(1L,report.idGeracao()).isPresent());
    }

    @Test void vazioRetornaPaginaVaziaESemUltimo() {
        assertEquals(0,repository.listar(2L,0,20).total());
        assertTrue(repository.listar(2L,0,20).itens().isEmpty());
        assertTrue(repository.ultimo(2L).isEmpty());
    }

    @Test void mockContinuaIdentificadoNoHistorico() {
        var r=HealthReportFixtures.resultado();
        var mock=new HealthReportDTO(r.idGeracao(),r.idPet(),r.geradoEm(),r.contextoGeradoEm(),"mock","simulacao",r.versaoPrompt(),true,true,r.relatorio());
        repository.salvar(mock,HealthReportFixtures.contexto(),1L,salvo);
        assertTrue(repository.listar(1L,0,20).itens().getFirst().simulado());
        assertEquals("mock",repository.buscar(1L,r.idGeracao()).orElseThrow().relatorio().provedor());
    }

    @Test void clobMaiorQueVarcharPreservaUnicode() {
        var r=HealthReportFixtures.resultado();
        var texto="Atenção à saúde e evolução do cão. ".repeat(300);
        var payload=new HealthReportDTO(r.idGeracao(),r.idPet(),r.geradoEm(),r.contextoGeradoEm(),r.provedor(),r.modelo(),r.versaoPrompt(),false,true,
            new HealthReportDTO.Relatorio(r.relatorio().resumo(),r.relatorio().nivelAtencao(),r.relatorio().motivos(),r.relatorio().recomendacoes(),java.util.List.of(texto)));
        repository.salvar(payload,HealthReportFixtures.contexto(),1L,salvo);
        assertEquals(texto,repository.buscar(1L,r.idGeracao()).orElseThrow().relatorio().relatorio().limitacoes().getFirst());
    }

    @Test void fluxoGerarSalvarConsultarPreservaSnapshotMesmoComIaOffline() {
        var contextos=org.mockito.Mockito.mock(com.fiap.pettrack.service.HealthContextService.class);
        var client=org.mockito.Mockito.mock(com.fiap.pettrack.service.HealthReportClient.class);
        var usuario=new com.fiap.pettrack.model.Usuario();usuario.setId(1L);
        org.mockito.Mockito.when(contextos.montar(1L,usuario)).thenReturn(HealthReportFixtures.contexto());
        org.mockito.Mockito.when(client.gerar(HealthReportFixtures.contexto())).thenReturn(HealthReportFixtures.resultado());
        var storage=new com.fiap.pettrack.service.HealthReportStorageService(repository,contextos,
            java.time.Clock.fixed(salvo.toInstant(),java.time.ZoneOffset.UTC));
        var service=new com.fiap.pettrack.service.HealthReportService(contextos,client,storage,repository);
        var gerado=service.gerar(1L,usuario);
        assertTrue(gerado.persistido());
        jdbc.update("UPDATE TB_PET SET NM_PET='Cadastro alterado' WHERE ID_PET=1");
        org.mockito.Mockito.when(client.gerar(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("IA offline"));
        var recuperado=service.buscar(1L,gerado.idGeracao(),usuario);
        assertEquals(gerado,recuperado.relatorio());
        assertEquals(HealthReportFixtures.contexto(),recuperado.contexto());
        assertEquals(gerado.idGeracao(),service.ultimo(1L,usuario).relatorio().idGeracao());
        assertEquals(1,service.listar(1L,0,20,usuario).total());
        org.mockito.Mockito.verify(client,org.mockito.Mockito.times(1)).gerar(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(contextos,org.mockito.Mockito.times(1)).montar(1L,usuario);
    }

    private HealthReportDTO comId(long pet,String uuid) {
        var r=HealthReportFixtures.resultado();
        return new HealthReportDTO(UUID.fromString(uuid),pet,r.geradoEm(),r.contextoGeradoEm(),r.provedor(),r.modelo(),r.versaoPrompt(),false,true,r.relatorio());
    }
}
