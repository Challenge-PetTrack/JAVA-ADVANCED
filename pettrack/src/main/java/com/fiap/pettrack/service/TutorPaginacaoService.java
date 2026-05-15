package com.fiap.pettrack.service;

import com.fiap.pettrack.dto.TutorDTO;
import com.fiap.pettrack.mapper.ITutorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class TutorPaginacaoService {

    @Autowired
    private TutorCachingService tutorCachingService;
    @Autowired
    private ITutorMapper iTutorMapper;

    @Transactional(readOnly = true)
    public Page<TutorDTO> paginar(PageRequest pageRequest){
        return tutorCachingService.findAll(pageRequest).map(iTutorMapper::toDTO);
    }
}
