package com.assignement.cardekho.service.impl;

import com.assignement.cardekho.domain.repository.CarRepository;
import com.assignement.cardekho.service.CarMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarMetadataServiceImpl implements CarMetadataService {

    private final CarRepository carRepository;

    @Override
    public List<String> getSegments() {
        return carRepository.findDistinctSegmentCategory();
    }

    @Override
    public List<String> getBodyStyles() {
        return carRepository.findDistinctBodyStyle();
    }

    @Override
    public List<String> getTransmissions() {
        return carRepository.findDistinctTransmissionType();
    }
}
