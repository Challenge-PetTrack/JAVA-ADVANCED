package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.MedicamentoDTO;
import com.fiap.pettrack.mapper.IMedicamentoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicamentoPaginacaoService {

    @Autowired
    private MedicamentoCachingService medicamentoCachingService;

    @Autowired
    private IMedicamentoMapper iMedicamentoMapper;

    @Transactional(readOnly = true)
    public Page<MedicamentoDTO> paginar(PageRequest pageRequest) {
        return medicamentoCachingService.findAll(pageRequest)
                .map(iMedicamentoMapper::toDTO);
    }
}