package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.MedicamentoDTO;
import com.fiap.pettrack.model.Medicamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IMedicamentoMapper {

    @Mapping(target = "idEvento", source = "evento.id")
    MedicamentoDTO toDTO(Medicamento medicamento);

    @Mapping(target = "evento.id", source = "idEvento")
    Medicamento toEntity(MedicamentoDTO medicamentoDTO);
}