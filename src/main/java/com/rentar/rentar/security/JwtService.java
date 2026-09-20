package com.rentar.rentar.security;

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

    private final SecretKey key;
    private final long expiracionMs;

    public JwtService(
            @Value("${jwt.secret}")String secret,
            @Value("${jwt.expiration-ms:86400000}") long expiracionMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs; // 24 horas por defecto
    }

    // Genera un token: email como subject, rol como claim
    public String generarToken (String email, String rol) {
                Date ahora = new Date();
                Date vencimiento = new Date(ahora.getTime() + expiracionMs);
                return Jwts.builder()
                        .subject(email)
                        .claim("rol", rol)
                        .issuedAt(ahora)
                        .expiration(vencimiento)
                        .signWith(key)
                        .compact();
    }

    public String extraerEmail (String token) {
        return parseClaims(token).getSubject();
    }

    public String extraerRol (String token) {
        return parseClaims(token).get("rol", String.class);
    }

    // true si la firma es válida y no venció
    public boolean tokenValido (String token) {
        try {
            parseClaims (token);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    private Claims parseClaims (String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
