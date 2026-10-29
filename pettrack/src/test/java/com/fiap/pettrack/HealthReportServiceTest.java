package com.fiap.pettrack;

import com.fiap.pettrack.model.Usuario;
import com.fiap.pettrack.repository.HealthReportRepository;
import com.fiap.pettrack.service.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthReportServiceTest {
    private HealthContextService contextos;
    private HealthReportClient client;
    private HealthReportRepository repository;
    private HealthReportStorageService storage;
    private HealthReportService service;
    private Usuario usuario;

    @BeforeEach void setup() {
        contextos=mock(HealthContextService.class);client=mock(HealthReportClient.class);repository=mock(HealthReportRepository.class);
        storage=spy(new HealthReportStorageService(repository,contextos,Clock.fixed(Instant.parse("2026-10-28T23:00:00Z"), ZoneOffset.UTC)));
        service=new HealthReportService(contextos,client,storage,repository);
        usuario=new Usuario();usuario.setId(1L);
        when(contextos.montar(1L,usuario)).thenReturn(HealthReportFixtures.contexto());
    }

    @Test void geraAntesDeAbrirPersistenciaERevalidaAcesso() {
        var report=HealthReportFixtures.resultado();when(client.gerar(any())).thenReturn(report);
        assertTrue(service.gerar(1L,usuario).persistido());
        var ordem=inOrder(contextos,client,repository);
        ordem.verify(contextos).montar(1L,usuario);
        ordem.verify(client).gerar(HealthReportFixtures.contexto());
        ordem.verify(contextos).autorizar(1L,usuario);
        ordem.verify(repository).salvar(eq(report.comoPersistido()),eq(HealthReportFixtures.contexto()),eq(1L),any());
    }

    @Test void usuarioSemIdNaoGera() {
        usuario.setId(null);
        assertEquals(401,assertThrows(ResponseStatusException.class,()->service.gerar(1L,usuario)).getStatusCode().value());
        verifyNoInteractions(client,repository);
    }

    @Test void falhaDaIaNaoGravaNemSimula() {
        when(client.gerar(any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY));
        assertThrows(ResponseStatusException.class,()->service.gerar(1L,usuario));
        verifyNoInteractions(storage,repository);
    }

    @Test void perdaDeAcessoDuranteGeracaoImpedePersistencia() {
        when(client.gerar(any())).thenReturn(HealthReportFixtures.resultado());
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(contextos).autorizar(1L,usuario);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.gerar(1L,usuario)).getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test void falhaDeBancoNaoVazaDadosNemRetornaSucesso() {
        when(client.gerar(any())).thenReturn(HealthReportFixtures.resultado());
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("senha-secreta")).when(repository).salvar(any(),any(),any(),any());
        var ex=assertThrows(ResponseStatusException.class,()->service.gerar(1L,usuario));
        assertEquals(503,ex.getStatusCode().value());assertFalse(ex.getMessage().contains("senha-secreta"));
        verify(client,times(1)).gerar(any());
    }

    @Test void duplicataRetornaConflitoSemNovaChamadaAoProvedor() {
        when(client.gerar(any())).thenReturn(HealthReportFixtures.resultado());
        doThrow(new org.springframework.dao.DuplicateKeyException("uuid")).when(repository).salvar(any(),any(),any(),any());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.gerar(1L,usuario)).getStatusCode().value());
        verify(client,times(1)).gerar(any());
    }

    @Test void leituraDoHistoricoNaoDependeDeGeminiOuClima() {
        var r=HealthReportFixtures.resultado();when(repository.buscar(1L,r.idGeracao())).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.buscar(1L,r.idGeracao(),usuario)).getStatusCode().value());
        verify(contextos).autorizar(1L,usuario);verifyNoInteractions(client,storage);
        verify(contextos,never()).montar(any(),any());
    }

    @Test void falhaDeConsultaEhSanitizada() {
        when(repository.ultimo(1L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("segredo"));
        var ex=assertThrows(ResponseStatusException.class,()->service.ultimo(1L,usuario));
        assertEquals(503,ex.getStatusCode().value());assertFalse(ex.getMessage().contains("segredo"));
    }
}
