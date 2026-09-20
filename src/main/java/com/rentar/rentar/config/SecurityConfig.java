package com.rentar.rentar.config;

import com.rentar.rentar.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig (JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter  = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // sin sesion de servidor el token es el estado
                .sessionManagement(sm
                        -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- PUBLICOS ---
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/graphiql/**",
                                "/graphql/**",
                                "/error"
                        ).permitAll()

                        // ABM VEHICULOS: solo admin
                        .requestMatchers("/api/vehiculos/**").hasRole("ADMINISTRADOR")

                        // ABM CLIENTES: solo admin
                        .requestMatchers("/api/clientes/**").hasRole("ADMINISTRADOR")

                        // RESERVAS: solo cliente
                        .requestMatchers("/api/reservas/**").hasRole("CLIENTE")

                        //Todo lo demas requiere autenticacion
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
