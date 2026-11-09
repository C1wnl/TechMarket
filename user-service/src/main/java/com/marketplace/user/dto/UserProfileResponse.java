
package com.marketplace.user.dto;

public class UserProfileResponse {

    private String nombre;
    private String email;

    public UserProfileResponse(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }
}
