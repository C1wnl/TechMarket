package com.marketplace.user.config;

import com.marketplace.user.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(
                                        HttpStatus.UNAUTHORIZED.value(),
                                        "Debes iniciar sesión para acceder a este recurso"
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(
                                        HttpStatus.FORBIDDEN.value(),
                                        "No tienes permisos para acceder a este recurso"
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Permitir los despachos internos de errores
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Permitir el registro de usuarios desde auth-service
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()

                        // Permitir la consulta por correo desde auth-service
                        .requestMatchers(HttpMethod.GET, "/users/email").permitAll()

                        // Solo ADMIN puede consultar la lista completa
                        .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")

                        // Solo ADMIN puede consultar un usuario por ID
                        .requestMatchers(HttpMethod.GET, "/users/{id}").hasRole("ADMIN")

                        // Proteger las demás rutas de usuarios
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // El resto de las rutas requieren autenticación
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}

