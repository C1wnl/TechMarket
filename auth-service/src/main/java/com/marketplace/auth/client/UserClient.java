package com.marketplace.auth.client;

import com.marketplace.auth.dto.UserResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public UserResponse obtenerUsuarioPorEmail(String email) {

        return restClient.get()
                .uri("http://localhost:8082/users/email?email={email}", email)
                .retrieve()
                .body(UserResponse.class);
    }
}