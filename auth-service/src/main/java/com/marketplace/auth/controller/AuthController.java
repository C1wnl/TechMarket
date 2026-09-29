package com.marketplace.auth.controller;
import com.marketplace.auth.client.UserClient;
import com.marketplace.auth.dto.CredentialRequest;
import com.marketplace.auth.dto.CredentialResponse;
import com.marketplace.auth.dto.UserResponse;
import com.marketplace.auth.model.Credential;
import com.marketplace.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.marketplace.auth.dto.LoginRequest;

@RestController
public class AuthController {

    private final AuthService authService;
    private final UserClient userClient;

    public AuthController(AuthService authService, UserClient userClient) {
        this.authService = authService;
        this.userClient = userClient;
    }

    @GetMapping("/hello")
    public String hello() {
        return authService.mensaje();
    }

    /*
    @GetMapping("/credential/test")
    public Credential crearCredential() {
        return authService.crearCredential();
    }
    */

    @PostMapping("/credential")
    public CredentialResponse crearCredential(
            @Valid @RequestBody CredentialRequest request) {

        return authService.crearCredential(request);
    }

    @GetMapping("/test-user")
    public UserResponse obtenerUsuarioPorEmail(@RequestParam String email) {
        return userClient.obtenerUsuarioPorEmail(email);
    }

    @PostMapping("/login-test")
    public boolean loginTest(@RequestBody LoginRequest request) {

        return authService.autenticar(request);
    }
}