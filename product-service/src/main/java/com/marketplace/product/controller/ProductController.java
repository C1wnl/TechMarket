
package com.marketplace.product.controller;

import com.marketplace.product.dto.ProductRequest;
import com.marketplace.product.model.Product;
import com.marketplace.product.service.CloudinaryStorageService;
import com.marketplace.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CloudinaryStorageService cloudinaryStorageService;

    public ProductController(
            ProductService productService,
            CloudinaryStorageService cloudinaryStorageService
    ) {
        this.productService = productService;
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product crearProducto(@Valid @RequestBody ProductRequest request) {
        Product product = new Product();
        product.setNombre(request.getNombre());
        product.setDescripcion(request.getDescripcion());
        product.setPrecio(request.getPrecio());
        product.setStock(request.getStock());
        product.setImagenUrl(request.getImagenUrl());

        return productService.crearProducto(product);
    }

    @PostMapping("/images")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> subirImagen(
            @RequestParam("imagen") MultipartFile imagen
    ) throws IOException {
        return cloudinaryStorageService.subirImagen(imagen);
    }

    @GetMapping
    public List<Product> obtenerProductos() {
        return productService.obtenerProductos();
    }

    @GetMapping("/{id}")
    public Product obtenerProductoPorId(@PathVariable Long id) {
        return productService.obtenerProductoPorId(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Product actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        Product product = new Product();
        product.setNombre(request.getNombre());
        product.setDescripcion(request.getDescripcion());
        product.setPrecio(request.getPrecio());
        product.setStock(request.getStock());
        product.setImagenUrl(request.getImagenUrl());

        return productService.actualizarProducto(id, product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarProducto(@PathVariable Long id) {
        productService.eliminarProducto(id);
    }
}
