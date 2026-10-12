package com.rentar.customer.services;

import com.rentar.customer.dtos.UsuarioAutenticado;
import com.rentar.customer.entities.Cliente;
import com.rentar.customer.entities.Usuario;
import com.rentar.customer.exceptions.AuthenticationException;
import com.rentar.customer.repositories.ClienteRepository;
import com.rentar.customer.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, clienteRepository, passwordEncoder);
    }

    private Usuario usuario(String rol, boolean activo) {
        Usuario usuario = new Usuario("ana@mail.com", "hash", rol);
        usuario.setId(1L);
        usuario.setActivo(activo);
        return usuario;
    }

    @Test
    void autenticar_clienteConCredencialesCorrectasDevuelveRolYClienteId() {
        Usuario usuario = usuario("CLIENTE", true);
        Cliente cliente = new Cliente(usuario, "123", "Ana", "Gómez", null, null);
        cliente.setId(7L);
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreto", "hash")).thenReturn(true);
        when(clienteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(cliente));

        UsuarioAutenticado autenticado = authService.autenticar("ana@mail.com", "secreto");

        assertThat(autenticado.rol()).isEqualTo("CLIENTE");
        assertThat(autenticado.clienteId()).isEqualTo(7L);
    }

    @Test
    void autenticar_adminNoTieneClienteAsociado() {
        Usuario admin = usuario("ADMINISTRADOR", true);
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secreto", "hash")).thenReturn(true);
        when(clienteRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        UsuarioAutenticado autenticado = authService.autenticar("ana@mail.com", "secreto");

        assertThat(autenticado.rol()).isEqualTo("ADMINISTRADOR");
        assertThat(autenticado.clienteId()).isNull();
    }

    @Test
    void autenticar_rechazaPasswordIncorrecta() {
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario("CLIENTE", true)));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar("ana@mail.com", "mala"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("incorrectos");
    }

    @Test
    void autenticar_rechazaEmailInexistente() {
        when(usuarioRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar("nadie@mail.com", "secreto"))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void autenticar_rechazaUsuarioInactivo() {
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario("CLIENTE", false)));

        assertThatThrownBy(() -> authService.autenticar("ana@mail.com", "secreto"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void autenticar_rechazaDatosVacios() {
        assertThatThrownBy(() -> authService.autenticar("", ""))
                .isInstanceOf(AuthenticationException.class);
    }
}
