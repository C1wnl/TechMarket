package com.marketplace.auth.service;

import com.marketplace.auth.model.Credential;
import com.marketplace.auth.repository.CredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.marketplace.auth.dto.CredentialRequest;
import com.marketplace.auth.dto.CredentialResponse;
import com.marketplace.auth.client.UserClient;
import com.marketplace.auth.dto.LoginRequest;
import com.marketplace.auth.dto.UserResponse;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserClient userClient;

    public AuthService(CredentialRepository credentialRepository, PasswordEncoder passwordEncoder, UserClient userClient) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userClient = userClient;
    }

    public String mensaje() {
        return "Hola desde Auth Service - capa Service";
    }

    public UserResponse buscarUsuarioParaLogin(LoginRequest request) {

        return userClient.obtenerUsuarioPorEmail(request.getEmail());
    }

    public Credential buscarCredentialParaLogin(Long userId) {

        return credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Las credenciales no existen"));
    }

    public boolean verificarPassword(String password, String passwordHash) {

        return passwordEncoder.matches(password, passwordHash);
    }

    public boolean autenticar(LoginRequest request) {

        UserResponse user = buscarUsuarioParaLogin(request);

        Credential credential = buscarCredentialParaLogin(user.getId());

        return verificarPassword(
                request.getPassword(),
                credential.getPasswordHash()
        );
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