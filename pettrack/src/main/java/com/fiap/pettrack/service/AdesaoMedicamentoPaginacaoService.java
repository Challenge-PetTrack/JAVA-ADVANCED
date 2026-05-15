package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.AdesaoMedicamentoDTO;
import com.fiap.pettrack.mapper.IAdesaoMedicamentoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdesaoMedicamentoPaginacaoService {

    @Autowired
    private AdesaoMedicamentoCachingService adesaoMedicamentoCachingService;

    @Autowired
    private IAdesaoMedicamentoMapper iAdesaoMedicamentoMapper;

    @Transactional(readOnly = true)
    public Page<AdesaoMedicamentoDTO> paginar(PageRequest pageRequest) {
        return adesaoMedicamentoCachingService.findAll(pageRequest)
                .map(iAdesaoMedicamentoMapper::toDTO);
    }
}