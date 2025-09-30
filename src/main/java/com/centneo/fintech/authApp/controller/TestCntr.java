package com.centneo.fintech.authApp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestCntr {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/ping")
    public String getName() {
        return "0K";
    }
}
