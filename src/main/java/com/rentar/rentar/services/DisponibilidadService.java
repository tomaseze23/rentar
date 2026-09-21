package com.rentar.rentar.services;

import com.rentar.rentar.dtos.DisponibilidadFiltro;
import com.rentar.rentar.dtos.VehiculoDisponibleResponse;
import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.Reserva;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.repositories.VehiculoRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DisponibilidadService {

    private final VehiculoRepository vehiculos;

    public DisponibilidadService(VehiculoRepository vehiculos) {
        this.vehiculos = vehiculos;
    }

    @Transactional(readOnly = true)
    public List<VehiculoDisponibleResponse> buscar(DisponibilidadFiltro filtro) {
        if (filtro == null) {
            throw new IllegalArgumentException("Debés indicar la fecha y hora de inicio y de finalización");
        }
        LocalDateTime inicio = parseFecha(filtro.getFechaInicio(), "fechaInicio");
        LocalDateTime fin = parseFecha(filtro.getFechaFin(), "fechaFin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("fechaFin debe ser posterior a fechaInicio");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("fechaInicio debe ser futura");
        }
        Double precioMin = filtro.getPrecioMin();
        Double precioMax = filtro.getPrecioMax();
        if (precioMin != null && precioMax != null && precioMin > precioMax) {
            throw new IllegalArgumentException("precioMin no puede ser mayor que precioMax");
        }

        Specification<Vehiculo> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.isTrue(root.get("activo")));
            if (filtro.getTipoVehiculo() != null) {
                conditions.add(cb.equal(root.get("tipoVehiculo"), filtro.getTipoVehiculo()));
            }
            if (hasText(filtro.getMarca())) {
                conditions.add(cb.like(cb.lower(root.get("marca")), contiene(filtro.getMarca())));
            }
            if (hasText(filtro.getModelo())) {
                conditions.add(cb.like(cb.lower(root.get("modelo")), contiene(filtro.getModelo())));
            }
            if (precioMin != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.<Double>get("precioDiario"), precioMin));
            }
            if (precioMax != null) {
                conditions.add(cb.lessThanOrEqualTo(root.<Double>get("precioDiario"), precioMax));
            }

            // Disponible durante TODO el período: sin reservas confirmadas que se superpongan.
            Subquery<Long> ocupado = query.subquery(Long.class);
            Root<Reserva> reserva = ocupado.from(Reserva.class);
            ocupado.select(reserva.<Long>get("id")).where(
                    cb.equal(reserva.get("vehiculo").get("id"), root.get("id")),
                    cb.equal(reserva.get("estado"), EstadoReserva.CONFIRMADA),
                    cb.lessThan(reserva.<LocalDateTime>get("fechaInicio"), fin),
                    cb.greaterThan(reserva.<LocalDateTime>get("fechaFin"), inicio)
            );
            conditions.add(cb.not(cb.exists(ocupado)));

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        return vehiculos.findAll(spec, Sort.by("marca", "modelo")).stream()
                .map(v -> new VehiculoDisponibleResponse(
                        v.getId(), v.getPatente(), v.getMarca(), v.getModelo(), v.getAnio(),
                        v.getColor(), v.getTipoVehiculo(), v.getPrecioDiario()))
                .toList();
    }

    private LocalDateTime parseFecha(String fecha, String campo) {
        if (!hasText(fecha)) {
            throw new IllegalArgumentException(campo + " es obligatoria");
        }
        try {
            return LocalDateTime.parse(fecha.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(campo + " debe tener formato ISO-8601, ej. 2026-10-01T10:00");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String contiene(String texto) {
        return "%" + texto.trim().toLowerCase() + "%";
    }
}
