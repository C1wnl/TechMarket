package com.marketplace.auth.service;

import com.marketplace.auth.client.UserClient;
import com.marketplace.auth.dto.LoginRequest;
import com.marketplace.auth.dto.LoginResponse;
import com.marketplace.auth.dto.RegisterRequest;
import com.marketplace.auth.dto.RegisterResponse;
import com.marketplace.auth.dto.RegisterUserRequest;
import com.marketplace.auth.dto.UserResponse;
import com.marketplace.auth.model.Credential;
import com.marketplace.auth.repository.CredentialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserClient userClient;
    private final JwtService jwtService;

    public AuthService(
            CredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            UserClient userClient,
            JwtService jwtService) {

        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userClient = userClient;
        this.jwtService = jwtService;
    }

    public UserResponse buscarUsuarioParaLogin(LoginRequest request) {

        return userClient.obtenerUsuarioPorEmail(request.getEmail());
    }

    public Credential buscarCredentialParaLogin(Long userId) {

        return credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Correo o contraseña incorrectos"
                ));
    }

    public boolean verificarPassword(
            String password,
            String passwordHash) {

        return passwordEncoder.matches(password, passwordHash);
    }

    public LoginResponse autenticar(LoginRequest request) {

        UserResponse user = buscarUsuarioParaLogin(request);

        Credential credential = buscarCredentialParaLogin(user.getId());

        if (!verificarPassword(
                request.getPassword(),
                credential.getPasswordHash())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }

        String token = jwtService.generarToken(
                user.getId(),
                user.getRol()
        );

        LoginResponse response = new LoginResponse();

        response.setToken(token);
        response.setUserId(user.getId());
        response.setRol(user.getRol());

        return response;
    }

    public RegisterResponse registrar(RegisterRequest request) {

        RegisterUserRequest userRequest = new RegisterUserRequest();

        userRequest.setNombre(request.getNombre());
        userRequest.setEmail(request.getEmail());

        UserResponse user = userClient.crearUsuario(userRequest);

        Credential credential = new Credential();

        credential.setUserId(user.getId());

        credential.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        credentialRepository.save(credential);

        RegisterResponse response = new RegisterResponse();

        response.setMessage("Usuario registrado correctamente");
        response.setUserId(user.getId());

        return response;
    }
}