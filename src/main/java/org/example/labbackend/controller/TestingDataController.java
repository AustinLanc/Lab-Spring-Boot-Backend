package org.example.labbackend.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.labbackend.model.TestingData;
import org.example.labbackend.repository.TestingDataRepository;

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

    @GetMapping("/{id}")
    public ResponseEntity<TestingData> getById(@PathVariable Long id) {
        return testingDataRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batch/{batch}")
    public List<TestingData> getByBatch(@PathVariable String batch) {
        return testingDataRepository.findByBatch(batch);
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

    @PutMapping("/{id}")
    public ResponseEntity<TestingData> update(@PathVariable Long id, @RequestBody TestingData testingData) {
        if (!testingDataRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        testingData.setId(id);
        return ResponseEntity.ok(testingDataRepository.save(testingData));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!testingDataRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        testingDataRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
