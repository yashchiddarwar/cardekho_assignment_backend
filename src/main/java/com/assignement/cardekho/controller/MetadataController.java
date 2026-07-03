package com.assignement.cardekho.controller;

import com.assignement.cardekho.service.CarMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class MetadataController {

    private final CarMetadataService carMetadataService;

    @GetMapping("/segments")
    public ResponseEntity<List<String>> getSegments() {
        log.debug("[METADATA] Fetching segments");
        return ResponseEntity.ok(carMetadataService.getSegments());
    }

    @GetMapping("/body-styles")
    public ResponseEntity<List<String>> getBodyStyles() {
        log.debug("[METADATA] Fetching body-styles");
        return ResponseEntity.ok(carMetadataService.getBodyStyles());
    }

    @GetMapping("/transmissions")
    public ResponseEntity<List<String>> getTransmissions() {
        log.debug("[METADATA] Fetching transmissions");
        return ResponseEntity.ok(carMetadataService.getTransmissions());
    }
}
