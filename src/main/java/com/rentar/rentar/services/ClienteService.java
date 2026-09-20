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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class ClienteService {

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
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + request.getEmail());
        }
        if (clienteRepository.existsByDocumento(request.getDocumento())) {
            throw new DuplicateResourceException("Ya existe un cliente con el documento: " + request.getDocumento());
        }


        Usuario usuario = new Usuario(request.getEmail(), passwordEncoder.encode(request.getPassword()), "CLIENTE");
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(
                usuario,
                request.getDocumento(),
                request.getNombre(),
                request.getApellido(),
                request.getTelefono(),
                request.getFechaNacimiento()
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
        Cliente cliente = obtenerClienteOrThrow(id);
        return ClienteResponse.fromEntity(cliente);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteUpdateRequest request) {
        Cliente cliente = obtenerClienteOrThrow(id);

        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setTelefono(request.getTelefono());
        cliente.setFechaNacimiento(request.getFechaNacimiento());

        cliente = clienteRepository.save(cliente);
        return ClienteResponse.fromEntity(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        // Baja lógica: se marca inactivo tanto el cliente como su usuario asociado,
        // en lugar de borrar el registro de la base de datos.
        Cliente cliente = obtenerClienteOrThrow(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        Usuario usuario = cliente.getUsuario();
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private Cliente obtenerClienteOrThrow(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }
}
