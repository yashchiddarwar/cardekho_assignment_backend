package com.assignement.cardekho.domain.repository;

import com.assignement.cardekho.domain.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, String> {

    @Query("SELECT DISTINCT c.bodyStyle FROM Car c WHERE c.bodyStyle IS NOT NULL")
    List<String> findDistinctBodyStyle();

    @Query("SELECT DISTINCT c.segmentCategory FROM Car c WHERE c.segmentCategory IS NOT NULL")
    List<String> findDistinctSegmentCategory();

    @Query("SELECT DISTINCT c.transmissionType FROM Car c WHERE c.transmissionType IS NOT NULL")
    List<String> findDistinctTransmissionType();
}
