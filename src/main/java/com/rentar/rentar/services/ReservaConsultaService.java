package com.rentar.rentar.services;

import com.rentar.rentar.dtos.HistorialAlquilerResponse;
import com.rentar.rentar.dtos.ReservaConsultaResponse;
import com.rentar.rentar.dtos.ReservaFiltro;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.Reserva;
import com.rentar.rentar.entities.Usuario;
import com.rentar.rentar.repositories.ClienteRepository;
import com.rentar.rentar.repositories.ReservaRepository;
import com.rentar.rentar.repositories.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ReservaConsultaService {
    private final ReservaRepository reservas;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;

    public ReservaConsultaService(ReservaRepository reservas,
                                  ClienteRepository clientes,
                                  UsuarioRepository usuarios) {
        this.reservas = reservas;
        this.clientes = clientes;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<ReservaConsultaResponse> consultar(ReservaFiltro filtro, Authentication autenticacion) {
        Usuario usuario = usuarioAutenticado(autenticacion);
        ReservaFiltro f = filtro == null ? new ReservaFiltro() : filtro;
        Long clienteId = f.getClienteId();

        if (esCliente(usuario)) {
            Long idPropio = clienteDe(usuario).getId();
            if (clienteId != null && !Objects.equals(clienteId, idPropio)) {
                throw new AccessDeniedException("No podés consultar reservas de otro cliente");
            }
            clienteId = idPropio;
        } else if (!esAdministrador(usuario)) {
            throw new AccessDeniedException("El rol no está autorizado para consultar reservas");
        }

        LocalDateTime desde = parseFecha(f.getFechaDesde(), "fechaDesde");
        LocalDateTime hasta = parseFecha(f.getFechaHasta(), "fechaHasta");
        if (desde != null && hasta != null && !hasta.isAfter(desde)) {
            throw new IllegalArgumentException("fechaHasta debe ser posterior a fechaDesde");
        }

        final Long idFiltrado = clienteId;
        Specification<Reserva> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (idFiltrado != null) conditions.add(cb.equal(root.get("cliente").get("id"), idFiltrado));
            if (f.getVehiculoId() != null) conditions.add(cb.equal(root.get("vehiculo").get("id"), f.getVehiculoId()));
            if (f.getTipoVehiculo() != null) conditions.add(cb.equal(root.get("vehiculo").get("tipoVehiculo"), f.getTipoVehiculo()));
            if (f.getEstado() != null) conditions.add(cb.equal(root.get("estado"), f.getEstado()));
            if (desde != null) conditions.add(cb.greaterThan(root.get("fechaFin"), desde));
            if (hasta != null) conditions.add(cb.lessThan(root.get("fechaInicio"), hasta));
            return cb.and(conditions.toArray(new Predicate[0]));
        };

        return reservas.findAll(spec, ordenReciente()).stream().map(this::aConsulta).toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialAlquilerResponse> historial(Authentication autenticacion) {
        Usuario usuario = usuarioAutenticado(autenticacion);
        if (!esCliente(usuario)) {
            throw new AccessDeniedException("El historial corresponde exclusivamente al cliente autenticado");
        }
        Long clienteId = clienteDe(usuario).getId();
        LocalDateTime ahora = LocalDateTime.now();

        Specification<Reserva> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("cliente").get("id"), clienteId),
                cb.or(
                        cb.equal(root.get("estado"), EstadoReserva.CANCELADA),
                        cb.and(cb.equal(root.get("estado"), EstadoReserva.CONFIRMADA),
                               cb.lessThanOrEqualTo(root.get("fechaFin"), ahora))
                )
        );
        return reservas.findAll(spec, ordenReciente()).stream().map(this::aHistorial).toList();
    }

    private Usuario usuarioAutenticado(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Iniciá sesión para consultar reservas");
        }
        String email = auth.getName();
        if (email == null || email.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("La sesión no identifica un email");
        }
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("No existe un usuario asociado a la sesión"));
        if (!usuario.isActivo()) {
            throw new AccessDeniedException("La cuenta está inactiva");
        }
        return usuario;
    }

    private Cliente clienteDe(Usuario usuario) {
        Cliente cliente = clientes.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException("La cuenta no tiene un cliente asociado"));
        if (!cliente.isActivo()) throw new AccessDeniedException("El cliente está inactivo");
        return cliente;
    }

    private boolean esCliente(Usuario usuario) {
        return "CLIENTE".equals(usuario.getRol());
    }

    private boolean esAdministrador(Usuario usuario) {
        return "ADMINISTRADOR".equals(usuario.getRol());
    }

    private LocalDateTime parseFecha(String fecha, String campo) {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDateTime.parse(fecha);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(campo + " debe tener formato ISO-8601, ej. 2026-10-01T10:00");
        }
    }

    private Sort ordenReciente() {
        return Sort.by(Sort.Order.desc("fechaInicio"), Sort.Order.desc("id"));
    }

    private String nombreVehiculo(Reserva reserva) {
        return reserva.getVehiculo().getMarca() + " " + reserva.getVehiculo().getModelo();
    }

    private ReservaConsultaResponse aConsulta(Reserva r) {
        return new ReservaConsultaResponse(
                r.getId(), r.getCliente().getId(),
                r.getCliente().getNombre() + " " + r.getCliente().getApellido(),
                r.getVehiculo().getId(), nombreVehiculo(r), r.getVehiculo().getPatente(),
                r.getFechaInicio().toString(), r.getFechaFin().toString(),
                r.getPrecioDiario().toPlainString(), r.getImporteTotal().toPlainString(),
                r.getEstado().name());
    }

    private HistorialAlquilerResponse aHistorial(Reserva r) {
        String estadoVisible = r.getEstado() == EstadoReserva.CANCELADA ? "CANCELADA" : "FINALIZADA";
        return new HistorialAlquilerResponse(
                r.getId(), nombreVehiculo(r), r.getVehiculo().getPatente(),
                r.getFechaInicio().toString(), r.getFechaFin().toString(),
                diasFacturados(r.getFechaInicio(), r.getFechaFin()),
                r.getImporteTotal().toPlainString(), estadoVisible);
    }

    private long diasFacturados(LocalDateTime inicio, LocalDateTime fin) {
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.isZero() || duracion.isNegative()) {
            throw new IllegalStateException("La reserva " + inicio + " tiene un período inválido");
        }
        long diasCompletos = duracion.toDays();
        return diasCompletos + (duracion.minusDays(diasCompletos).isZero() ? 0 : 1);
    }
}
