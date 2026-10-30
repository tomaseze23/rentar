package com.rental.rentalservice.services;

import com.rental.rentalservice.entities.EstadoReserva;
import com.rental.rentalservice.entities.ResultadoMantenimiento;
import com.rental.rentalservice.entities.Reserva;
import com.rental.rentalservice.events.MantenimientoResultado;
import com.rental.rentalservice.events.ReservaConfirmada;
import com.rental.rentalservice.repositories.ReservaRepository;
import org.springframework.context.ApplicationEventPublisher;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ReservaService {

    private static final DateTimeFormatter FORMATO_EVENTO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ReservaRepository reservaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReservaService(ReservaRepository reservaRepository, ApplicationEventPublisher eventPublisher) {
        this.reservaRepository = reservaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Reserva crearReserva(long clienteId, long vehiculoId, String fechaInicio, String fechaFin,
                                double precioDiario) {
        validarId(clienteId, "cliente_id");
        validarId(vehiculoId, "vehiculo_id");
        LocalDateTime inicio = parseFecha(fechaInicio, "fecha_inicio");
        LocalDateTime fin = parseFecha(fechaFin, "fecha_fin");
        validarPeriodo(inicio, fin);
        validarPrecio(precioDiario);

        if (reservaRepository.existsConfirmedOverlap(vehiculoId, inicio, fin)) {
            throw status(Status.FAILED_PRECONDITION, "El vehículo ya tiene una reserva confirmada en ese período");
        }

        BigDecimal precio = BigDecimal.valueOf(precioDiario).setScale(2, RoundingMode.HALF_UP);
        long dias = cantidadDias(inicio, fin);
        Reserva reserva = new Reserva();
        reserva.setClienteId(clienteId);
        reserva.setVehiculoId(vehiculoId);
        reserva.setFechaInicio(inicio);
        reserva.setFechaFin(fin);
        reserva.setPrecioDiario(precio);
        reserva.setImporteTotal(precio.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setResultadoMantenimiento(ResultadoMantenimiento.PENDIENTE);
        Reserva guardada = reservaRepository.save(reserva);
        eventPublisher.publishEvent(new ReservaConfirmada(
                guardada.getId(),
                guardada.getVehiculoId(),
                guardada.getFechaInicio().format(FORMATO_EVENTO),
                guardada.getFechaFin().format(FORMATO_EVENTO)));
        return guardada;
    }

    @Transactional
    public void registrarResultadoMantenimiento(MantenimientoResultado evento) {
        validarId(evento.reservaId(), "reserva_id");
        validarId(evento.vehiculoId(), "vehiculo_id");
        ResultadoMantenimiento resultado;
        try {
            resultado = ResultadoMantenimiento.valueOf(evento.resultado());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw status(Status.INVALID_ARGUMENT, "resultado de mantenimiento inválido");
        }
        if (resultado == ResultadoMantenimiento.PENDIENTE) {
            throw status(Status.INVALID_ARGUMENT, "El resultado de mantenimiento no puede ser PENDIENTE");
        }

        Reserva reserva = obtenerPorId(evento.reservaId());
        if (!reserva.getVehiculoId().equals(evento.vehiculoId())) {
            throw status(Status.FAILED_PRECONDITION, "El vehículo no coincide con la reserva");
        }
        if (reserva.getResultadoMantenimiento() == ResultadoMantenimiento.PENDIENTE) {
            reserva.setResultadoMantenimiento(resultado);
            reservaRepository.save(reserva);
        } else if (reserva.getResultadoMantenimiento() != resultado) {
            throw status(Status.FAILED_PRECONDITION, "La reserva ya tiene otro resultado de mantenimiento");
        }
    }

    @Transactional(readOnly = true)
    public Reserva obtenerPorId(long id) {
        validarId(id, "reserva_id");
        return reservaRepository.findById(id)
                .orElseThrow(() -> status(Status.NOT_FOUND, "No existe la reserva " + id));
    }

    @Transactional
    public Reserva cancelarReserva(long id, long clienteId) {
        validarId(clienteId, "cliente_id");
        Reserva reserva = obtenerPorId(id);
        if (!reserva.getClienteId().equals(clienteId)) {
            throw status(Status.PERMISSION_DENIED, "La reserva no pertenece al cliente");
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA
                || !reserva.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw status(Status.FAILED_PRECONDITION, "La reserva ya está cancelada o su período ya comenzó");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public List<Reserva> consultarReservas(Long clienteId, List<Long> vehiculoIds, EstadoReserva estado,
                                           LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        Specification<Reserva> filtros = (root, query, builder) -> builder.conjunction();
        if (clienteId != null) {
            filtros = filtros.and((root, query, builder) -> builder.equal(root.get("clienteId"), clienteId));
        }
        if (!vehiculoIds.isEmpty()) {
            filtros = filtros.and((root, query, builder) -> root.get("vehiculoId").in(vehiculoIds));
        }
        if (estado != null) {
            filtros = filtros.and((root, query, builder) -> builder.equal(root.get("estado"), estado));
        }
        if (fechaDesde != null) {
            filtros = filtros.and((root, query, builder) ->
                    builder.greaterThan(root.get("fechaFin"), fechaDesde));
        }
        if (fechaHasta != null) {
            filtros = filtros.and((root, query, builder) ->
                    builder.lessThan(root.get("fechaInicio"), fechaHasta));
        }
        return reservaRepository.findAll(filtros);
    }

    @Transactional(readOnly = true)
    public List<Reserva> obtenerPorCliente(long clienteId) {
        validarId(clienteId, "cliente_id");
        return reservaRepository.findByClienteId(clienteId);
    }

    @Transactional(readOnly = true)
    public List<Long> consultarVehiculosOcupados(String fechaInicio, String fechaFin) {
        LocalDateTime inicio = parseFecha(fechaInicio, "fecha_inicio");
        LocalDateTime fin = parseFecha(fechaFin, "fecha_fin");
        validarPeriodo(inicio, fin);
        return reservaRepository.findAll((root, query, builder) -> builder.and(
                        builder.equal(root.get("estado"), EstadoReserva.CONFIRMADA),
                        builder.lessThan(root.get("fechaInicio"), fin),
                        builder.greaterThan(root.get("fechaFin"), inicio)))
                .stream()
                .map(Reserva::getVehiculoId)
                .distinct()
                .sorted()
                .toList();
    }

    public static long cantidadDias(LocalDateTime inicio, LocalDateTime fin) {
        Duration duracion = Duration.between(inicio, fin);
        long dias = duracion.toDays();
        return duracion.minusDays(dias).isZero() ? dias : dias + 1;
    }

    public static LocalDateTime parseFecha(String valor, String campo) {
        try {
            return LocalDateTime.parse(valor);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw status(Status.INVALID_ARGUMENT, campo + " debe tener formato ISO-8601 (YYYY-MM-DDTHH:mm:ss)");
        }
    }

    private static void validarPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw status(Status.INVALID_ARGUMENT, "fecha_fin debe ser posterior a fecha_inicio");
        }
    }

    private static void validarPrecio(double precioDiario) {
        if (!Double.isFinite(precioDiario) || precioDiario < 0) {
            throw status(Status.INVALID_ARGUMENT, "precio_diario debe ser un número finito no negativo");
        }
    }

    private static void validarId(long id, String campo) {
        if (id <= 0) {
            throw status(Status.INVALID_ARGUMENT, campo + " debe ser mayor que cero");
        }
    }

    private static StatusRuntimeException status(Status status, String description) {
        return status.withDescription(description).asRuntimeException();
    }
}
