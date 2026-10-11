package com.rental.rentalservice.grpc;

import com.rental.rentalservice.entities.EstadoReserva;
import com.rental.rentalservice.entities.Reserva;
import com.rental.rentalservice.services.ReservaService;
import com.rentar.rentar.grpc.rental.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@GrpcService
public class RentalGrpcService extends RentalServiceGrpc.RentalServiceImplBase {

    @Autowired
    private ReservaService reservaService;

    @Override
    public void crearReserva(CrearReservaRequest request, StreamObserver<ReservaResponse> responseObserver) {
        try {
            Reserva reserva = new Reserva();
            reserva.setClienteId(request.getClienteId());
            reserva.setVehiculoId(request.getVehiculoId());
            reserva.setFechaInicio(LocalDateTime.parse(request.getFechaInicio()));
            reserva.setFechaFin(LocalDateTime.parse(request.getFechaFin()));
            reserva.setPrecioDiario(BigDecimal.valueOf(request.getPrecioDiario()));
            reserva.setImporteTotal(BigDecimal.valueOf(request.getImporteTotal()));

            Reserva creada = reservaService.crearReserva(reserva);
            ReservaResponse response = mapToResponse(creada, "Reserva creada exitosamente");

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void cancelarReserva(CancelarReservaRequest request, StreamObserver<ReservaResponse> responseObserver) {
        try {
            Reserva cancelada = reservaService.cancelarReserva(request.getReservaId());
            ReservaResponse response = mapToResponse(cancelada, "Reserva cancelada correctamente");

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void obtenerReserva(ObtenerReservaRequest request, StreamObserver<ReservaResponse> responseObserver) {
        try {
            Reserva reserva = reservaService.obtenerPorId(request.getReservaId());
            ReservaResponse response = mapToResponse(reserva, "Reserva encontrada");

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listarReservasPorCliente(ListarPorClienteRequest request, StreamObserver<ListarReservasResponse> responseObserver) {
        try {
            List<Reserva> reservas = reservaService.obtenerPorCliente(request.getClienteId());
            ListarReservasResponse.Builder responseBuilder = ListarReservasResponse.newBuilder();

            for (Reserva r : reservas) {
                responseBuilder.addReservas(mapToResponse(r, ""));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void obtenerHistorial(HistorialRequest request, StreamObserver<ListarReservasResponse> responseObserver) {
        listarReservasPorCliente(ListarPorClienteRequest.newBuilder().setClienteId(request.getClienteId()).build(), responseObserver);
    }

    private ReservaResponse mapToResponse(Reserva r, String mensaje) {
        return ReservaResponse.newBuilder()
                .setId(r.getId())
                .setClienteId(r.getClienteId())
                .setVehiculoId(r.getVehiculoId())
                .setFechaInicio(r.getFechaInicio().toString())
                .setFechaFin(r.getFechaFin().toString())
                .setPrecioDiario(r.getPrecioDiario().doubleValue())
                .setImporteTotal(r.getImporteTotal().doubleValue())
                .setEstado(EstadoReservaProto.valueOf(r.getEstado().name()))
                .setMensaje(mensaje)
                .build();
    }
}