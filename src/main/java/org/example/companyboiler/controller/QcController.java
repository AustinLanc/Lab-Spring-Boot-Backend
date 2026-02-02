package org.example.companyboiler.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public List<QcLog> getAllLogs() {
        return qcLogRepository.findAll();
    }

}
