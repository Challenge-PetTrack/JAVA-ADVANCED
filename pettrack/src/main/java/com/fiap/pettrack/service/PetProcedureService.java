package com.fiap.pettrack.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.pettrack.model.Pet;
import com.fiap.pettrack.repository.IPetRepository;
import com.fiap.pettrack.repository.PetProcedureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PetProcedureService {
    private final PetProcedureRepository procedures;
    private final IPetRepository pets;
    private final PetCachingService cache;
    private final ObjectMapper json;

    public PetProcedureService(PetProcedureRepository procedures, IPetRepository pets,
                               PetCachingService cache, ObjectMapper json) {
        this.procedures = procedures;
        this.pets = pets;
        this.cache = cache;
        this.json = json;
    }

    @Transactional
    public Pet inserir(Pet pet) {
        if (pet.getTutor() == null || pet.getTutor().getId() == null ||
                pet.getClinica() == null || pet.getClinica().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe tutor e clinica existentes.");
        }
        long id = procedures.inserir(pet);
        Pet salvo = pets.findById(id).orElseThrow(() ->
                new IllegalStateException("Insercao nao confirmada no Oracle."));
        cache.removerCache();
        return salvo;
    }

    @Transactional(readOnly = true)
    public String exportarJson(String especie) {
        if (especie != null && especie.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de especie invalido.");
        }
        String dataset = procedures.exportarJson(especie);
        try {
            JsonNode documentos = json.readTree(dataset);
            if (documentos == null || !documentos.isArray()) throw new IllegalStateException("Dataset nao e uma lista JSON.");
            for (JsonNode documento : documentos) {
                if (!documento.path("idPet").isIntegralNumber() || !documento.path("nome").isTextual() ||
                        !documento.path("tutor").path("id").isIntegralNumber() ||
                        !documento.path("clinica").path("id").isIntegralNumber() || documento.has("erro")) {
                    throw new IllegalStateException("Dataset possui documento incompleto.");
                }
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("O Oracle retornou JSON invalido.", e);
        }
        return dataset;
    }

    @Transactional(readOnly = true)
    public String relatorioAtividades() {
        return procedures.relatorioAtividades();
    }
}
