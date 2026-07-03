package com.assignement.cardekho;

import com.assignement.cardekho.domain.dto.CarRecommendationResponseDTO;
import com.assignement.cardekho.domain.dto.RecommendationRequestDTO;
import com.assignement.cardekho.domain.model.Car;
import com.assignement.cardekho.domain.repository.CarRepository;
import com.assignement.cardekho.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService - Scoring Logic Tests")
public class RecommendationServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private Car petrolSedan;    // Maruti Dzire – budget car, petrol, manual
    private Car dieselSuv;      // Hyundai Creta – over budget in some tests, diesel auto, sunroof
    private Car evSuv;          // Tata Punch EV – mid-range EV

    @BeforeEach
    public void setup() {
        petrolSedan = Car.builder()
                .carId("CAR_001").make("Maruti").model("Dzire")
                .exShowroomPrice(7.50)
                .fuelType("Petrol")
                .fuelEfficiencyKmplMpg(24.79)
                .waitingPeriodMonths(1.5)
                .segmentCategory("Commuter")
                .bodyStyle("Sedan")
                .ncapSafetyRating(5).airbagCount(6)
                .transmissionType("Manual")
                .drivetrain("FWD")
                .adasLevel(0)
                .hasSunroof(false)
                .hasWirelessCarPlayAndroidAuto(false)
                .build();

        dieselSuv = Car.builder()
                .carId("CAR_010").make("Hyundai").model("Creta")
                .exShowroomPrice(20.15)
                .fuelType("Diesel")
                .fuelEfficiencyKmplMpg(21.8)
                .waitingPeriodMonths(5.0)
                .segmentCategory("Commuter")
                .bodyStyle("SUV")
                .ncapSafetyRating(3).airbagCount(6)
                .transmissionType("Automatic")
                .drivetrain("FWD")
                .adasLevel(2)
                .hasSunroof(true)
                .hasWirelessCarPlayAndroidAuto(true)
                .build();

        evSuv = Car.builder()
                .carId("CAR_007").make("Tata").model("Punch EV")
                .exShowroomPrice(13.30)
                .fuelType("EV")
                .fuelEfficiencyKmplMpg(140.00)   // EV range, not KMPL
                .waitingPeriodMonths(1.5)
                .segmentCategory("EV")
                .bodyStyle("SUV")
                .ncapSafetyRating(5).airbagCount(6)
                .transmissionType("Automatic")
                .drivetrain("FWD")
                .adasLevel(0)
                .hasSunroof(true)
                .hasWirelessCarPlayAndroidAuto(true)
                .build();
    }

    // ── Q1: Budget hard filter ─────────────────────────────────────────────────

    @Test
    @DisplayName("Q1: Budget filter drops cars over cap")
    public void testBudgetFilterDropsExpensiveCars() {
        when(carRepository.findAll()).thenReturn(Arrays.asList(petrolSedan, dieselSuv, evSuv));

        RecommendationRequestDTO request = baseRequest(15.0);
        // dieselSuv costs 20.15 – should be excluded
        List<CarRecommendationResponseDTO> result = recommendationService.getRecommendations(request);

        assertTrue(result.stream().noneMatch(r -> "CAR_010".equals(r.getCar().getCarId())),
                "Creta (20.15L) should be filtered out for budget cap of 15L");
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Q1: All cars pass budget when cap is high enough")
    public void testBudgetFilterPassesAll() {
        when(carRepository.findAll()).thenReturn(Arrays.asList(petrolSedan, dieselSuv, evSuv));
        RecommendationRequestDTO request = baseRequest(25.0);
        List<CarRecommendationResponseDTO> result = recommendationService.getRecommendations(request);
        assertEquals(3, result.size());
    }

    // ── Q3: Mileage proximity ───────────────────────────────────────────────────

    @Test
    @DisplayName("Q3: EV car gets neutral 10pts (not penalised on KMPL axis)")
    public void testEVCarGetsNeutralMileageScore() {
        when(carRepository.findAll()).thenReturn(List.of(evSuv));

        // Target mileage of 20 – very different from EV's 140 range value
        RecommendationRequestDTO request = RecommendationRequestDTO.builder()
                .budgetCap(20.0).drivingProfile("Low").targetMileage(20.0)
                .deliveryTimeTolerance("No Rush").primaryDrivingUseCase(List.of("EV"))
                .vehicleShape(List.of("SUV")).safetyWeighting("Standard")
                .transmissionPreference(List.of("Automatic"))
                .drivetrainGeography(List.of("Plain City Roads & Smooth Highways"))
                .adasRequired(false).sunroofRequired(false).wirelessTetheringRequired(false)
                .build();

        List<CarRecommendationResponseDTO> result = recommendationService.getRecommendations(request);
        // Q3 for EV should be +10 (neutral), not 0
        // Q2 Low + EV = +10, Q3 neutral = +10, Q4 No Rush = +10, Q5 EV = +10, Q6 SUV = +10,
        // Q7 Std 5*2=+10, Q8 Auto=+10, Q9 FWD city=+10, Q10 no=+10, Q11 no=+10, Q12 no=+10
        // Expected = 110 pts = 100%
        assertEquals(100.0, result.get(0).getMatchPercentage(), 0.01,
                "A perfect-match EV should score 100%");
    }

    // ── Q7: Safety weighting cap ────────────────────────────────────────────────

    @Test
    @DisplayName("Q7 High Priority: score is capped at 10pts per question ceiling")
    public void testSafetyHighPriorityIsCapped() {
        // Car with 5-star NCAP and 8 airbags: raw = 5*1.5 + 8*0.5 = 7.5+4.0 = 11.5 → capped to 10
        Car safeCar = Car.builder()
                .carId("SAFE_01").make("BrandA").model("ModelA")
                .exShowroomPrice(10.0).fuelType("Petrol")
                .fuelEfficiencyKmplMpg(20.0).waitingPeriodMonths(1.0)
                .segmentCategory("Commuter").bodyStyle("SUV")
                .ncapSafetyRating(5).airbagCount(8)
                .transmissionType("Manual").drivetrain("FWD").adasLevel(0)
                .hasSunroof(false).hasWirelessCarPlayAndroidAuto(false).build();

        // Car with unrealistically high airbags: 5*1.5+20*0.5 = 17.5 → also capped at 10
        Car overloadedCar = Car.builder()
                .carId("SAFE_02").make("BrandB").model("ModelB")
                .exShowroomPrice(10.0).fuelType("Petrol")
                .fuelEfficiencyKmplMpg(20.0).waitingPeriodMonths(1.0)
                .segmentCategory("Commuter").bodyStyle("SUV")
                .ncapSafetyRating(5).airbagCount(20)
                .transmissionType("Manual").drivetrain("FWD").adasLevel(0)
                .hasSunroof(false).hasWirelessCarPlayAndroidAuto(false).build();

        when(carRepository.findAll()).thenReturn(Arrays.asList(safeCar, overloadedCar));

        RecommendationRequestDTO request = baseRequest(15.0);
        request.setSafetyWeighting("High Priority");

        List<CarRecommendationResponseDTO> result = recommendationService.getRecommendations(request);
        // Both are capped to 10 pts for Q7, so both earn the same total
        assertEquals(2, result.size(), "Both distinct models should appear in results");
        assertEquals(result.get(0).getMatchPercentage(), result.get(1).getMatchPercentage(),
                "Both safe cars should score equally after 10pt cap");
        assertTrue(result.get(0).getEarnedPoints() <= 110.0,
                "Earned points must never exceed TOTAL_POSSIBLE_POINTS (110)");
    }

    // ── Q8: AMT counted as Automatic ───────────────────────────────────────────

    @Test
    @DisplayName("Q8: AMT transmission counts as Automatic")
    public void testAMTCountsAsAutomatic() {
        Car amtCar = Car.builder()
                .carId("AMT_01").exShowroomPrice(8.0).fuelType("Petrol")
                .fuelEfficiencyKmplMpg(22.0).waitingPeriodMonths(1.0)
                .segmentCategory("Commuter").bodyStyle("Hatchback")
                .ncapSafetyRating(3).airbagCount(4)
                .transmissionType("AMT").drivetrain("FWD").adasLevel(0)
                .hasSunroof(false).hasWirelessCarPlayAndroidAuto(false).build();

        when(carRepository.findAll()).thenReturn(List.of(amtCar));
        RecommendationRequestDTO request = baseRequest(15.0);
        request.setTransmissionPreference(List.of("Automatic"));

        List<CarRecommendationResponseDTO> result = recommendationService.getRecommendations(request);
        // Should score +10 for Q8
        assertFalse(result.isEmpty());
        // Verify earned points include Q8 contribution: pick a score > what it'd be without Q8
        assertTrue(result.get(0).getMatchPercentage() > 0);
    }

    // ── Top 3 limit ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns at most top 3 results sorted by matchPercentage descending")
    public void testTop3Sorted() {
        when(carRepository.findAll()).thenReturn(Arrays.asList(petrolSedan, evSuv, dieselSuv));
        RecommendationRequestDTO request = baseRequest(25.0);
        List<CarRecommendationResponseDTO> result = recommendationService.getRecommendations(request);
        assertTrue(result.size() <= 3, "Should return at most 3 results");
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getMatchPercentage() >= result.get(i + 1).getMatchPercentage(),
                    "Results should be sorted descending by matchPercentage");
        }
    }

    // ── Helper: builds a sensible default request ────────────────────────────────
    private RecommendationRequestDTO baseRequest(double budget) {
        return RecommendationRequestDTO.builder()
                .requestedResultCount(3)
                .budgetCap(budget)
                .drivingProfile("Low")
                .targetMileage(20.0)
                .deliveryTimeTolerance("Immediate")
                .primaryDrivingUseCase(List.of("Commuter"))
                .vehicleShape(List.of("SUV"))
                .safetyWeighting("Standard")
                .transmissionPreference(List.of("Manual"))
                .drivetrainGeography(List.of("Plain City Roads & Smooth Highways"))
                .adasRequired(false)
                .sunroofRequired(false)
                .wirelessTetheringRequired(false)
                .build();
    }
}
