package com.assignement.cardekho.service.impl;

import com.assignement.cardekho.domain.dto.CarDTO;
import com.assignement.cardekho.domain.dto.CarRecommendationResponseDTO;
import com.assignement.cardekho.domain.dto.RecommendationRequestDTO;
import com.assignement.cardekho.domain.model.Car;
import com.assignement.cardekho.domain.repository.CarRepository;
import com.assignement.cardekho.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the 12-question algorithmic matching engine as defined in
 * scoring_strat.txt.
 *
 * Scoring breakdown (max 110 points across Q2–Q12):
 * Q2 - Driving Profile : 0 or +10 pts
 * Q3 - Target Mileage (Proximity): 0 to +10 pts (linear decay, EV range
 * excluded)
 * Q4 - Delivery Tolerance : 0 to +10 pts
 * Q5 - Use Case Segment : 0 or +10 pts
 * Q6 - Body Style : 0 or +10 pts
 * Q7 - Safety Weighting : Standard = NCAP*2 (max 10), High Priority = NCAP*1.5+Airbags*0.5 (capped at 10)
 * Q8 - Transmission : 0 or +10 pts
 * Q9 - Drivetrain/Geography : 0 or +10 pts
 * Q10 - ADAS : 0 or +10 pts
 * Q11 - Sunroof : 0 or +10 pts
 * Q12 - Wireless CarPlay/AA : 0 or +10 pts
 *
 * Total possible = 110 pts (max 10 per question, Q2–Q12).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private static final double TOTAL_POSSIBLE_POINTS = 110.0;
    // EV cars report range in km as "efficiency" — exclude them from mileage
    // proximity scoring
    private static final String EV_FUEL_TYPE = "EV";

    private final CarRepository carRepository;

    @Override
    public List<CarRecommendationResponseDTO> getRecommendations(RecommendationRequestDTO request) {
        List<Car> allCars = carRepository.findAll();

        // ── Q1: Hard Budget Filter ──────────────────────────────────────────────
        // Drop any car whose ex-showroom price exceeds the user's budget cap.
        List<Car> budgetFilteredCars = allCars.stream()
                .filter(car -> car.getExShowroomPrice() != null
                        && car.getExShowroomPrice() <= request.getBudgetCap())
                .collect(Collectors.toList());

        log.info("[SCORING] Budget filter: {}/{} cars pass the ₹{}L cap",
                budgetFilteredCars.size(), allCars.size(), request.getBudgetCap());

        List<CarRecommendationResponseDTO> scoredCars = new ArrayList<>();

        for (Car car : budgetFilteredCars) {
            double earnedPoints = 0.0;

            // ── Q2: Driving Profile (Running Costs) ───────────────────────────
            // Matches fuel type to expected monthly mileage band.
            earnedPoints += scoreQ2DrivingProfile(request.getDrivingProfile(), car.getFuelType());

            // ── Q3: Target Mileage (Proximity Decay) ─────────────────────────
            // Score = Max(0, 10 - |target - actual|). EV cars skipped (range ≠ KMPL).
            earnedPoints += scoreQ3TargetMileage(request.getTargetMileage(), car);

            // ── Q4: Delivery Time Tolerance ────────────────────────────────────
            earnedPoints += scoreQ4DeliveryTolerance(request.getDeliveryTimeTolerance(), car.getWaitingPeriodMonths());

            // ── Q5: Primary Driving Use Case (Segment Category Match) ─────────
            if (request.getPrimaryDrivingUseCase() != null
                    && isAnyInListIgnoreCase(car.getSegmentCategory(), request.getPrimaryDrivingUseCase())) {
                earnedPoints += 10.0;
            }

            // ── Q6: Vehicle Shape (Body Style Match) ──────────────────────────
            if (request.getVehicleShape() != null
                    && isAnyInListIgnoreCase(car.getBodyStyle(), request.getVehicleShape())) {
                earnedPoints += 10.0;
            }

            // ── Q7: Safety Weighting ──────────────────────────────────────────
            earnedPoints += scoreQ7Safety(request.getSafetyWeighting(), car);

            // ── Q8: Transmission Preference ───────────────────────────────────
            // "Automatic" group: Automatic, CVT, DCT, AMT (per spec's grouping note)
            earnedPoints += scoreQ8Transmission(request.getTransmissionPreference(), car.getTransmissionType());

            // ── Q9: Drivetrain & Geography ────────────────────────────────────
            earnedPoints += scoreQ9Drivetrain(request.getDrivetrainGeography(), car.getDrivetrain());

            // ── Q10: ADAS Requirement ─────────────────────────────────────────
            // No/Indifferent = neutral +10 for all cars.
            if (Boolean.TRUE.equals(request.getAdasRequired())) {
                if (car.getAdasLevel() != null && car.getAdasLevel() >= 1) {
                    earnedPoints += 10.0;
                }
                // ADAS_Level == 0 → +0 pts
            } else {
                earnedPoints += 10.0; // Neutral
            }

            // ── Q11: Sunroof Requirement ──────────────────────────────────────
            // "No" = neutral +10 for all cars.
            if (Boolean.TRUE.equals(request.getSunroofRequired())) {
                if (Boolean.TRUE.equals(car.getHasSunroof())) {
                    earnedPoints += 10.0;
                }
                // Has_Sunroof == false → +0 pts
            } else {
                earnedPoints += 10.0; // Neutral
            }

            // ── Q12: Smart Tethering (Wireless CarPlay / Android Auto) ────────
            // "No" = neutral +10 for all cars.
            if (Boolean.TRUE.equals(request.getWirelessTetheringRequired())) {
                if (Boolean.TRUE.equals(car.getHasWirelessCarPlayAndroidAuto())) {
                    earnedPoints += 10.0;
                }
                // Has_Wireless == false → +0 pts
            } else {
                earnedPoints += 10.0; // Neutral
            }

            // ── Final Score Compilation ───────────────────────────────────────
            // Each question contributes max 10 pts; total is normalised over 110.
            double matchPercentage = (earnedPoints / TOTAL_POSSIBLE_POINTS) * 100.0;

            log.debug("[SCORING] {} {} — earned: {} pts, match: {}%",
                    car.getMake(), car.getModel(), round2(earnedPoints), round2(matchPercentage));

            scoredCars.add(CarRecommendationResponseDTO.builder()
                    .car(toCarDTO(car))
                    .earnedPoints(round2(earnedPoints))
                    .totalPossiblePoints(TOTAL_POSSIBLE_POINTS)
                    .matchPercentage(round2(matchPercentage))
                    .build());
        }

        // ── Deduplication by Make + Model ────────────────────────────────────
        // If multiple variants of the same model are scored (e.g. Thar, Thar Roxx,
        // Thar Roxx 2025), we keep only the highest-scoring variant per make+model
        // group. This ensures the top 3 results represent 3 distinct car models,
        // giving the user genuinely diverse recommendations.
        Map<String, CarRecommendationResponseDTO> bestPerModel = new LinkedHashMap<>();
        scoredCars.stream()
                .sorted((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()))
                .forEach(rec -> {
                    String key = (rec.getCar().getMake() + "|"
                            + rec.getCar().getModel()).toLowerCase();
                    bestPerModel.putIfAbsent(key, rec); // first encountered = highest score
                });

        log.info("[SCORING] Deduplication: {} unique make+model group(s) from {} scored car(s)",
                bestPerModel.size(), scoredCars.size());

        int limit = request.getRequestedResultCount() != null ? request.getRequestedResultCount() : 3;

        // Sort deduplicated results and return Top N
        List<CarRecommendationResponseDTO> topN = bestPerModel.values().stream()
                .sorted((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()))
                .limit(limit)
                .collect(Collectors.toList());

        log.info("[SCORING] Final top-{}: {}", topN.size(),
                topN.stream().map(r -> r.getCar().getMake() + " " + r.getCar().getModel()
                        + " " + r.getCar().getVariantTrim()
                        + " -> " + r.getMatchPercentage() + "%").toList());

        return topN;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Scoring Methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Q2: Fuel type affinity based on monthly distance band.
     * Low → Petrol / EV (+10)
     * Medium → Petrol / Hybrid / EV (+10)
     * High → Diesel / CNG / Hybrid (+10)
     */
    private double scoreQ2DrivingProfile(String profile, String fuelType) {
        if (profile == null || fuelType == null)
            return 0.0;
        return switch (profile.trim().toLowerCase()) {
            case "low" -> (isAnyOf(fuelType, "Petrol", "EV")) ? 10.0 : 0.0;
            case "medium" -> (isAnyOf(fuelType, "Petrol", "Hybrid", "EV")) ? 10.0 : 0.0;
            case "high" -> (isAnyOf(fuelType, "Diesel", "CNG", "Hybrid")) ? 10.0 : 0.0;
            default -> 0.0;
        };
    }

    /**
     * Q3: Linear proximity decay.
     * Score = Max(0, 10 - |targetMileage - car.fuelEfficiency|)
     * EV cars are skipped: their "efficiency" value is electric range (km/charge),
     * not KMPL.
     * They receive a neutral +10 so they are not unfairly penalised on this axis.
     */
    private double scoreQ3TargetMileage(Double targetMileage, Car car) {
        if (targetMileage == null)
            return 0.0;
        // EV cars don't have a comparable KMPL — award neutral score
        if (EV_FUEL_TYPE.equalsIgnoreCase(car.getFuelType())) {
            return 10.0;
        }
        if (car.getFuelEfficiencyKmplMpg() == null)
            return 0.0;
        double diff = Math.abs(targetMileage - car.getFuelEfficiencyKmplMpg());
        return Math.max(0.0, 10.0 - diff);
    }

    /**
     * Q4: Delivery time tolerance scoring.
     * Immediate → ≤2 months: +10; >2 months: Max(0, 10 - waitingMonths*2)
     * Mod Flexible → ≤6 months: +10; >6 months: +0
     * No Rush → All: +10 (neutral)
     */
    private double scoreQ4DeliveryTolerance(String tolerance, Double waitingMonths) {
        if (tolerance == null)
            return 0.0;
        return switch (tolerance.trim().toLowerCase()) {
            case "immediate" -> {
                if (waitingMonths == null)
                    yield 0.0;
                if (waitingMonths <= 2.0)
                    yield 10.0;
                yield Math.max(0.0, 10.0 - (waitingMonths * 2.0));
            }
            case "moderately flexible" -> {
                if (waitingMonths != null && waitingMonths <= 6.0)
                    yield 10.0;
                yield 0.0;
            }
            case "no rush" -> 10.0;
            default -> 0.0;
        };
    }

    /**
     * Q7: Safety weighting.
     * Standard     → NCAP_Rating * 2 (max 10 pts for a 5-star car)
     * High Priority → (NCAP_Rating * 1.5) + (Airbag_Count * 0.5), capped at 10 pts.
     *
     * The High Priority formula gives a richer signal (airbags matter too), but the
     * ceiling is 10 — identical to every other question — so the 110-point pool is
     * never breached. A 5★ / 6-airbag car scores 10.5 raw, capped to 10.
     * A 3★ / 6-airbag car scores 7.5 raw — no cap needed. Differentiation is preserved.
     */
    private double scoreQ7Safety(String safetyWeighting, Car car) {
        if (safetyWeighting == null) return 0.0;
        if ("Standard".equalsIgnoreCase(safetyWeighting)) {
            return car.getNcapSafetyRating() != null ? car.getNcapSafetyRating() * 2.0 : 0.0;
        }
        if ("High Priority".equalsIgnoreCase(safetyWeighting)) {
            double ratingPart = car.getNcapSafetyRating() != null ? car.getNcapSafetyRating() * 1.5 : 0.0;
            double airbagPart = car.getAirbagCount() != null ? car.getAirbagCount() * 0.5 : 0.0;
            return Math.min(10.0, ratingPart + airbagPart); // Hard cap at 10 to match every other question's ceiling
        }
        return 0.0;
    }

    /**
     * Q8: Transmission preference.
     * Manual → Transmission_Type == "Manual": +10
     * Automatic → Transmission_Type in {Automatic, CVT, DCT, AMT}: +10
     * (AMT included per spec's "grouping" note for Automatic category)
     */
    private double scoreQ8Transmission(List<String> preferences, String transmissionType) {
        if (preferences == null || preferences.isEmpty() || transmissionType == null)
            return 0.0;
        for (String pref : preferences) {
            if ("Manual".equalsIgnoreCase(pref) && "Manual".equalsIgnoreCase(transmissionType)) {
                return 10.0;
            }
            if ("Automatic".equalsIgnoreCase(pref) && isAnyOf(transmissionType, "Automatic", "CVT", "DCT", "AMT")) {
                return 10.0;
            }
        }
        return 0.0;
    }

    /**
     * Q9: Drivetrain & geography.
     * City/Highways → FWD or AWD: +10
     * Off-road/Slopes → RWD, 4WD, or AWD: +10
     */
    private double scoreQ9Drivetrain(List<String> geographies, String drivetrain) {
        if (geographies == null || geographies.isEmpty() || drivetrain == null)
            return 0.0;
        for (String geo : geographies) {
            if (geo.equalsIgnoreCase("Plain City Roads & Smooth Highways") && isAnyOf(drivetrain, "FWD", "AWD")) {
                return 10.0;
            }
            if (geo.equalsIgnoreCase("Mountainous Terrain, Frequent Slopes, or Loose Sand/Mud Off-Roads")
                    && isAnyOf(drivetrain, "RWD", "4WD", "AWD")) {
                return 10.0;
            }
        }
        return 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Case-insensitive multi-value match. */
    private boolean isAnyOf(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value))
                return true;
        }
        return false;
    }

    /** Case-insensitive check if a string is in a list. */
    private boolean isAnyInListIgnoreCase(String target, List<String> candidates) {
        if (target == null || candidates == null)
            return false;
        for (String candidate : candidates) {
            if (target.equalsIgnoreCase(candidate))
                return true;
        }
        return false;
    }

    /** Round to 2 decimal places. */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Map Car entity to CarDTO to avoid exposing JPA internals. */
    private CarDTO toCarDTO(Car car) {
        return CarDTO.builder()
                .carId(car.getCarId())
                .make(car.getMake())
                .model(car.getModel())
                .variantTrim(car.getVariantTrim())
                .bodyStyle(car.getBodyStyle())
                .segmentCategory(car.getSegmentCategory())
                .exShowroomPrice(car.getExShowroomPrice())
                .fuelType(car.getFuelType())
                .engineDisplacementCc(car.getEngineDisplacementCc())
                .horsepowerBhp(car.getHorsepowerBhp())
                .torqueNm(car.getTorqueNm())
                .transmissionType(car.getTransmissionType())
                .drivetrain(car.getDrivetrain())
                .fuelEfficiencyKmplMpg(car.getFuelEfficiencyKmplMpg())
                .zeroTo100KmphSec(car.getZeroTo100KmphSec())
                .ncapSafetyRating(car.getNcapSafetyRating())
                .airbagCount(car.getAirbagCount())
                .adasLevel(car.getAdasLevel())
                .infotainmentScreenSizeInches(car.getInfotainmentScreenSizeInches())
                .hasWirelessCarPlayAndroidAuto(car.getHasWirelessCarPlayAndroidAuto())
                .hasSunroof(car.getHasSunroof())
                .averageUserReviewScore(car.getAverageUserReviewScore())
                .waitingPeriodMonths(car.getWaitingPeriodMonths())
                .build();
    }
}
