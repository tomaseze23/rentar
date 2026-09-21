package com.rentar.rentar.controllers;

import com.rentar.rentar.dtos.LoginRequest;
import com.rentar.rentar.dtos.LoginResponse;
import com.rentar.rentar.entities.Usuario;
import com.rentar.rentar.repositories.UsuarioRepository;
import com.rentar.rentar.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/auth")
@Tag(name = "Autenticación", description = "Login y obtención del token JWT")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private JwtService jwtService;

    public AuthController (UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));
        if(!usuario.isActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
