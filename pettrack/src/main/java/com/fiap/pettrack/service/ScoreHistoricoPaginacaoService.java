package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.ScoreHistoricoDTO;
import com.fiap.pettrack.mapper.IScoreHistoricoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreHistoricoPaginacaoService {

    @Autowired
    private ScoreHistoricoCachingService scoreHistoricoCachingService;

    @Autowired
    private IScoreHistoricoMapper iScoreHistoricoMapper;

    @Transactional(readOnly = true)
    public Page<ScoreHistoricoDTO> paginar(PageRequest pageRequest) {
        return scoreHistoricoCachingService.findAll(pageRequest)
                .map(iScoreHistoricoMapper::toDTO);
    }
}