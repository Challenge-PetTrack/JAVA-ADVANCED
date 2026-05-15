package com.fiap.pettrack.mapper;

import com.fiap.pettrack.dto.AdesaoMedicamentoDTO;
import com.fiap.pettrack.model.AdesaoMedicamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IAdesaoMedicamentoMapper {

    @Mapping(target = "idMedicamento", source = "medicamento.id")
    AdesaoMedicamentoDTO toDTO(AdesaoMedicamento adesao);

    @Mapping(target = "medicamento.id", source = "idMedicamento")
    AdesaoMedicamento toEntity(AdesaoMedicamentoDTO adesaoDTO);
}