package org.example.companyboiler.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.companyboiler.model.Retain;
import org.example.companyboiler.repository.RetainRepository;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/retains")
public class RetainController {

    private final RetainRepository retainRepository;

    public RetainController(RetainRepository retainRepository) {
        this.retainRepository = retainRepository;
    }

    @GetMapping
    public List<Retain> getAll() {
        return retainRepository.findAllOrderByDateDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Retain> getById(@PathVariable Long id) {
        return retainRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batch/{batch}")
    public List<Retain> getByBatch(@PathVariable String batch) {
        return retainRepository.findByBatch(batch);
    }

    @GetMapping("/code/{code}")
    public List<Retain> getByCode(@PathVariable Long code) {
        return retainRepository.findByCode(code);
    }

    @GetMapping("/box/{box}")
    public List<Retain> getByBox(@PathVariable Long box) {
        return retainRepository.findByBox(box);
    }

    @GetMapping("/search")
    public List<Retain> searchByBatch(@RequestParam String batch) {
        return retainRepository.findByBatchContainingIgnoreCase(batch);
    }

    @GetMapping("/daterange")
    public List<Retain> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date end) {
        return retainRepository.findByDateRange(start, end);
    }

    @PostMapping
    public Retain create(@RequestBody Retain retain) {
        return retainRepository.save(retain);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Retain> update(@PathVariable Long id, @RequestBody Retain retain) {
        if (!retainRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        retain.setId(id);
        return ResponseEntity.ok(retainRepository.save(retain));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!retainRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        retainRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
