package com.marketplace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Email(message = "El correo electronico debe tener un formato valido")
    @NotBlank(message = "El correo electronico es obligatorio")
    private String email;

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(
            regexp = "USER|ADMIN",
            message = "El rol debe ser USER o ADMIN"
    )
    private String rol;

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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}