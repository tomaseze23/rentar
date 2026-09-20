package com.rentar.rentar.services;

import com.rentar.rentar.dtos.ClienteRequest;
import com.rentar.rentar.dtos.ClienteResponse;
import com.rentar.rentar.dtos.ClienteUpdateRequest;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.Usuario;
import com.rentar.rentar.exceptions.DuplicateResourceException;
import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.repositories.ClienteRepository;
import com.rentar.rentar.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository, usuarioRepository, passwordEncoder);
    }

    private ClienteRequest buildRequest() {
        ClienteRequest request = new ClienteRequest();
        request.setEmail("juan.perez@mail.com");
        request.setPassword("secreto123");
        request.setDocumento("30123456");
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setTelefono("1122334455");
        request.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        return request;
    }

    @Test
    void crear_deberiaGuardarUsuarioYClienteCuandoNoHayDuplicados() {
        ClienteRequest request = buildRequest();

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(clienteRepository.existsByDocumento(request.getDocumento())).thenReturn(false);

        Usuario usuarioGuardado = new Usuario(request.getEmail(), request.getPassword(), "CLIENTE");
        usuarioGuardado.setId(1L);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        ClienteResponse response = clienteService.crear(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUsuarioId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("juan.perez@mail.com");
        assertThat(response.getDocumento()).isEqualTo("30123456");
        assertThat(response.isActivo()).isTrue();

        verify(usuarioRepository).save(any(Usuario.class));
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void crear_deberiaLanzarExcepcionSiEmailYaExiste() {
        ClienteRequest request = buildRequest();
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.getEmail());

        verify(clienteRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_deberiaLanzarExcepcionSiDocumentoYaExiste() {
        ClienteRequest request = buildRequest();
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(clienteRepository.existsByDocumento(request.getDocumento())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.getDocumento());

        verify(clienteRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void buscarPorId_deberiaLanzarExcepcionSiNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscarPorId_deberiaDevolverClienteSiExiste() {
        Usuario usuario = new Usuario("ana@mail.com", "pass", "CLIENTE");
        usuario.setId(2L);
        Cliente cliente = new Cliente(usuario, "40111222", "Ana", "Gómez", "1155667788", LocalDate.of(1995, 1, 1));
        cliente.setId(5L);

        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));

        ClienteResponse response = clienteService.buscarPorId(5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getNombre()).isEqualTo("Ana");
    }

    @Test
    void listarActivos_deberiaFiltrarSoloClientesActivos() {
        Usuario u1 = new Usuario("a@mail.com", "p", "CLIENTE");
        u1.setId(1L);
        Cliente activo = new Cliente(u1, "111", "Activo", "Uno", null, null);
        activo.setId(1L);
        activo.setActivo(true);

        Usuario u2 = new Usuario("b@mail.com", "p", "CLIENTE");
        u2.setId(2L);
        Cliente inactivo = new Cliente(u2, "222", "Inactivo", "Dos", null, null);
        inactivo.setId(2L);
        inactivo.setActivo(false);

        when(clienteRepository.findAll()).thenReturn(List.of(activo, inactivo));

        List<ClienteResponse> resultado = clienteService.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Activo");
    }

    @Test
    void actualizar_deberiaModificarDatosPropiosDelCliente() {
        Usuario usuario = new Usuario("c@mail.com", "p", "CLIENTE");
        usuario.setId(3L);
        Cliente cliente = new Cliente(usuario, "333", "Viejo", "Nombre", "111", LocalDate.of(1990, 1, 1));
        cliente.setId(7L);

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteUpdateRequest update = new ClienteUpdateRequest();
        update.setNombre("Nuevo");
        update.setApellido("Apellido");
        update.setTelefono("222");
        update.setFechaNacimiento(LocalDate.of(1991, 2, 2));

        ClienteResponse response = clienteService.actualizar(7L, update);

        assertThat(response.getNombre()).isEqualTo("Nuevo");
        assertThat(response.getApellido()).isEqualTo("Apellido");
        assertThat(response.getTelefono()).isEqualTo("222");
    }

    @Test
    void eliminar_deberiaMarcarInactivoClienteYUsuario() {
        Usuario usuario = new Usuario("d@mail.com", "p", "CLIENTE");
        usuario.setId(4L);
        usuario.setActivo(true);
        Cliente cliente = new Cliente(usuario, "444", "Nom", "Ape", null, null);
        cliente.setId(8L);
        cliente.setActivo(true);

        when(clienteRepository.findById(8L)).thenReturn(Optional.of(cliente));

        clienteService.eliminar(8L);

        ArgumentCaptor<Cliente> clienteCaptor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(clienteCaptor.capture());
        assertThat(clienteCaptor.getValue().isActivo()).isFalse();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().isActivo()).isFalse();
    }

    @Test
    void eliminar_deberiaLanzarExcepcionSiClienteNoExiste() {
        when(clienteRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.eliminar(123L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(usuarioRepository, never()).save(any());
    }
}
