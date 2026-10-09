package com.rentar.vehicleservice.grpc;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.exceptions.VehiculoNoEncontradoException;
import com.rentar.vehicleservice.services.VehiculoDomainService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class VehiculoGrpcServiceImpl extends VehiculoServiceGrpc.VehiculoServiceImplBase {

    private final VehiculoDomainService domainService;

    public VehiculoGrpcServiceImpl(VehiculoDomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public void crearVehiculo(CrearVehiculoRequest request, StreamObserver<VehiculoResponse> responseObserver) {
        try {
            var vehiculo = domainService.crear(VehiculoGrpcMapper.fromCrearRequest(request));
            responseObserver.onNext(VehiculoResponse.newBuilder()
                    .setVehiculo(VehiculoGrpcMapper.toProto(vehiculo))
                    .build());
            responseObserver.onCompleted();
        } catch (PatenteDuplicadaException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void modificarVehiculo(ModificarVehiculoRequest request, StreamObserver<VehiculoResponse> responseObserver) {
        try {
            var vehiculo = domainService.modificar(request.getId(), VehiculoGrpcMapper.fromModificarRequest(request));
            responseObserver.onNext(VehiculoResponse.newBuilder()
                    .setVehiculo(VehiculoGrpcMapper.toProto(vehiculo))
                    .build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void bajaVehiculo(BajaVehiculoRequest request, StreamObserver<BajaVehiculoResponse> responseObserver) {
        try {
            domainService.bajaLogica(request.getId());
            responseObserver.onNext(BajaVehiculoResponse.newBuilder().setExito(true).build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listarVehiculos(ListarVehiculosRequest request, StreamObserver<ListarVehiculosResponse> responseObserver) {
        Boolean activo = request.hasActivo() ? request.getActivo() : null;
        var vehiculos = domainService.listar(activo);
        var builder = ListarVehiculosResponse.newBuilder();
        vehiculos.forEach(v -> builder.addVehiculos(VehiculoGrpcMapper.toProto(v)));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void consultarVehiculo(ConsultarVehiculoRequest request, StreamObserver<VehiculoResponse> responseObserver) {
        try {
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(VehiculoResponse.newBuilder()
                    .setVehiculo(VehiculoGrpcMapper.toProto(vehiculo))
                    .build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void consultarDisponibilidad(ConsultarDisponibilidadRequest request,
                                        StreamObserver<ListarVehiculosResponse> responseObserver) {
        var vehiculos = domainService.consultarDisponibilidad(
                request.hasTipoVehiculo() ? request.getTipoVehiculo().name() : null,
                request.hasMarca() ? request.getMarca() : null,
                request.hasModelo() ? request.getModelo() : null,
                request.hasPrecioMin() ? request.getPrecioMin() : null,
                request.hasPrecioMax() ? request.getPrecioMax() : null,
                request.getIdsOcupadosList()
        );
        var builder = ListarVehiculosResponse.newBuilder();
        vehiculos.forEach(v -> builder.addVehiculos(VehiculoGrpcMapper.toProto(v)));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void validarVehiculo(ValidarVehiculoRequest request, StreamObserver<ValidarVehiculoResponse> responseObserver) {
        try {
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(ValidarVehiculoResponse.newBuilder()
                    .setExiste(true)
                    .setActivo(vehiculo.getActivo())
                    .setVehiculo(VehiculoGrpcMapper.toProto(vehiculo))
                    .build());
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onNext(ValidarVehiculoResponse.newBuilder()
                    .setExiste(false)
                    .setActivo(false)
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void actualizarEstado(ActualizarEstadoRequest request, StreamObserver<VehiculoResponse> responseObserver) {
        try {
            EstadoVehiculo nuevoEstado = EstadoVehiculo.valueOf(request.getNuevoEstado().name());
            domainService.actualizarEstado(request.getId(), nuevoEstado);
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(VehiculoResponse.newBuilder()
                    .setVehiculo(VehiculoGrpcMapper.toProto(vehiculo))
                    .build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Estado inválido: " + request.getNuevoEstado())
                    .asRuntimeException());
        }
    }
}