package com.marketplace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Email(message = "El correo electronico debe tener un formato valido")
    @NotBlank(message = "El correo electronico es obligatorio")
    private String email;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}