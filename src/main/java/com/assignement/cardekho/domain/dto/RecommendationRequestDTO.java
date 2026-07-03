package com.assignement.cardekho.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequestDTO {

    // Q0: Number of recommendations requested
    @NotNull(message = "Requested result count is required")
    @Min(value = 3, message = "Minimum 3 recommendations required")
    @Max(value = 10, message = "Maximum 10 recommendations allowed")
    private Integer requestedResultCount;

    // Q1: Budget Cap
    @NotNull(message = "Budget cap is required")
    @Min(value = 0, message = "Budget cap cannot be negative")
    private Double budgetCap;

    // Q2: Driving Profile (Running Costs)
    @NotNull(message = "Driving profile is required")
    private String drivingProfile; // "Low", "Medium", "High"

    // Q3: Target Mileage
    @NotNull(message = "Target mileage is required")
    @Min(value = 5, message = "Target mileage must be at least 5.0")
    @Max(value = 40, message = "Target mileage must be at most 40.0")
    private Double targetMileage;

    // Q4: Delivery Time Tolerance
    @NotNull(message = "Delivery time tolerance is required")
    private String deliveryTimeTolerance; // "Immediate", "Moderately Flexible", "No Rush"

    // Q5: Primary Driving Use Case
    @NotNull(message = "Primary driving use case segment is required")
    private List<String> primaryDrivingUseCase; // category match, e.g. ["Commuter", "Aspirational"]

    // Q6: Vehicle Shape
    @NotNull(message = "Vehicle shape / body style is required")
    private List<String> vehicleShape; // body style match, e.g. ["Hatchback", "Sedan"]

    // Q7: Safety Weighting
    @NotNull(message = "Safety weighting is required")
    private String safetyWeighting; // "Standard", "High Priority"

    // Q8: Transmission Preference
    @NotNull(message = "Transmission preference is required")
    private List<String> transmissionPreference; // ["Manual", "Automatic"]

    // Q9: Drivetrain & Geography
    @NotNull(message = "Drivetrain & geography option is required")
    private List<String> drivetrainGeography; // ["Plain City Roads & Smooth Highways", ...]

    // Q10: ADAS Requirement
    @NotNull(message = "ADAS requirement selection is required")
    private Boolean adasRequired; // true/false or "Yes"/"No"

    // Q11: Sunroof Requirement
    @NotNull(message = "Sunroof requirement selection is required")
    private Boolean sunroofRequired; // true/false

    // Q12: Smart Tethering Connections
    @NotNull(message = "Wireless CarPlay/Android Auto requirement selection is required")
    private Boolean wirelessTetheringRequired; // true/false
}
