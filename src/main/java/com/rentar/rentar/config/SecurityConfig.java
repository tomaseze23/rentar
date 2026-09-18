package com.rentar.rentar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Desactivar CSRF para APIs REST
                .authorizeHttpRequests(auth -> auth
                        // Dejar rutas de documentación y GraphQL públicas por ahora
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/graphiql/**",
                                "/graphql/**",
                                "/api/vehiculos/**",
                                "/api/reservas/**",
                                "/error",
                                // TODO: sacar de la whitelist cuando se integre JWT.
                                // Por ahora públicos para poder desarrollar/probar el ABM de clientes.
                                "/api/clientes/**"
                        ).permitAll()
                        // El resto de los endpoints requerirán autenticación (para cuando sumen JWT)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
