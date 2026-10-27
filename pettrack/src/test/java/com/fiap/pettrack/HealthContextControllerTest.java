package com.fiap.pettrack;

import com.fiap.pettrack.config.SecurityConfig;
import com.fiap.pettrack.control.HealthContextController;
import com.fiap.pettrack.dto.HealthContextDTO.*;
import com.fiap.pettrack.model.*;
import com.fiap.pettrack.model.enums.PerfilEnum;
import com.fiap.pettrack.repository.*;
import com.fiap.pettrack.service.*;
import com.fiap.pettrack.service.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthContextController.class)
@ContextConfiguration(classes = {HealthContextController.class, SecurityConfig.class, CustomUserDetailsService.class,
        HealthContextService.class, HealthContextControllerTest.TimeConfig.class})
class HealthContextControllerTest {
    @Autowired MockMvc mvc;
    @MockBean IUsuarioRepository usuarios;
    @MockBean IPetRepository pets;
    @MockBean HealthContextRepository contextos;
    @MockBean HealthContextWeatherClient clima;
    private Usuario usuario;
    private final LocalDateTime fim = LocalDateTime.of(2026, 10, 26, 12, 0);

    @TestConfiguration static class TimeConfig {
        @Bean Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @BeforeEach void setup() {
        Tutor tutor = new Tutor(); tutor.setId(10L); tutor.setEmail("privado@example.invalid");
        Pet pet = new Pet(); pet.setId(1L); pet.setNome("Rex"); pet.setTutor(tutor);
        usuario = new Usuario(); usuario.setEmail("tutor@example.invalid");
        usuario.setSenha("nao-deve-ser-serializada"); usuario.setRole(PerfilEnum.ROLE_TUTOR); usuario.setTutor(tutor);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(pets.findById(1L)).thenReturn(Optional.of(pet));
        when(contextos.buscarIot(1L, fim.minusHours(24), fim))
                .thenReturn(new IotResumo(0, 0, 0, null, null, null, null, List.of()));
        when(contextos.buscarUltimosBcs(1L, fim)).thenReturn(List.of());
        when(contextos.buscarHistorico(1L, fim)).thenReturn(new Historico(fim.toLocalDate().minusDays(90), null,
                List.of(), List.of(), fim.toLocalDate().plusDays(30), List.of()));
        when(clima.buscar()).thenReturn(Clima.indisponivel("NAO_CONFIGURADO", ""));
    }

    @Test void sessaoDoTutorRecebeJsonSemEntidadesOuCredenciais() throws Exception {
        mvc.perform(get("/health-context/pets/1").with(user(usuario)))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.pet.id").value(1))
                .andExpect(jsonPath("$.periodoIot.inicioInclusivo").value("2026-10-25T12:00:00"))
                .andExpect(jsonPath("$.bcs.tendencia").value("SEM_HISTORICO_SUFICIENTE"))
                .andExpect(jsonPath("$.pet.tutor").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test void visitanteEEncaminhadoAoLoginAntesDasConsultas() throws Exception {
        mvc.perform(get("/health-context/pets/1"))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", endsWith("/login")));
        verifyNoInteractions(contextos, clima);
    }

    @Test void sessaoDeOutroTutorRecebe403() throws Exception {
        Tutor outro = new Tutor(); outro.setId(11L); usuario.setTutor(outro);
        mvc.perform(get("/health-context/pets/1").with(user(usuario))).andExpect(status().isForbidden());
        verifyNoInteractions(contextos, clima);
    }

    @Test void petInexistenteRetorna404() throws Exception {
        mvc.perform(get("/health-context/pets/99").with(user(usuario))).andExpect(status().isNotFound());
        verifyNoInteractions(contextos, clima);
    }

    @Test void idInvalidoRetorna400() throws Exception {
        mvc.perform(get("/health-context/pets/0").with(user(usuario))).andExpect(status().isBadRequest());
        verifyNoInteractions(contextos, clima);
    }
}
