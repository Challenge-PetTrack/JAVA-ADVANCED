package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.CollarLeituraDTO;
import com.fiap.pettrack.mapper.ICollarLeituraMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollarLeituraPaginacaoService {

    @Autowired
    private CollarLeituraCachingService collarLeituraCachingService;

    @Autowired
    private ICollarLeituraMapper iCollarLeituraMapper;

    @Transactional(readOnly = true)
    public Page<CollarLeituraDTO> paginar(PageRequest pageRequest) {
        return collarLeituraCachingService.findAll(pageRequest)
                .map(iCollarLeituraMapper::toDTO);
    }
}