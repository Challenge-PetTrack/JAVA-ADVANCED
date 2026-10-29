package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.HealthReportDTO;
import com.fiap.pettrack.dto.HealthReportHistoryDTO.*;
import com.fiap.pettrack.model.Usuario;
import com.fiap.pettrack.repository.HealthReportRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthReportService {
    private final HealthContextService contextos;
    private final HealthReportClient client;
    private final HealthReportStorageService storage;
    private final HealthReportRepository repository;

    public HealthReportDTO gerar(Long idPet, Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado");
        }
        var contexto = contextos.montar(idPet, usuario);
        var report = client.gerar(contexto);
        try { return storage.salvar(report, contexto, usuario); }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Geracao ja registrada");
        } catch (DataAccessException ex) { throw indisponivel(); }
    }

    public Pagina listar(Long pet, int pagina, int tamanho, Usuario usuario) {
        contextos.autorizar(pet, usuario);
        if (pagina < 0 || tamanho < 1 || tamanho > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagina deve ser positiva ou zero e tamanho entre 1 e 50");
        }
        try { return repository.listar(pet, pagina, tamanho); }
        catch (DataAccessException ex) { throw indisponivel(); }
    }

    public Detalhe buscar(Long pet, UUID id, Usuario usuario) {
        contextos.autorizar(pet, usuario);
        try { return repository.buscar(pet, id).orElseThrow(this::ausente); }
        catch (DataAccessException ex) { throw indisponivel(); }
    }

    public Detalhe ultimo(Long pet, Usuario usuario) {
        contextos.autorizar(pet, usuario);
        try { return repository.ultimo(pet).orElseThrow(this::ausente); }
        catch (DataAccessException ex) { throw indisponivel(); }
    }

    private ResponseStatusException ausente() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Relatorio nao encontrado para este pet");
    }

    private ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Historico de relatorios indisponivel; operacao nao confirmada");
    }
}
