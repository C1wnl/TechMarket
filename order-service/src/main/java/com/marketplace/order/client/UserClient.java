
package com.marketplace.order.client;

import com.marketplace.order.dto.InternalUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserClient {

    private final RestClient restClient;
    private final String internalKey;

    public UserClient(
            @org.springframework.beans.factory.annotation.Qualifier("userRestClient")
            RestClient userRestClient,
            @Value("${user.internal.key}") String internalKey
    ) {
        this.restClient = userRestClient;
        this.internalKey = internalKey;
    }

    public InternalUserResponse obtenerUsuario(Long userId) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Identificador de usuario inválido"
            );
        }

        try {
            InternalUserResponse user = restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .header("X-Internal-Key", internalKey)
                    .retrieve()
                    .body(InternalUserResponse.class);

            if (user == null
                    || user.nombre() == null
                    || user.nombre().isBlank()
                    || user.email() == null
                    || user.email().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "User Service devolvió datos inválidos"
                );
            }

            return user;

        } catch (RestClientResponseException ex) {

            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No se encontró el comprador del pedido"
                );
            }

            if (ex.getStatusCode().value() == 401
                    || ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Falló la autenticación interna con User Service"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar los datos del comprador"
            );

        } catch (ResourceAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "User Service no está disponible"
            );

        } catch (RestClientException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta inválida de User Service"
            );
        }
    }
}

