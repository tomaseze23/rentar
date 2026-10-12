package com.rentar.vehicleservice.grpc;

import com.rentar.proto.vehiculo.ServicioVehiculosGrpc;
import com.rentar.proto.vehiculo.SolicitudIdVehiculo;
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
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoGrpcServiceImplIT {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    private VehiculoRepository repository;

    private ServicioVehiculosGrpc.ServicioVehiculosBlockingStub levantarServidorYObtenerStub(
            VehiculoDomainService domainService) throws Exception {

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new VehiculoGrpcServiceImpl(domainService))
                .build().start());

        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

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
                SolicitudIdVehiculo.newBuilder().setId(1L).build());

        assertEquals("AB123CD", response.getPatente());
        assertEquals("SEDAN", response.getTipoVehiculo().name());
    }

    @Test
    void consultarVehiculo_idInexistente_devuelveNotFound() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        var stub = levantarServidorYObtenerStub(new VehiculoDomainService(repository));

        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () ->
                stub.consultarVehiculo(SolicitudIdVehiculo.newBuilder().setId(999L).build()));

        assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    }
}