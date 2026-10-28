package com.marketplace.auth.client;

import com.marketplace.auth.dto.RegisterUserRequest;
import com.marketplace.auth.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public UserResponse obtenerUsuarioPorEmail(String email) {

        try {

            return restClient.get()
                    .uri("http://localhost:8082/users/email?email={email}", email)
                    .retrieve()
                    .body(UserResponse.class);

        } catch (HttpClientErrorException.NotFound ex) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }
    }

    public UserResponse crearUsuario(RegisterUserRequest request) {

        try {

            return restClient.post()
                    .uri("http://localhost:8082/users")
                    .body(request)
                    .retrieve()
                    .body(UserResponse.class);

        } catch (HttpClientErrorException.Conflict ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo electrónico ya está registrado"
            );
        }
    }
}