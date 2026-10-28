package com.marketplace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @Email(message = "El correo electrónico debe tener un formato válido")
    @NotBlank(message = "El correo electrónico es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}