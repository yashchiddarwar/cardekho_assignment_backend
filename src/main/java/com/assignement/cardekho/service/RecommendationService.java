package com.assignement.cardekho.service;

import com.assignement.cardekho.domain.dto.CarRecommendationResponseDTO;
import com.assignement.cardekho.domain.dto.RecommendationRequestDTO;

import java.util.List;

public interface RecommendationService {
    List<CarRecommendationResponseDTO> getRecommendations(RecommendationRequestDTO request);
}
