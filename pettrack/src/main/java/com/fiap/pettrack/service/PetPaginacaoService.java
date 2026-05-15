package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.PetDTO;
import com.fiap.pettrack.mapper.IPetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetPaginacaoService {

    @Autowired
    private PetCachingService petCachingService;

    @Autowired
    private IPetMapper iPetMapper;

    @Transactional(readOnly = true)
    public Page<PetDTO> paginar(PageRequest pageRequest) {
        return petCachingService.findAll(pageRequest)
                .map(iPetMapper::toDTO);
    }
}