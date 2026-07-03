package com.assignement.cardekho.controller;

import com.assignement.cardekho.domain.dto.CarRecommendationResponseDTO;
import com.assignement.cardekho.domain.dto.RecommendationRequestDTO;
import com.assignement.cardekho.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<List<CarRecommendationResponseDTO>> getRecommendations(
            @Valid @RequestBody RecommendationRequestDTO request) {
        log.info("[RECOMMENDATION] Incoming request — requested: {}, budget: {}L, profile: {}, mileage: {} kmpl, " +
                        "useCases: {}, shapes: {}, safety: {}, transmission: {}, terrain: {}, " +
                        "adas: {}, sunroof: {}, wireless: {}",
                request.getRequestedResultCount(), request.getBudgetCap(), request.getDrivingProfile(), request.getTargetMileage(),
                request.getPrimaryDrivingUseCase(), request.getVehicleShape(), request.getSafetyWeighting(),
                request.getTransmissionPreference(), request.getDrivetrainGeography(),
                request.getAdasRequired(), request.getSunroofRequired(), request.getWirelessTetheringRequired());

        long start = System.currentTimeMillis();
        List<CarRecommendationResponseDTO> results = recommendationService.getRecommendations(request);
        long elapsed = System.currentTimeMillis() - start;

        log.info("[RECOMMENDATION] Completed in {}ms — returning {} result(s): {}",
                elapsed,
                results.size(),
                results.stream().map(r -> r.getCar().getMake() + " " + r.getCar().getModel()
                        + " (" + r.getMatchPercentage() + "%)").toList());

        return ResponseEntity.ok(results);
    }
}
