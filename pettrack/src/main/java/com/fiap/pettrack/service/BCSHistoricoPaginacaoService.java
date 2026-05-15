package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.BCSHistoricoDTO;
import com.fiap.pettrack.mapper.IBCSHistoricoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BCSHistoricoPaginacaoService {

    @Autowired
    private BCSHistoricoCachingService bcsHistoricoCachingService;

    @Autowired
    private IBCSHistoricoMapper iBCSHistoricoMapper;

    @Transactional(readOnly = true)
    public Page<BCSHistoricoDTO> paginar(PageRequest pageRequest) {
        return bcsHistoricoCachingService.findAll(pageRequest)
                .map(iBCSHistoricoMapper::toDTO);
    }
}