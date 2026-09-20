package com.rentar.rentar.repositories;

import com.rentar.rentar.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    boolean existsByUsuarioId(Long usuarioId);

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    Optional<Cliente> findByUsuarioEmail(String email);
}
