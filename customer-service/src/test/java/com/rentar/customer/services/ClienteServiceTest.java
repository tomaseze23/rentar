package com.rentar.customer.services;

import com.rentar.customer.dtos.ClienteRequest;
import com.rentar.customer.dtos.ClienteResponse;
import com.rentar.customer.dtos.ClienteUpdateRequest;
import com.rentar.customer.dtos.VerificacionCliente;
import com.rentar.customer.entities.Cliente;
import com.rentar.customer.entities.Usuario;
import com.rentar.customer.exceptions.DuplicateResourceException;
import com.rentar.customer.exceptions.InvalidArgumentException;
import com.rentar.customer.exceptions.ResourceNotFoundException;
import com.rentar.customer.repositories.ClienteRepository;
import com.rentar.customer.repositories.UsuarioRepository;
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
import com.rentar.customer.exceptions.BusinessRuleException;

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
        return new ClienteRequest("juan.perez@mail.com", "secreto123", "30123456", "Juan", "Pérez",
                "1122334455", LocalDate.of(1990, 5, 20));
    }

    @Test
    void crear_deberiaGuardarUsuarioYClienteCuandoNoHayDuplicados() {
        ClienteRequest request = buildRequest();

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(clienteRepository.existsByDocumento(request.documento())).thenReturn(false);

        Usuario usuarioGuardado = new Usuario(request.email(), request.password(), "CLIENTE");
        usuarioGuardado.setId(1L);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        ClienteResponse response = clienteService.crear(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("juan.perez@mail.com");
        assertThat(response.documento()).isEqualTo("30123456");
        assertThat(response.activo()).isTrue();

        verify(usuarioRepository).save(any(Usuario.class));
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void crear_deberiaLanzarExcepcionSiEmailYaExiste() {
        ClienteRequest request = buildRequest();
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.email());

        verify(clienteRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_deberiaLanzarExcepcionSiDocumentoYaExiste() {
        ClienteRequest request = buildRequest();
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(clienteRepository.existsByDocumento(request.documento())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.documento());

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

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.nombre()).isEqualTo("Ana");
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
        assertThat(resultado.get(0).nombre()).isEqualTo("Activo");
    }

    @Test
    void actualizar_deberiaModificarDatosPropiosDelCliente() {
        Usuario usuario = new Usuario("c@mail.com", "p", "CLIENTE");
        usuario.setId(3L);
        Cliente cliente = new Cliente(usuario, "333", "Viejo", "Nombre", "111", LocalDate.of(1990, 1, 1));
        cliente.setId(7L);

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteUpdateRequest update = new ClienteUpdateRequest("Nuevo", "Apellido", "222", LocalDate.of(1991, 2, 2));

        ClienteResponse response = clienteService.actualizar(7L, update);

        assertThat(response.nombre()).isEqualTo("Nuevo");
        assertThat(response.apellido()).isEqualTo("Apellido");
        assertThat(response.telefono()).isEqualTo("222");
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

        clienteService.darDeBaja(8L);

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

        assertThatThrownBy(() -> clienteService.darDeBaja(123L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(usuarioRepository, never()).save(any());
    }

     @Test
    void reactivar_deberiaActivarClienteYUsuario() {
        Usuario usuario = new Usuario("e@mail.com", "p", "CLIENTE");
        usuario.setId(5L);
        usuario.setActivo(false);
        Cliente cliente = new Cliente(usuario, "555", "Nom", "Ape", null, null);
        cliente.setId(9L);
        cliente.setActivo(false);

        when(clienteRepository.findById(9L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = clienteService.reactivar(9L);

        assertThat(response.activo()).isTrue();
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().isActivo()).isTrue();
    }

    @Test
    void reactivar_deberiaRechazarUnClienteYaActivo() {
        Usuario usuario = new Usuario("f@mail.com", "p", "CLIENTE");
        Cliente cliente = new Cliente(usuario, "666", "Nom", "Ape", null, null);
        cliente.setId(10L);

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> clienteService.reactivar(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya se encuentra activo");

        verify(clienteRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_deberiaRechazarDatosObligatoriosFaltantes() {
        ClienteRequest request = new ClienteRequest("juan.perez@mail.com", "secreto123", "30123456", " ", "Pérez",
                null, null);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("nombre");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_deberiaRechazarEmailConFormatoInvalido() {
        ClienteRequest request = new ClienteRequest("no-es-un-email", "secreto123", "30123456", "Juan", "Pérez",
                null, null);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void crear_deberiaRechazarFechaDeNacimientoFutura() {
        ClienteRequest request = new ClienteRequest("juan.perez@mail.com", "secreto123", "30123456", "Juan", "Pérez",
                null, LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("fecha de nacimiento");
    }

    @Test
    void verificarActivo_clienteActivoPuedeAlquilar() {
        Usuario usuario = new Usuario("g@mail.com", "p", "CLIENTE");
        Cliente cliente = new Cliente(usuario, "777", "Nom", "Ape", null, null);
        cliente.setId(11L);
        when(clienteRepository.findById(11L)).thenReturn(Optional.of(cliente));

        VerificacionCliente verificacion = clienteService.verificarActivo(11L);

        assertThat(verificacion.existe()).isTrue();
        assertThat(verificacion.activo()).isTrue();
    }

    @Test
    void verificarActivo_clienteDadoDeBajaNoPuedeAlquilar() {
        Usuario usuario = new Usuario("h@mail.com", "p", "CLIENTE");
        Cliente cliente = new Cliente(usuario, "888", "Nom", "Ape", null, null);
        cliente.setId(12L);
        cliente.setActivo(false);
        when(clienteRepository.findById(12L)).thenReturn(Optional.of(cliente));

        VerificacionCliente verificacion = clienteService.verificarActivo(12L);

        assertThat(verificacion.existe()).isTrue();
        assertThat(verificacion.activo()).isFalse();
    }

    @Test
    void verificarActivo_clienteInexistenteNoLanzaExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        VerificacionCliente verificacion = clienteService.verificarActivo(99L);

        assertThat(verificacion.existe()).isFalse();
        assertThat(verificacion.activo()).isFalse();
    }
}
