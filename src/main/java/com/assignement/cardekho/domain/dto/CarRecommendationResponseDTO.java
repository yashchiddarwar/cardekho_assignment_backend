package com.assignement.cardekho.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarRecommendationResponseDTO {
    private CarDTO car;
    private Double matchPercentage;
    private Double earnedPoints;
    private Double totalPossiblePoints;
}
