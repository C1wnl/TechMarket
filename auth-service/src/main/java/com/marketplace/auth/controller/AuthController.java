package com.marketplace.auth.controller;

import com.marketplace.auth.model.Credential;
import com.marketplace.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/hello")
    public String hello() {
        return authService.mensaje();
    }

    @GetMapping("/credential/test")
    public Credential crearCredential() {
        return authService.crearCredential();
    }
}