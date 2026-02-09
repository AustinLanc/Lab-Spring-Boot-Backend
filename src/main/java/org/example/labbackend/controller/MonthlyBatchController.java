package org.example.labbackend.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.labbackend.dto.MonthlyStatsDTO;
import org.example.labbackend.model.MonthlyBatch;
import org.example.labbackend.repository.MonthlyBatchRepository;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class MonthlyBatchController {

    private final MonthlyBatchRepository monthlyBatchRepository;

    public MonthlyBatchController(MonthlyBatchRepository monthlyBatchRepository) {
        this.monthlyBatchRepository = monthlyBatchRepository;
    }

    @GetMapping
    public List<MonthlyBatch> getAll() {
        return monthlyBatchRepository.findAllOrderByDateDesc();
    }

    @GetMapping("/{batch}")
    public ResponseEntity<MonthlyBatch> getByBatch(@PathVariable String batch) {
        return monthlyBatchRepository.findById(batch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public List<MonthlyBatch> getByCode(@PathVariable Integer code) {
        return monthlyBatchRepository.findByCode(code);
    }

    @GetMapping("/type/{type}")
    public List<MonthlyBatch> getByType(@PathVariable String type) {
        return monthlyBatchRepository.findByType(type);
    }

    @GetMapping("/released/{released}")
    public List<MonthlyBatch> getByReleased(@PathVariable String released) {
        return monthlyBatchRepository.findByReleased(released);
    }

    @GetMapping("/daterange")
    public List<MonthlyBatch> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return monthlyBatchRepository.findByDateRange(start, end);
    }

    @PostMapping
    public MonthlyBatch create(@RequestBody MonthlyBatch monthlyBatch) {
        return monthlyBatchRepository.save(monthlyBatch);
    }

    @PutMapping("/{batch}")
    public ResponseEntity<MonthlyBatch> update(@PathVariable String batch, @RequestBody MonthlyBatch monthlyBatch) {
        if (!monthlyBatchRepository.existsById(batch)) {
            return ResponseEntity.notFound().build();
        }
        monthlyBatch.setBatch(batch);
        return ResponseEntity.ok(monthlyBatchRepository.save(monthlyBatch));
    }

    @DeleteMapping("/{batch}")
    public ResponseEntity<Void> delete(@PathVariable String batch) {
        if (!monthlyBatchRepository.existsById(batch)) {
            return ResponseEntity.notFound().build();
        }
        monthlyBatchRepository.deleteById(batch);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/years")
    public List<Integer> getAvailableYears() {
        return monthlyBatchRepository.getAvailableYears();
    }

    @GetMapping("/stats/{year}")
    public List<MonthlyStatsDTO> getMonthlyStats(@PathVariable int year) {
        List<Object[]> results = monthlyBatchRepository.getMonthlyStatsByYear(year);
        return results.stream()
                .map(row -> new MonthlyStatsDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        ((Number) row[5]).longValue(),
                        ((Number) row[6]).longValue()
                ))
                .toList();
    }

    @GetMapping("/stats")
    public List<MonthlyStatsDTO> getCurrentYearStats() {
        return getMonthlyStats(Year.now().getValue());
    }

}
