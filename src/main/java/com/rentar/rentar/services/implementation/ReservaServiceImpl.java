package com.rentar.rentar.services.implementation;

import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.repositories.ClienteRepository;
import com.rentar.rentar.repositories.ReservaRepository;
import com.rentar.rentar.repositories.VehiculoRepository;
import com.rentar.rentar.dtos.ReservaRequest;
import com.rentar.rentar.dtos.ReservaResponse;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.Reserva;
import com.rentar.rentar.entities.Vehiculo;

import com.rentar.rentar.services.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    @Override
    @Transactional
    public ReservaResponse crearReserva(ReservaRequest request) {
        // 1. Validaciones de fechas
        if (!request.getFechaFin().isAfter(request.getFechaInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de fin debe ser posterior a la fecha de inicio"
            );
        }

        if (request.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de inicio debe ser futura"
            );
        }

        // 2. Verificar existencia y estado del Cliente
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con ID: " + request.getClienteId()
                ));

        if (!Boolean.TRUE.equals(cliente.isActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El cliente no se encuentra activo para realizar reservas"
            );
        }

        // 3. Verificar existencia y estado del Vehículo
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vehículo no encontrado con ID: " + request.getVehiculoId()
                ));

        if (!Boolean.TRUE.equals(vehiculo.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El vehículo no se encuentra activo"
            );
        }

        // 4. Verificar disponibilidad temporal (evitar solapamiento con reservas confirmadas)
        boolean solapado = reservaRepository.existsSolapamiento(
                vehiculo.getId(),
                request.getFechaInicio(),
                request.getFechaFin(),
                EstadoReserva.CONFIRMADA
        );

        if (solapado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El vehículo ya cuenta con una reserva confirmada en el período indicado"
            );
        }

        // 5. Cálculo del total según duración y precio diario
        long horas = Duration.between(request.getFechaInicio(), request.getFechaFin()).toHours();
        long dias = (long) Math.ceil((double) horas / 24.0);
        if (dias == 0) {
            dias = 1;
        }

        BigDecimal importeTotal = BigDecimal.valueOf(vehiculo.getPrecioDiario())
                .multiply(BigDecimal.valueOf(dias));
        // 6. Persistencia de la reserva con estado CONFIRMADA
        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaInicio(request.getFechaInicio());
        reserva.setFechaFin(request.getFechaFin());
        reserva.setPrecioDiario(BigDecimal.valueOf(vehiculo.getPrecioDiario()));
        reserva.setImporteTotal(importeTotal);
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        Reserva reservaGuardada = reservaRepository.save(reserva);

        // 7. Retornar DTO de respuesta
        return new ReservaResponse(
                reservaGuardada.getId(),
                cliente.getId(),
                vehiculo.getId(),
                reservaGuardada.getFechaInicio(),
                reservaGuardada.getFechaFin(),
                reservaGuardada.getPrecioDiario(),
                reservaGuardada.getImporteTotal(),
                reservaGuardada.getEstado().name()
        );
    }

    @Override
    @Transactional
    public ReservaResponse cancelarReserva(Long id) {
        // 1. La reserva debe existir
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con ID: " + id));

        // 2. No cancelar algo ya cancelado
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La reserva ya se encuentra cancelada");
        }

        // 3. Solo si el período todavía no comenzó
        if (!reserva.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No se puede cancelar una reserva cuyo período ya comenzó");
        }

        // 4. Baja lógica: cambia el estado, no se borra
        reserva.setEstado(EstadoReserva.CANCELADA);
        Reserva actualizada = reservaRepository.save(reserva);

        // 5. DTO de respuesta
        return new ReservaResponse(
                actualizada.getId(),
                actualizada.getCliente().getId(),
                actualizada.getVehiculo().getId(),
                actualizada.getFechaInicio(),
                actualizada.getFechaFin(),
                actualizada.getPrecioDiario(),
                actualizada.getImporteTotal(),
                actualizada.getEstado().name()
        );
    }

}
