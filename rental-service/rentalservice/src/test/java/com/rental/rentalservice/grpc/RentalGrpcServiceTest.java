package com.rental.rentalservice.grpc;

import com.rentar.proto.alquiler.EstadoHistorial;
import com.rentar.proto.alquiler.HistorialAlquiler;
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
        // Un solo instante: con dos llamadas a now() el período mide "2 días + microsegundos" y el ceil da 3.
        LocalDateTime ahora = LocalDateTime.now();
        reserva.setFechaInicio(ahora.minusDays(3));
        reserva.setFechaFin(ahora.minusDays(1));
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
        assertEquals(EstadoHistorial.ESTADO_HISTORIAL_FINALIZADA, response.getValue().getAlquileres(0).getEstado());
        assertEquals(2, response.getValue().getAlquileres(0).getCantidadDias());
    }

    @Test
    void propagaCodigosGrpcDeLaLogicaDeNegocio() {
        when(reservaService.cancelarReserva(5, 10))
                .thenThrow(Status.PERMISSION_DENIED.withDescription("No pertenece al cliente").asRuntimeException());

        @SuppressWarnings("unchecked")
        StreamObserver<com.rentar.proto.alquiler.RespuestaCancelarReserva> observer = mock(StreamObserver.class);
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

    private static Reserva reserva(long id, EstadoReserva estado, LocalDateTime inicio, LocalDateTime fin) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setClienteId(10L);
        reserva.setVehiculoId(20L);
        reserva.setFechaInicio(inicio);
        reserva.setFechaFin(fin);
        reserva.setPrecioDiario(BigDecimal.valueOf(100));
        reserva.setImporteTotal(BigDecimal.valueOf(100));
        reserva.setEstado(estado);
        return reserva;
    }

    @Test
    void historialSoloIncluyeCanceladasYFinalizadas() {
        LocalDateTime ahora = LocalDateTime.now();
        when(reservaService.obtenerPorCliente(10)).thenReturn(List.of(
                reserva(1L, EstadoReserva.CONFIRMADA, ahora.minusDays(3), ahora.minusDays(2)),
                reserva(2L, EstadoReserva.CANCELADA, ahora.plusDays(5), ahora.plusDays(6)),
                reserva(3L, EstadoReserva.CONFIRMADA, ahora.plusDays(5), ahora.plusDays(6)),
                reserva(4L, EstadoReserva.CONFIRMADA, ahora.minusDays(1), ahora.plusDays(1))));

        @SuppressWarnings("unchecked")
        StreamObserver<RespuestaConsultarHistorial> observer = mock(StreamObserver.class);
        grpcService.consultarHistorial(
                SolicitudConsultarHistorial.newBuilder().setClienteId(10).build(), observer);

        ArgumentCaptor<RespuestaConsultarHistorial> response =
                ArgumentCaptor.forClass(RespuestaConsultarHistorial.class);
        verify(observer).onNext(response.capture());
        // La 3 (futura) y la 4 (en curso) no son historial todavía.
        assertEquals(List.of(1L, 2L), response.getValue().getAlquileresList().stream()
                .map(HistorialAlquiler::getReservaId).toList());
        assertEquals(EstadoHistorial.ESTADO_HISTORIAL_FINALIZADA, response.getValue().getAlquileres(0).getEstado());
        assertEquals(EstadoHistorial.ESTADO_HISTORIAL_CANCELADA, response.getValue().getAlquileres(1).getEstado());
    }

    @Test
    void lasFechasSiempreIncluyenSegundos() {
        when(reservaService.obtenerPorCliente(10)).thenReturn(List.of(reserva(1L, EstadoReserva.CANCELADA,
                LocalDateTime.of(2027, 1, 10, 10, 0), LocalDateTime.of(2027, 1, 12, 10, 0))));

        @SuppressWarnings("unchecked")
        StreamObserver<RespuestaConsultarHistorial> observer = mock(StreamObserver.class);
        grpcService.consultarHistorial(
                SolicitudConsultarHistorial.newBuilder().setClienteId(10).build(), observer);

        ArgumentCaptor<RespuestaConsultarHistorial> response =
                ArgumentCaptor.forClass(RespuestaConsultarHistorial.class);
        verify(observer).onNext(response.capture());
        assertEquals("2027-01-10T10:00:00", response.getValue().getAlquileres(0).getFechaInicio());
        assertEquals("2027-01-12T10:00:00", response.getValue().getAlquileres(0).getFechaFin());
    }
}
