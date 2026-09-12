package com.knightdevelopers.kheerabackend.service;

import com.knightdevelopers.kheerabackend.dto.SpaceListDto;
import com.knightdevelopers.kheerabackend.repository.SpaceMembersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpaceService {
    private final SpaceMembersRepository spaceMembersRepository;

    public SpaceService(SpaceMembersRepository spaceMembersRepository) {
        this.spaceMembersRepository = spaceMembersRepository;
    }

    @Transactional(readOnly = true)
    public List<SpaceListDto> getSpacesForUserEmail(String email) {
        return spaceMembersRepository.findActiveSpacesByUserEmail(email);
    }
}
