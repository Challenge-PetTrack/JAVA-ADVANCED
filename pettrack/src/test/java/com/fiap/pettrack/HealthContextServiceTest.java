package com.fiap.pettrack;

import com.fiap.pettrack.dto.HealthContextDTO.*;
import com.fiap.pettrack.model.*;
import com.fiap.pettrack.model.enums.PerfilEnum;
import com.fiap.pettrack.repository.HealthContextRepository;
import com.fiap.pettrack.repository.IPetRepository;
import com.fiap.pettrack.service.HealthContextService;
import com.fiap.pettrack.service.HealthContextWeatherClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthContextServiceTest {
    private IPetRepository pets;
    private HealthContextRepository repository;
    private HealthContextWeatherClient clima;
    private HealthContextService service;
    private Pet pet;
    private Usuario usuario;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final LocalDateTime fim = LocalDateTime.of(2026, 10, 26, 12, 0);

    @BeforeEach void setup() {
        pets = mock(IPetRepository.class);
        repository = mock(HealthContextRepository.class);
        clima = mock(HealthContextWeatherClient.class);
        service = new HealthContextService(pets, repository, clima, clock);
        Tutor tutor = new Tutor(); tutor.setId(10L);
        Clinica clinica = new Clinica(); clinica.setId(20L);
        pet = new Pet(); pet.setId(1L); pet.setNome("Rex"); pet.setEspecie("Cao");
        pet.setTutor(tutor); pet.setClinica(clinica);
        usuario = new Usuario(); usuario.setRole(PerfilEnum.ROLE_TUTOR); usuario.setTutor(tutor);
        when(pets.findById(1L)).thenReturn(Optional.of(pet));
        when(repository.buscarIot(1L, fim.minusHours(24), fim))
                .thenReturn(new IotResumo(0, 0, 0, null, null, null, null, List.of()));
        when(repository.buscarUltimosBcs(1L, fim)).thenReturn(List.of());
        when(repository.buscarHistorico(1L, fim)).thenReturn(new Historico(fim.toLocalDate().minusDays(90), null,
                List.of(), List.of(), fim.toLocalDate().plusDays(30), List.of()));
        when(clima.buscar()).thenReturn(Clima.indisponivel("NAO_CONFIGURADO", ""));
    }

    @Test void contextoVazioExplicitaAusenciasSemInventarDados() {
        var result = service.montar(1L, usuario);
        assertEquals("America/Sao_Paulo", result.fusoHorario());
        assertEquals(fim.minusHours(24), result.periodoIot().inicioInclusivo());
        assertEquals("SEM_HISTORICO_SUFICIENTE", result.bcs().tendencia());
        assertNull(result.bcs().ultima());
        assertNull(result.pet().pesoKg());
        assertTrue(result.avisos().stream().anyMatch(s -> s.contains("Sem leituras IoT")));
        assertEquals("NAO_CONFIGURADO", result.clima().status());
    }

    @ParameterizedTest
    @CsvSource({"6,5,SUBINDO", "5,5,ESTAVEL", "4,5,DESCENDO"})
    void tendenciaUsaDuasAvaliacoesPersistidas(int atual, int anterior, String esperado) {
        when(repository.buscarUltimosBcs(1L, fim)).thenReturn(List.of(
                new AvaliacaoBcs(2L, atual, "Atual", fim.minusHours(1)),
                new AvaliacaoBcs(1L, anterior, "Anterior", fim.minusDays(10))));
        assertEquals(esperado, service.montar(1L, usuario).bcs().tendencia());
    }

    @Test void umaAvaliacaoNaoPermiteInferirTendencia() {
        when(repository.buscarUltimosBcs(1L, fim)).thenReturn(List.of(new AvaliacaoBcs(1L, 5, null, fim.minusDays(1))));
        assertEquals("SEM_HISTORICO_SUFICIENTE", service.montar(1L, usuario).bcs().tendencia());
    }

    @Test void outroTutorNaoConsultaDadosNemClima() {
        Tutor outro = new Tutor(); outro.setId(11L); usuario.setTutor(outro);
        var erro = assertThrows(ResponseStatusException.class, () -> service.montar(1L, usuario));
        assertEquals(403, erro.getStatusCode().value());
        verifyNoInteractions(repository, clima);
    }

    @Test void veterinarioSoAcessaClinicaVinculada() {
        usuario.setRole(PerfilEnum.ROLE_VET);
        Clinica outra = new Clinica(); outra.setId(21L); usuario.setClinica(outra);
        assertThrows(ResponseStatusException.class, () -> service.montar(1L, usuario));
        verifyNoInteractions(repository, clima);
        usuario.setClinica(pet.getClinica());
        assertEquals(1L, service.montar(1L, usuario).pet().id());
    }

    @Test void adminPodeConsultarPetSemVinculoPessoal() {
        usuario.setRole(PerfilEnum.ROLE_ADMIN); usuario.setTutor(null);
        assertEquals(1L, service.montar(1L, usuario).pet().id());
    }

    @Test void usuarioAusenteInativoOuSemVinculoNaoAcessa() {
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> service.montar(1L, null)).getStatusCode().value());
        usuario.setAtivo("N");
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> service.montar(1L, usuario)).getStatusCode().value());
        usuario.setAtivo("S"); usuario.setTutor(null);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.montar(1L, usuario)).getStatusCode().value());
        verifyNoInteractions(repository, clima);
    }

    @Test void petInexistenteEIdInvalidoRetornamErrosDistintos() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.montar(-1L, usuario)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.montar(99L, usuario)).getStatusCode().value());
        verifyNoInteractions(repository, clima);
    }

    @Test void perfilInvalidoNaoEnviaNumerosInventadosOuNaoFinitos() {
        pet.setIdade(Double.NaN); pet.setPeso(-1.0);
        var result = service.montar(1L, usuario);
        assertNull(result.pet().idadeAnos());
        assertNull(result.pet().pesoKg());
    }
}
