package org.example.companyboiler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.companyboiler.model.QcLog;
import org.example.companyboiler.repository.QcLogRepository;

import java.util.List;

@RestController
@RequestMapping("/api/qc")
public class QcController {

    private final QcLogRepository qcLogRepository;

    public QcController(QcLogRepository qcLogRepository) {
        this.qcLogRepository = qcLogRepository;
    }

    @GetMapping
    public List<QcLog> getAll() {
        return qcLogRepository.findAllOrderByDateDesc();
    }

    @GetMapping("/{batch}")
    public ResponseEntity<QcLog> getByBatch(@PathVariable String batch) {
        return qcLogRepository.findById(batch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public List<QcLog> getByCode(@PathVariable String code) {
        return qcLogRepository.findByCode(code);
    }

    @GetMapping("/releasedby/{releasedBy}")
    public List<QcLog> getByReleasedBy(@PathVariable String releasedBy) {
        return qcLogRepository.findByReleasedBy(releasedBy);
    }

    @GetMapping("/search")
    public List<QcLog> searchByBatch(@RequestParam String batch) {
        return qcLogRepository.findByBatchContainingIgnoreCase(batch);
    }

    @PostMapping
    public QcLog create(@RequestBody QcLog qcLog) {
        return qcLogRepository.save(qcLog);
    }

    @PutMapping("/{batch}")
    public ResponseEntity<QcLog> update(@PathVariable String batch, @RequestBody QcLog qcLog) {
        if (!qcLogRepository.existsById(batch)) {
            return ResponseEntity.notFound().build();
        }
        qcLog.setBatch(batch);
        return ResponseEntity.ok(qcLogRepository.save(qcLog));
    }

    @DeleteMapping("/{batch}")
    public ResponseEntity<Void> delete(@PathVariable String batch) {
        if (!qcLogRepository.existsById(batch)) {
            return ResponseEntity.notFound().build();
        }
        qcLogRepository.deleteById(batch);
        return ResponseEntity.noContent().build();
    }

}
