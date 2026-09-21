package com.marketplace.auth.service;

import com.marketplace.auth.model.Credential;
import com.marketplace.auth.repository.CredentialRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;

    public AuthService(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    public String mensaje() {
        return "Hola desde Auth Service - capa Service";
    }

    public Credential crearCredential() {

        Credential credential = new Credential();

        credential.setUserId(1L);
        credential.setPasswordHash("hash-de-prueba");

        return credentialRepository.save(credential);
    }
}