package com.rentar.customer.services;

import com.rentar.customer.dtos.UsuarioAutenticado;
import com.rentar.customer.entities.Cliente;
import com.rentar.customer.entities.Usuario;
import com.rentar.customer.exceptions.AuthenticationException;
import com.rentar.customer.repositories.ClienteRepository;
import com.rentar.customer.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida credenciales de usuarios. No genera el JWT: eso lo hace el API Gateway
 * con los datos que devuelve este servicio.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       ClienteRepository clienteRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticado autenticar(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Email y contraseña son obligatorios");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Email o contraseña incorrectos"));
        if (!usuario.isActivo()) {
            throw new AuthenticationException("Usuario inactivo");
        }
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new AuthenticationException("Email o contraseña incorrectos");
        }

        Long clienteId = clienteRepository.findByUsuarioId(usuario.getId())
                .map(Cliente::getId)
                .orElse(null);

        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getRol(), clienteId);
    }
}
