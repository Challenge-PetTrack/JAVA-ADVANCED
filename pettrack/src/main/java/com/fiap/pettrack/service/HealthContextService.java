package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.HealthContextDTO;
import com.fiap.pettrack.dto.HealthContextDTO.*;
import com.fiap.pettrack.model.Pet;
import com.fiap.pettrack.model.Usuario;
import com.fiap.pettrack.repository.HealthContextRepository;
import com.fiap.pettrack.repository.IPetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HealthContextService {
    private final IPetRepository pets;
    private final HealthContextRepository contextos;
    private final HealthContextWeatherClient climaClient;
    private final Clock clock;

    public HealthContextDTO montar(Long idPet, Usuario usuario) {
        if (usuario == null || !usuario.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao necessaria");
        }
        if (idPet == null || idPet <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID do pet deve ser positivo");
        }
        Pet pet = pets.findById(idPet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet nao encontrado"));
        if (!podeAcessar(usuario, pet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem acesso a este pet");
        }
        OffsetDateTime agora = OffsetDateTime.now(clock).withNano(0);
        var fim = agora.toLocalDateTime();
        var inicio = fim.minusHours(24);
        var iot = contextos.buscarIot(idPet, inicio, fim);
        var avaliacoes = contextos.buscarUltimosBcs(idPet, fim);
        var ultima = avaliacoes.isEmpty() ? null : avaliacoes.get(0);
        var anterior = avaliacoes.size() < 2 ? null : avaliacoes.get(1);
        String tendencia = anterior == null ? "SEM_HISTORICO_SUFICIENTE"
                : ultima.score().equals(anterior.score()) ? "ESTAVEL"
                : ultima.score() > anterior.score() ? "SUBINDO" : "DESCENDO";
        var historico = contextos.buscarHistorico(idPet, fim);
        var clima = climaClient.buscar();
        var avisos = new ArrayList<String>();
        if (iot.totalLeituras() == 0) avisos.add("Sem leituras IoT nas ultimas 24 horas; medias ausentes nao significam zero.");
        if (iot.temperaturasValidas() < iot.totalLeituras() || iot.atividadesValidas() < iot.totalLeituras()) {
            avisos.add("Leituras nulas ou fora do contrato foram excluidas das respectivas medias.");
        }
        if (ultima == null) avisos.add("Sem avaliacao BCS valida registrada ate o momento da consulta.");
        if (anterior == null) avisos.add("Tendencia BCS indisponivel: sao necessarias duas avaliacoes validas.");
        if (historico.eventosRecentes().isEmpty()) avisos.add("Sem eventos clinicos registrados no periodo de 90 dias.");
        avisos.add("Ausencia de medicamentos ou vacinas na consulta nao comprova ausencia de tratamento ou vacinacao em dia.");
        if (!"DISPONIVEL".equals(clima.status())) avisos.add("Clima indisponivel: " + clima.status() + ".");
        else avisos.add("Clima da localidade configurada: " + clima.localidade() + "; nao representa geolocalizacao do pet.");
        Double idade = naoNegativo(pet.getIdade());
        Double peso = naoNegativo(pet.getPeso());
        if (peso != null && peso == 0) peso = null;
        if (idade == null || peso == null || pet.getRaca() == null || pet.getRaca().isBlank()) {
            avisos.add("Perfil incompleto: idade, peso ou raca ausentes/invalidos nao foram estimados.");
        }
        return new HealthContextDTO("1.0", agora, clock.getZone().getId(),
                new PerfilPet(pet.getId(), pet.getNome(), pet.getEspecie(), pet.getRaca(), idade, peso),
                new Periodo(inicio, fim), iot, new BcsResumo(ultima, anterior, tendencia),
                historico, clima, List.copyOf(avisos));
    }

    private boolean podeAcessar(Usuario usuario, Pet pet) {
        if (usuario.getRole() == null) return false;
        return switch (usuario.getRole()) {
            case ROLE_ADMIN -> true;
            case ROLE_TUTOR -> usuario.getTutor() != null && pet.getTutor() != null
                    && usuario.getTutor().getId() != null
                    && Objects.equals(usuario.getTutor().getId(), pet.getTutor().getId());
            case ROLE_VET -> usuario.getClinica() != null && pet.getClinica() != null
                    && usuario.getClinica().getId() != null
                    && Objects.equals(usuario.getClinica().getId(), pet.getClinica().getId());
        };
    }

    private Double naoNegativo(Double valor) {
        return valor == null || !Double.isFinite(valor) || valor < 0 ? null : valor;
    }
}
