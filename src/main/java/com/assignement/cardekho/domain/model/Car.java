package com.assignement.cardekho.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cars")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Car {

    @Id
    @Column(name = "car_id")
    private String carId;

    @Column(name = "make")
    private String make;

    @Column(name = "model")
    private String model;

    @Column(name = "variant_trim")
    private String variantTrim;

    @Column(name = "body_style")
    private String bodyStyle;

    @Column(name = "segment_category")
    private String segmentCategory;

    @Column(name = "ex_showroom_price")
    private Double exShowroomPrice;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "engine_displacement_cc")
    private Integer engineDisplacementCc;

    @Column(name = "horsepower_bhp")
    private Integer horsepowerBhp;

    @Column(name = "torque_nm")
    private Integer torqueNm;

    @Column(name = "transmission_type")
    private String transmissionType;

    @Column(name = "drivetrain")
    private String drivetrain;

    @Column(name = "fuel_efficiency_kmpl_mpg")
    private Double fuelEfficiencyKmplMpg;

    @Column(name = "zero_to_100_kmph_sec")
    private Double zeroTo100KmphSec;

    @Column(name = "ncap_safety_rating")
    private Integer ncapSafetyRating;

    @Column(name = "airbag_count")
    private Integer airbagCount;

    @Column(name = "adas_level")
    private Integer adasLevel;

    @Column(name = "infotainment_screen_size_inches")
    private Double infotainmentScreenSizeInches;

    @Column(name = "has_wireless_carplay_androidauto")
    private Boolean hasWirelessCarPlayAndroidAuto;

    @Column(name = "has_sunroof")
    private Boolean hasSunroof;

    @Column(name = "average_user_review_score")
    private Double averageUserReviewScore;

    @Column(name = "waiting_period_months")
    private Double waitingPeriodMonths;
}
