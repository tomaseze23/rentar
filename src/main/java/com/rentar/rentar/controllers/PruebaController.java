package com.rentar.rentar.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PruebaController {

    @GetMapping("/api/ping")
    public String ping() {
        return "REST ok";
    }
}
