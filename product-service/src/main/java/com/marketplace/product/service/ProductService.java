package com.marketplace.product.service;

import com.marketplace.product.model.Product;
import com.marketplace.product.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product crearProducto(Product product) {
        return productRepository.save(product);
    }

    public List<Product> obtenerProductos() {
        return productRepository.findAll(Sort.by("id"));
    }

    public Product obtenerProductoPorId(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El producto no existe"
                ));
    }
}
