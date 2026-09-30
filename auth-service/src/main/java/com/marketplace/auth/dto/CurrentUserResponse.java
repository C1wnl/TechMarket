package com.marketplace.auth.dto;

public class CurrentUserResponse {

    private Long userId;
    private String rol;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}