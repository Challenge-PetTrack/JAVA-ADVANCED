package com.fiap.pettrack.control;

import com.fiap.pettrack.dto.PetDTO;
import com.fiap.pettrack.mapper.IPetMapper;
import com.fiap.pettrack.model.Pet;
import com.fiap.pettrack.model.enums.SexoPetEnum;
import com.fiap.pettrack.repository.IPetRepository;
import com.fiap.pettrack.service.PetCachingService;
import com.fiap.pettrack.service.PetPaginacaoService;
import com.fiap.pettrack.service.PetProcedureService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/pet")
public class PetController {

    @Autowired
    private IPetRepository repository;
    @Autowired
    private IPetMapper mapper;
    @Autowired
    private PetCachingService cachingService;
    @Autowired
    private PetPaginacaoService paginacaoService;
    @Autowired
    private PetProcedureService procedureService;

    @Operation(description = "Retorna todos os pets", summary = "Retorna PetDTO", tags = "Retorno de Informações")
    @GetMapping("/todos")
    public ResponseEntity<List<PetDTO>> retornarTodos() {
        return ResponseEntity.ok(cachingService.findAll().stream()
                .map(mapper::toDTO).collect(Collectors.toList()));
    }

    @Operation(description = "Retorna todos os pets paginados", summary = "Retorna PetDTO paginado", tags = "Retorno de Informações")
    @GetMapping("/paginar")
    public ResponseEntity<Page<PetDTO>> paginar(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        PageRequest request = PageRequest.of(page, size);
        return ResponseEntity.ok(paginacaoService.paginar(request).map(mapper::toDTO));
    }

    @Operation(description = "Retorna Pet por ID", summary = "Retorna Pet por ID", tags = "Retorno de Informações")
    @GetMapping("/{id}")
    public ResponseEntity<PetDTO> retornarPorId(@PathVariable Long id) {
        Optional<Pet> op = cachingService.findById(id);
        if (op.isPresent()) {
            return ResponseEntity.ok(mapper.toDTO(op.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Retorna pets por clínica", summary = "Retorna pets por clínica", tags = "Retorno de Informações")
    @GetMapping("/clinica/{idClinica}")
    public ResponseEntity<List<PetDTO>> retornarPorClinica(@PathVariable Long idClinica) {
        return ResponseEntity.ok(cachingService.findByClinicaId(idClinica).stream()
                .map(mapper::toDTO).collect(Collectors.toList()));
    }

    @Operation(description = "Retorna pets por sexo", summary = "Retorna pets por sexo", tags = "Retorno de Informações")
    @GetMapping("/sexo")
    public ResponseEntity<List<PetDTO>> retornarPorSexo(@RequestParam SexoPetEnum sexo) {
        return ResponseEntity.ok(cachingService.findBySexo(sexo).stream()
                .map(mapper::toDTO).collect(Collectors.toList()));
    }

    @Operation(description = "Retorna pets por nome ou espécie", summary = "Busca pets", tags = "Retorno de Informações")
    @GetMapping("/buscar")
    public ResponseEntity<List<PetDTO>> buscarPorNomeOuEspecie(@RequestParam String busca) {
        return ResponseEntity.ok(cachingService.buscarPorNomeOuEspecie(busca).stream()
                .map(mapper::toDTO).collect(Collectors.toList()));
    }

    @Operation(description = "Retorna pets com alertas pendentes", summary = "Pets com alertas", tags = "Retorno de Informações")
    @GetMapping("/alertasPendentes")
    public ResponseEntity<List<PetDTO>> buscarPetsComAlertasPendentes() {
        return ResponseEntity.ok(cachingService.buscarPetsComAlertasPendentes().stream()
                .map(mapper::toDTO).collect(Collectors.toList()));
    }

    @Operation(description = "Inserir novo Pet", summary = "Inserir Pet", tags = "Inserção de Informações")
    @PostMapping("/novo")
    public ResponseEntity<PetDTO> inserirPet(@RequestBody @Valid PetDTO petDTO) {
        try {
            Pet salvo = procedureService.inserir(mapper.toEntity(petDTO));
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(salvo));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Dados rejeitados pelo Oracle.", e);
        }
    }

    @Operation(summary = "Exporta pets do Oracle para JSON por procedure empacotada", tags = "Sprint 4 - Banco de Dados")
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    @GetMapping(value = "/exportar-json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exportarJson(@RequestParam(required = false) String especie) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pets.json\"")
                .contentType(MediaType.APPLICATION_JSON).body(procedureService.exportarJson(especie));
    }

    @Operation(summary = "Executa o relatorio manual de subtotais do Oracle", tags = "Sprint 4 - Banco de Dados")
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    @GetMapping(value = "/relatorio-atividades", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> relatorioAtividades() {
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(procedureService.relatorioAtividades());
    }

    @Operation(description = "Remover Pet", summary = "Remover Pet", tags = "Remoção de Informações")
    @DeleteMapping("/remover/{id}")
    public ResponseEntity<Void> removerPet(@PathVariable Long id) {
        Optional<Pet> op = repository.findById(id);
        if (op.isPresent()) {
            repository.delete(op.get());
            cachingService.removerCache();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(description = "Atualizar Pet", summary = "Atualizar Pet", tags = "Atualização de Informações")
    @PutMapping("/atualizar/{id}")
    public ResponseEntity<PetDTO> atualizarPet(
            @PathVariable Long id,
            @RequestBody @Valid PetDTO petDTO) {
        Optional<Pet> op = cachingService.findById(id);
        if (op.isPresent()) {
            Pet petAntigo = op.get();
            Pet petNovo = mapper.toEntity(petDTO);
            petAntigo.transferir(petNovo);
            repository.save(petAntigo);
            cachingService.removerCache();
            return ResponseEntity.status(HttpStatus.OK).body(mapper.toDTO(petAntigo));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
