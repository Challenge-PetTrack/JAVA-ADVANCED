package com.fiap.pettrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.pettrack.model.Clinica;
import com.fiap.pettrack.model.Pet;
import com.fiap.pettrack.model.Tutor;
import com.fiap.pettrack.repository.IPetRepository;
import com.fiap.pettrack.repository.PetProcedureRepository;
import com.fiap.pettrack.service.PetCachingService;
import com.fiap.pettrack.service.PetProcedureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PetProcedureServiceTest {
    private PetProcedureRepository procedures;
    private IPetRepository pets;
    private PetCachingService cache;
    private PetProcedureService service;

    @BeforeEach void setup() {
        procedures = mock(PetProcedureRepository.class);
        pets = mock(IPetRepository.class);
        cache = mock(PetCachingService.class);
        service = new PetProcedureService(procedures, pets, cache, new ObjectMapper());
    }

    @Test void cadastroConfirmaIdGeradoELimpaCache() {
        Pet entrada = pet();
        Pet salvo = pet(); salvo.setId(42L);
        when(procedures.inserir(entrada)).thenReturn(42L);
        when(pets.findById(42L)).thenReturn(Optional.of(salvo));
        assertSame(salvo, service.inserir(entrada));
        verify(cache).removerCache();
        verify(pets, never()).save(any());
    }

    @Test void cadastroExigeRelacionamentosAntesDeChamarBanco() {
        assertThrows(ResponseStatusException.class, () -> service.inserir(new Pet()));
        verifyNoInteractions(procedures, pets, cache);
    }

    @Test void erroDoOracleNaoRetornaSucessoNemLimpaCache() {
        when(procedures.inserir(any())).thenThrow(new DataIntegrityViolationException("FK"));
        assertThrows(DataIntegrityViolationException.class, () -> service.inserir(pet()));
        verifyNoInteractions(pets, cache);
    }

    @Test void exportacaoRejeitaTextoDeConsoleEObjetosDeErro() {
        when(procedures.exportarJson(null)).thenReturn("CABECALHO\n{\"idPet\":1}");
        assertThrows(IllegalStateException.class, () -> service.exportarJson(null));
        when(procedures.exportarJson(null)).thenReturn("[{\"erro\":\"Peso invalido\"}]");
        assertThrows(IllegalStateException.class, () -> service.exportarJson(null));
    }

    @Test void exportacaoAceitaDocumentosComRelacionamentosEVazio() {
        String json = "[{\"idPet\":1,\"nome\":\"Thor\",\"tutor\":{\"id\":2},\"clinica\":{\"id\":3}}]";
        when(procedures.exportarJson(null)).thenReturn(json);
        assertEquals(json, service.exportarJson(null));
        when(procedures.exportarJson(null)).thenReturn("[]");
        assertEquals("[]", service.exportarJson(null));
    }

    @Test void filtroInvalidoNaoChegaAoBanco() {
        assertThrows(ResponseStatusException.class, () -> service.exportarJson("x"));
        verifyNoInteractions(procedures);
    }

    private Pet pet() {
        Pet p = new Pet(); p.setNome("Teste"); p.setEspecie("Cao");
        Tutor tutor = new Tutor(); tutor.setId(1L); p.setTutor(tutor);
        Clinica clinica = new Clinica(); clinica.setId(1L); p.setClinica(clinica);
        return p;
    }
}
