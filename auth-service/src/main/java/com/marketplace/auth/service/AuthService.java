package com.marketplace.auth.service;

import com.marketplace.auth.model.Credential;
import com.marketplace.auth.repository.CredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.marketplace.auth.dto.CredentialRequest;
import com.marketplace.auth.dto.CredentialResponse;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(CredentialRepository credentialRepository, PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String mensaje() {
        return "Hola desde Auth Service - capa Service";
    }

    public CredentialResponse crearCredential(CredentialRequest request) {

        Credential credential = new Credential();

        credential.setUserId(request.getUserId());

        credential.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        Credential credentialGuardada = credentialRepository.save(credential);

        CredentialResponse response = new CredentialResponse();

        response.setId(credentialGuardada.getId());
        response.setUserId(credentialGuardada.getUserId());

        return response;
    }
}