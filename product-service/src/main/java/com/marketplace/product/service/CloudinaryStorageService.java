package com.marketplace.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map<String, String> subirImagen(MultipartFile archivo)
            throws IOException {

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar una imagen.");
        }

        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "La imagen no puede superar los 5 MB."
            );
        }

        String tipo = archivo.getContentType();

        if (tipo == null || !(
                tipo.equals("image/jpeg") ||
                        tipo.equals("image/png") ||
                        tipo.equals("image/webp")
        )) {
            throw new IllegalArgumentException(
                    "Solo se permiten imágenes JPG, PNG o WEBP."
            );
        }

        Map<?, ?> resultado = cloudinary.uploader().upload(
                archivo.getBytes(),
                ObjectUtils.asMap(
                        "folder", "techmarket/products",
                        "resource_type", "image"
                )
        );

        String imagenUrl = (String) resultado.get("secure_url");
        String publicId = (String) resultado.get("public_id");

        return Map.of(
                "imagenUrl", imagenUrl,
                "publicId", publicId
        );
    }
}
