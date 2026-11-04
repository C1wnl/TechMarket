
package com.marketplace.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${payment.internal.key}")
    private String internalKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        boolean isCreatePayment =
                "POST".equals(request.getMethod())
                        && "/payments".equals(request.getServletPath());

        if (isCreatePayment) {

            String providedKey = request.getHeader("X-Internal-Key");

            if (providedKey != null
                    && !providedKey.isBlank()
                    && internalKey != null
                    && !internalKey.isBlank()
                    && MessageDigest.isEqual(
                    providedKey.getBytes(StandardCharsets.UTF_8),
                    internalKey.getBytes(StandardCharsets.UTF_8)
            )) {

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                "order-service",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
