package org.example.companyboiler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RouteController {

    @GetMapping(value = { "/", "/qc", "/results", "/update", "/retains", "/reminders" })
    public String index() {
        return "forward:/index.html";
    }
}
