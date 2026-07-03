package com.assignement.cardekho.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A clean DTO representation of a Car entity, decoupled from JPA concerns.
 * Returned as part of recommendation and other responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarDTO {
    private String carId;
    private String make;
    private String model;
    private String variantTrim;
    private String bodyStyle;
    private String segmentCategory;
    private Double exShowroomPrice;
    private String fuelType;
    private Integer engineDisplacementCc;
    private Integer horsepowerBhp;
    private Integer torqueNm;
    private String transmissionType;
    private String drivetrain;
    private Double fuelEfficiencyKmplMpg;
    private Double zeroTo100KmphSec;
    private Integer ncapSafetyRating;
    private Integer airbagCount;
    private Integer adasLevel;
    private Double infotainmentScreenSizeInches;
    private Boolean hasWirelessCarPlayAndroidAuto;
    private Boolean hasSunroof;
    private Double averageUserReviewScore;
    private Double waitingPeriodMonths;
}
