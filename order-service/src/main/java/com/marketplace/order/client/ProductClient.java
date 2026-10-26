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

    public ProductClient(RestClient productRestClient, @Value("${stock.internal.key}") String internalKey) {
        this.restClient = productRestClient;
        this.internalKey = internalKey;
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
                    || product.stock() == null || product.stock() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Productos devolvió datos incompletos o inválidos");
            }
            return product;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El producto " + id + " no existe");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar el producto " + id);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Productos no responde. No se guardó el pedido; intenta de nuevo más tarde");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Productos devolvió una respuesta que no se pudo interpretar");
        }
    }
    public void reservar(Long id, Integer quantity) { actualizarStock(id, quantity, "reserve"); }

    public void liberar(Long id, Integer quantity) { actualizarStock(id, quantity, "release"); }

    private void actualizarStock(Long id, Integer quantity, String action) {
        try {
            restClient.post().uri("/internal/products/{id}/{action}", id, action)
                    .header("X-Internal-Key", internalKey)
                    .body(Map.of("quantity", quantity))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para producto " + id);
            if (ex.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto " + id + " no existe");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al " + action + " stock del producto " + id);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar la operación de stock del producto " + id + "; revisa el estado antes de reintentar");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta inválida de Productos");
        }
    }

}
