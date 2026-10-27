package com.rental.rentalservice.grpc;

import com.rentar.proto.alquiler.*;
import com.rentar.proto.comun.Periodo;
import com.rental.rentalservice.entities.EstadoReserva;
import com.rental.rentalservice.entities.Reserva;
import com.rental.rentalservice.services.ReservaService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

@GrpcService
public class RentalGrpcService extends ServicioAlquileresGrpc.ServicioAlquileresImplBase {

    // Formato del contrato: yyyy-MM-ddTHH:mm:ss, siempre con segundos.
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ReservaService reservaService;

    public RentalGrpcService(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Override
    public void crearReserva(SolicitudCrearReserva request, StreamObserver<RespuestaCrearReserva> observer) {
        respond(observer, () -> {
            Periodo periodo = request.getPeriodo();
            Reserva reserva = reservaService.crearReserva(
                    request.getClienteId(),
                    request.getVehiculoId(),
                    periodo.getFechaInicio(),
                    periodo.getFechaFin(),
                    request.getPrecioDiario());
            return RespuestaCrearReserva.newBuilder().setReserva(mapReserva(reserva)).build();
        });
    }

    @Override
    public void consultarReservas(SolicitudConsultarReservas request,
                                  StreamObserver<RespuestaConsultarReservas> observer) {
        respond(observer, () -> {
            Long clienteId = request.hasClienteId() ? request.getClienteId() : null;
            if (clienteId != null && clienteId <= 0) {
                throw Status.INVALID_ARGUMENT.withDescription("cliente_id debe ser mayor que cero")
                        .asRuntimeException();
            }
            List<Long> vehiculoIds = request.getVehiculoIdsList();
            if (vehiculoIds.stream().anyMatch(id -> id <= 0)) {
                throw Status.INVALID_ARGUMENT.withDescription("vehiculo_ids debe contener IDs mayores que cero")
                        .asRuntimeException();
            }

            EstadoReserva estado = request.hasEstado()
                    ? mapEstado(request.getEstado())
                    : null;
            LocalDateTime desde = request.hasFechaDesde()
                    ? ReservaService.parseFecha(request.getFechaDesde(), "fecha_desde")
                    : null;
            LocalDateTime hasta = request.hasFechaHasta()
                    ? ReservaService.parseFecha(request.getFechaHasta(), "fecha_hasta")
                    : null;
            if (desde != null && hasta != null && !hasta.isAfter(desde)) {
                throw Status.INVALID_ARGUMENT.withDescription("fecha_hasta debe ser posterior a fecha_desde")
                        .asRuntimeException();
            }

            RespuestaConsultarReservas.Builder response = RespuestaConsultarReservas.newBuilder();
            reservaService.consultarReservas(clienteId, vehiculoIds, estado, desde, hasta)
                    .forEach(reserva -> response.addReservas(mapReserva(reserva)));
            return response.build();
        });
    }

    @Override
    public void cancelarReserva(SolicitudCancelarReserva request,
                                StreamObserver<RespuestaCancelarReserva> observer) {
        respond(observer, () -> RespuestaCancelarReserva.newBuilder()
                .setReserva(mapReserva(reservaService.cancelarReserva(request.getReservaId(), request.getClienteId())))
                .build());
    }

    @Override
    public void consultarHistorial(SolicitudConsultarHistorial request,
                                   StreamObserver<RespuestaConsultarHistorial> observer) {
        respond(observer, () -> {
            LocalDateTime ahora = LocalDateTime.now();
            RespuestaConsultarHistorial.Builder response = RespuestaConsultarHistorial.newBuilder();
            for (Reserva reserva : reservaService.obtenerPorCliente(request.getClienteId())) {
                EstadoHistorial estado = estadoHistorial(reserva, ahora);
                // Las reservas vigentes o futuras todavía no son parte del historial.
                if (estado == EstadoHistorial.ESTADO_HISTORIAL_NO_ESPECIFICADO) {
                    continue;
                }
                response.addAlquileres(HistorialAlquiler.newBuilder()
                        .setReservaId(reserva.getId())
                        .setVehiculoId(reserva.getVehiculoId())
                        .setFechaInicio(formatear(reserva.getFechaInicio()))
                        .setFechaFin(formatear(reserva.getFechaFin()))
                        .setCantidadDias(ReservaService.cantidadDias(reserva.getFechaInicio(), reserva.getFechaFin()))
                        .setImporteTotal(reserva.getImporteTotal().doubleValue())
                        .setEstado(estado)
                        .build());
            }
            return response.build();
        });
    }

    @Override
    public void consultarVehiculosOcupados(SolicitudConsultarVehiculosOcupados request,
                                            StreamObserver<RespuestaConsultarVehiculosOcupados> observer) {
        respond(observer, () -> {
            Periodo periodo = request.getPeriodo();
            return RespuestaConsultarVehiculosOcupados.newBuilder()
                    .addAllVehiculoIds(reservaService.consultarVehiculosOcupados(
                            periodo.getFechaInicio(), periodo.getFechaFin()))
                    .build();
        });
    }

    private static EstadoReserva mapEstado(com.rentar.proto.comun.EstadoReserva estado) {
        return switch (estado) {
            case ESTADO_RESERVA_PENDIENTE -> EstadoReserva.PENDIENTE;
            case ESTADO_RESERVA_CONFIRMADA -> EstadoReserva.CONFIRMADA;
            case ESTADO_RESERVA_CANCELADA -> EstadoReserva.CANCELADA;
            case ESTADO_RESERVA_NO_ESPECIFICADO -> null;
            case UNRECOGNIZED -> throw Status.INVALID_ARGUMENT
                    .withDescription("estado de reserva inválido").asRuntimeException();
        };
    }

    private static EstadoHistorial estadoHistorial(Reserva reserva, LocalDateTime ahora) {
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return EstadoHistorial.ESTADO_HISTORIAL_CANCELADA;
        }
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA && !reserva.getFechaFin().isAfter(ahora)) {
            return EstadoHistorial.ESTADO_HISTORIAL_FINALIZADA;
        }
        return EstadoHistorial.ESTADO_HISTORIAL_NO_ESPECIFICADO;
    }

    private static com.rentar.proto.alquiler.Reserva mapReserva(Reserva reserva) {
        return com.rentar.proto.alquiler.Reserva.newBuilder()
                .setId(reserva.getId())
                .setClienteId(reserva.getClienteId())
                .setVehiculoId(reserva.getVehiculoId())
                .setFechaInicio(formatear(reserva.getFechaInicio()))
                .setFechaFin(formatear(reserva.getFechaFin()))
                .setPrecioDiario(reserva.getPrecioDiario().doubleValue())
                .setImporteTotal(reserva.getImporteTotal().doubleValue())
                .setEstado(switch (reserva.getEstado()) {
                    case PENDIENTE -> com.rentar.proto.comun.EstadoReserva.ESTADO_RESERVA_PENDIENTE;
                    case CONFIRMADA -> com.rentar.proto.comun.EstadoReserva.ESTADO_RESERVA_CONFIRMADA;
                    case CANCELADA -> com.rentar.proto.comun.EstadoReserva.ESTADO_RESERVA_CANCELADA;
                })
                .build();
    }

    private static String formatear(LocalDateTime fecha) {
        return fecha.format(FORMATO_FECHA);
    }

    private static <T> void respond(StreamObserver<T> observer, Supplier<T> operation) {
        try {
            observer.onNext(operation.get());
            observer.onCompleted();
        } catch (StatusRuntimeException exception) {
            observer.onError(exception);
        }
    }
}
