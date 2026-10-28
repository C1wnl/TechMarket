package com.marketplace.order.client;

import com.marketplace.order.dto.ProductResponse;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductClient {
    private final RestClient restClient;
    private final String internalKey;
    public ProductClient(@org.springframework.beans.factory.annotation.Qualifier("productRestClient") RestClient productRestClient, @Value("${stock.internal.key}") String internalKey) {
        this.restClient = productRestClient; this.internalKey = internalKey;
    }
    public ProductResponse obtenerProducto(Long id) {
        try {
            ProductResponse product = restClient.get().uri("/products/{id}", id)
                    .retrieve().body(ProductResponse.class);
            if (product == null || !id.equals(product.id()) || product.nombre() == null
                    || product.nombre().isBlank() || product.nombre().length() > 100
                    || product.precio() == null || product.precio().signum() <= 0
                    || product.precio().stripTrailingZeros().scale() > 2
                    || product.precio().compareTo(new java.math.BigDecimal("9999999999.99")) > 0
                    || product.stock() == null || product.stock() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Productos devolvió datos inválidos");
            return product;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto " + id + " no existe");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible consultar el producto " + id);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Productos no responde");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta inválida de Productos");
        }
    }
    public void reservar(Long orderId, Long productId, Integer quantity) {
        call(orderId, productId, "reserve", Map.of("quantity",quantity));
    }
    public void liberar(Long orderId, Long productId) { call(orderId, productId, "release", null); }
    public void confirmar(Long orderId, Long productId) { call(orderId, productId, "confirm", null); }
    private void call(Long orderId, Long productId, String action, Object body) {
        try {
            var spec = restClient.post().uri("/internal/reservations/{orderId}/products/{productId}/{action}",
                            orderId, productId, action).header("X-Internal-Key", internalKey);
            if (body != null) spec.body(body).retrieve().toBodilessEntity();
            else spec.retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value()==409) throw new ResponseStatusException(HttpStatus.CONFLICT, "Conflicto en reserva " + productId);
            if (ex.getStatusCode().value()==404) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva o producto inexistente");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error en " + action + " de reserva");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Resultado de " + action + " incierto. Reintentar con el mismo orderId");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta inválida de Productos");
        }
    }
}
