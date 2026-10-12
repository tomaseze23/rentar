package com.rentar.customer.services;

import com.rentar.customer.dtos.ClienteRequest;
import com.rentar.customer.dtos.ClienteResponse;
import com.rentar.customer.dtos.ClienteUpdateRequest;
import com.rentar.customer.dtos.VerificacionCliente;
import com.rentar.customer.entities.Cliente;
import com.rentar.customer.entities.Usuario;
import com.rentar.customer.exceptions.BusinessRuleException;
import com.rentar.customer.exceptions.DuplicateResourceException;
import com.rentar.customer.exceptions.InvalidArgumentException;
import com.rentar.customer.exceptions.ResourceNotFoundException;
import com.rentar.customer.repositories.ClienteRepository;
import com.rentar.customer.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de negocio del dominio clientes. Es la misma lógica del Hito 1
 * (ClienteService del monolito), ahora detrás del contrato gRPC.
 */
@Service
public class ClienteService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        requerido(request.email(), "email");
        requerido(request.password(), "password");
        requerido(request.documento(), "documento");
        requerido(request.nombre(), "nombre");
        requerido(request.apellido(), "apellido");
        if (!EMAIL.matcher(request.email()).matches()) {
            throw new InvalidArgumentException("El email no tiene un formato válido: " + request.email());
        }
        validarFechaNacimiento(request.fechaNacimiento());

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + request.email());
        }
        if (clienteRepository.existsByDocumento(request.documento())) {
            throw new DuplicateResourceException("Ya existe un cliente con el documento: " + request.documento());
        }

        Usuario usuario = new Usuario(request.email(), passwordEncoder.encode(request.password()), "CLIENTE");
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(
                usuario,
                request.documento(),
                request.nombre(),
                request.apellido(),
                request.telefono(),
                request.fechaNacimiento()
        );
        cliente = clienteRepository.save(cliente);

        return ClienteResponse.fromEntity(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarActivos() {
        return clienteRepository.findAll().stream()
                .filter(Cliente::isActivo)
                .map(ClienteResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.fromEntity(obtenerClienteOrThrow(id));
    }

    /**
     * Verifica si el cliente existe y está activo (cliente y usuario). Es la consulta
     * que hace el Gateway antes de crear una reserva.
     */
    @Transactional(readOnly = true)
    public VerificacionCliente verificarActivo(Long id) {
        return clienteRepository.findById(id)
                .map(cliente -> new VerificacionCliente(true, cliente.isActivo() && cliente.getUsuario().isActivo()))
                .orElseGet(() -> new VerificacionCliente(false, false));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteUpdateRequest request) {
        requerido(request.nombre(), "nombre");
        requerido(request.apellido(), "apellido");
        validarFechaNacimiento(request.fechaNacimiento());

        Cliente cliente = obtenerClienteOrThrow(id);

        cliente.setNombre(request.nombre());
        cliente.setApellido(request.apellido());
        cliente.setTelefono(request.telefono());
        cliente.setFechaNacimiento(request.fechaNacimiento());

        cliente = clienteRepository.save(cliente);
        return ClienteResponse.fromEntity(cliente);
    }

    @Transactional
    public ClienteResponse darDeBaja(Long id) {
        // Baja lógica: se marca inactivo tanto el cliente como su usuario asociado,
        // en lugar de borrar el registro de la base de datos.
        Cliente cliente = obtenerClienteOrThrow(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        Usuario usuario = cliente.getUsuario();
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        return ClienteResponse.fromEntity(cliente);
    }

    @Transactional
    public ClienteResponse reactivar(Long id) {
        // Revierte la baja lógica, el cliente y su usuario vuelven a quedar activos.
        Cliente cliente = obtenerClienteOrThrow(id);
        if (cliente.isActivo()) {
            throw new BusinessRuleException("El cliente ya se encuentra activo");
        }
        cliente.setActivo(true);
        cliente = clienteRepository.save(cliente);

        Usuario usuario = cliente.getUsuario();
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        return ClienteResponse.fromEntity(cliente);
    }

    private Cliente obtenerClienteOrThrow(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    private static void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new InvalidArgumentException("El campo '" + campo + "' es obligatorio");
        }
    }

    private static void validarFechaNacimiento(LocalDate fechaNacimiento) {
        if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.now())) {
            throw new InvalidArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }
}
