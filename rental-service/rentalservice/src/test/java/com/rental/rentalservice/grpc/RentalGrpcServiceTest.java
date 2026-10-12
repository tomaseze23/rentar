package com.rental.rentalservice.grpc;

import com.rentar.proto.alquiler.EstadoHistorial;
import com.rentar.proto.alquiler.RespuestaConsultarHistorial;
import com.rentar.proto.alquiler.SolicitudConsultarHistorial;
import com.rental.rentalservice.entities.EstadoReserva;
import com.rental.rentalservice.entities.Reserva;
import com.rental.rentalservice.services.ReservaService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalGrpcServiceTest {

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private RentalGrpcService grpcService;

    @Test
    void historialMarcaFinalizadaUnaReservaConfirmadaQueYaTermino() {
        Reserva reserva = new Reserva();
        reserva.setId(5L);
        reserva.setClienteId(10L);
        reserva.setVehiculoId(20L);
        reserva.setFechaInicio(LocalDateTime.now().minusDays(3));
        reserva.setFechaFin(LocalDateTime.now().minusDays(1));
        reserva.setPrecioDiario(BigDecimal.valueOf(80));
        reserva.setImporteTotal(BigDecimal.valueOf(160));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        when(reservaService.obtenerPorCliente(10)).thenReturn(List.of(reserva));

        @SuppressWarnings("unchecked")
        StreamObserver<RespuestaConsultarHistorial> observer = mock(StreamObserver.class);
        grpcService.consultarHistorial(
                SolicitudConsultarHistorial.newBuilder().setClienteId(10).build(), observer);

        ArgumentCaptor<RespuestaConsultarHistorial> response =
                ArgumentCaptor.forClass(RespuestaConsultarHistorial.class);
        verify(observer).onNext(response.capture());
        verify(observer).onCompleted();
        assertEquals(EstadoHistorial.FINALIZADA, response.getValue().getAlquileres(0).getEstado());
        assertEquals(2, response.getValue().getAlquileres(0).getCantidadDias());
    }

    @Test
    void propagaCodigosGrpcDeLaLogicaDeNegocio() {
        when(reservaService.cancelarReserva(5, 10))
                .thenThrow(Status.PERMISSION_DENIED.withDescription("No pertenece al cliente").asRuntimeException());

        @SuppressWarnings("unchecked")
        StreamObserver<com.rentar.proto.alquiler.Reserva> observer = mock(StreamObserver.class);
        grpcService.cancelarReserva(
                com.rentar.proto.alquiler.SolicitudCancelarReserva.newBuilder()
                        .setReservaId(5)
                        .setClienteId(10)
                        .build(),
                observer);

        ArgumentCaptor<StatusRuntimeException> error = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(observer).onError(error.capture());
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(error.getValue()).getCode());
        verify(observer, never()).onNext(any());
    }
}
