package org.example.companyboiler.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.companyboiler.model.Retain;
import org.example.companyboiler.repository.RetainRepository;

import java.util.List;

@RestController
@RequestMapping("/api/retains")
public class RetainController {

    private final RetainRepository retainRepository;

    public RetainController(RetainRepository retainRepository) {
        this.retainRepository = retainRepository;
    }

    @GetMapping
    public List<Retain> getAllLogs() {
        return retainRepository.findAll();
    }

}
