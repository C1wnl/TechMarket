package com.marketplace.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generarToken(Long userId, String rol) {

        Date ahora = new Date();
        Date expiracion = new Date(
                ahora.getTime() + 1000L * 60 * 60
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validarToken(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}