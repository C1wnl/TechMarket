package com.marketplace.auth.controller;

import com.marketplace.auth.dto.CurrentUserResponse;
import com.marketplace.auth.dto.LoginRequest;
import com.marketplace.auth.dto.LoginResponse;
import com.marketplace.auth.dto.RegisterRequest;
import com.marketplace.auth.dto.RegisterResponse;
import com.marketplace.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public RegisterResponse registrar(
            @Valid @RequestBody RegisterRequest request) {

        return authService.registrar(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse usuarioActual(
            Authentication authentication) {

        CurrentUserResponse response = new CurrentUserResponse();

        response.setUserId(Long.valueOf(authentication.getName()));

        response.setRol(
                authentication.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
                        .replace("ROLE_", "")
        );

        return response;
    }
}