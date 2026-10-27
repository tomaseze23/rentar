package com.rentar.vehicleservice.grpc;

import com.rentar.proto.vehiculo.ServicioVehiculosGrpc;
import com.rentar.proto.vehiculo.SolicitudConsultarVehiculos;
import com.rentar.proto.vehiculo.SolicitudConsultarVehiculo;
import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.repositories.VehiculoRepository;
import com.rentar.vehicleservice.services.VehiculoDomainService;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoGrpcServiceImplTest {

    // Server y channel in-process de cada test; se cierran en @AfterEach
    // (GrpcCleanupRule es una @Rule de JUnit 4 y no se ejecuta en JUnit 5).
    private Server server;
    private ManagedChannel channel;

    @Mock
    private VehiculoRepository repository;

    @AfterEach
    void cerrar() {
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    private static Vehiculo vehiculo(long id, String patente, TipoVehiculo tipo) {
        Vehiculo v = new Vehiculo();
        v.setId(id);
        v.setPatente(patente);
        v.setMarca("Marca");
        v.setModelo("Modelo");
        v.setAnio("2022");
        v.setTipoVehiculo(tipo);
        v.setPrecioDiario(10000.0);
        v.setEstado(EstadoVehiculo.DISPONIBLE);
        v.setActivo(true);
        return v;
    }

    private ServicioVehiculosGrpc.ServicioVehiculosBlockingStub levantarServidorYObtenerStub(
            VehiculoDomainService domainService) throws Exception {

        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new VehiculoGrpcServiceImpl(domainService))
                .build().start();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

        return ServicioVehiculosGrpc.newBlockingStub(channel);
    }

    @Test
    void consultarVehiculo_idExistente_devuelveVehiculoPorGrpc() throws Exception {
        Vehiculo v = new Vehiculo();
        v.setId(1L);
        v.setPatente("AB123CD");
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setAnio("2022");
        v.setTipoVehiculo(TipoVehiculo.SEDAN);
        v.setPrecioDiario(45000.0);
        v.setEstado(EstadoVehiculo.DISPONIBLE);
        v.setActivo(true);
        when(repository.findById(1L)).thenReturn(Optional.of(v));

        var stub = levantarServidorYObtenerStub(new VehiculoDomainService(repository));

        var response = stub.consultarVehiculo(
                SolicitudConsultarVehiculo.newBuilder().setId(1L).build()).getVehiculo();

        assertEquals("AB123CD", response.getPatente());
        assertEquals("TIPO_VEHICULO_SEDAN", response.getTipoVehiculo().name());
    }

    @Test
    void consultarVehiculo_idInexistente_devuelveNotFound() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        var stub = levantarServidorYObtenerStub(new VehiculoDomainService(repository));

        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () ->
                stub.consultarVehiculo(SolicitudConsultarVehiculo.newBuilder().setId(999L).build()));

        assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    }

    @Test
    void consultarVehiculos_filtraPorTipoEIds() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                vehiculo(1L, "AAA111", TipoVehiculo.SEDAN),
                vehiculo(2L, "BBB222", TipoVehiculo.SUV),
                vehiculo(3L, "CCC333", TipoVehiculo.SUV)));

        var stub = levantarServidorYObtenerStub(new VehiculoDomainService(repository));

        var suvs = stub.consultarVehiculos(SolicitudConsultarVehiculos.newBuilder()
                .setTipoVehiculo(com.rentar.proto.comun.TipoVehiculo.TIPO_VEHICULO_SUV).build());
        assertEquals(List.of(2L, 3L), suvs.getVehiculosList().stream().map(v -> v.getId()).toList());

        var porIds = stub.consultarVehiculos(SolicitudConsultarVehiculos.newBuilder()
                .addIds(1L).addIds(3L).build());
        assertEquals(List.of(1L, 3L), porIds.getVehiculosList().stream().map(v -> v.getId()).toList());

        var sinFiltros = stub.consultarVehiculos(SolicitudConsultarVehiculos.getDefaultInstance());
        assertEquals(3, sinFiltros.getVehiculosCount());
    }
}
