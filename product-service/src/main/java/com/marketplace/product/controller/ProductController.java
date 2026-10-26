package com.marketplace.product.controller;

import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.model.Product;
import com.marketplace.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product crearProducto(@Valid @RequestBody ProductRequest request) {
        Product product = new Product();
        product.setNombre(request.getNombre());
        product.setDescripcion(request.getDescripcion());
        product.setPrecio(request.getPrecio());
        product.setStock(request.getStock());
        return productService.crearProducto(product);
    }

    @GetMapping
    public List<Product> obtenerProductos() {
        return productService.obtenerProductos();
    }

    @GetMapping("/{id}")
    public Product obtenerProductoPorId(@PathVariable Long id) {
        return productService.obtenerProductoPorId(id);
    }
}
