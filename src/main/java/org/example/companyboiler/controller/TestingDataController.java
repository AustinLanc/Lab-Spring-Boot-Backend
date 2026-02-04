package org.example.companyboiler.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.companyboiler.model.TestingData;
import org.example.companyboiler.repository.TestingDataRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/testing")
public class TestingDataController {

    private final TestingDataRepository testingDataRepository;

    public TestingDataController(TestingDataRepository testingDataRepository) {
        this.testingDataRepository = testingDataRepository;
    }

    @GetMapping
    public List<TestingData> getAll() {
        return testingDataRepository.findAllOrderByDateDesc();
    }

    @GetMapping("/{batch}")
    public ResponseEntity<TestingData> getByBatch(@PathVariable String batch) {
        return testingDataRepository.findById(batch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public List<TestingData> getByCode(@PathVariable String code) {
        return testingDataRepository.findByCode(code);
    }

    @GetMapping("/search")
    public List<TestingData> searchByBatch(@RequestParam String batch) {
        return testingDataRepository.findByBatchContainingIgnoreCase(batch);
    }

    @GetMapping("/date/{date}")
    public List<TestingData> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return testingDataRepository.findByDate(date);
    }

    @GetMapping("/daterange")
    public List<TestingData> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return testingDataRepository.findByDateRange(start, end);
    }

    @PostMapping
    public TestingData create(@RequestBody TestingData testingData) {
        return testingDataRepository.save(testingData);
    }

    @PutMapping("/{batch}")
    public ResponseEntity<TestingData> update(@PathVariable String batch, @RequestBody TestingData testingData) {
        if (!testingDataRepository.existsById(batch)) {
            return ResponseEntity.notFound().build();
        }
        testingData.setBatch(batch);
        return ResponseEntity.ok(testingDataRepository.save(testingData));
    }

    @DeleteMapping("/{batch}")
    public ResponseEntity<Void> delete(@PathVariable String batch) {
        if (!testingDataRepository.existsById(batch)) {
            return ResponseEntity.notFound().build();
        }
        testingDataRepository.deleteById(batch);
        return ResponseEntity.noContent().build();
    }

}
