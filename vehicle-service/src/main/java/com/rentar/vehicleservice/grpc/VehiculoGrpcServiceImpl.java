package com.rentar.vehicleservice.grpc;

import com.rentar.proto.vehiculo.*;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.exceptions.VehiculoNoEncontradoException;
import com.rentar.vehicleservice.exceptions.VehiculoYaActivoException;
import com.rentar.vehicleservice.services.VehiculoDomainService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class VehiculoGrpcServiceImpl extends ServicioVehiculosGrpc.ServicioVehiculosImplBase {

    private final VehiculoDomainService domainService;

    public VehiculoGrpcServiceImpl(VehiculoDomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public void crearVehiculo(SolicitudCrearVehiculo request, StreamObserver<com.rentar.proto.vehiculo.Vehiculo> responseObserver) {
        try {
            var vehiculo = domainService.crear(VehiculoGrpcMapper.fromCrearRequest(request));
            responseObserver.onNext(VehiculoGrpcMapper.toProto(vehiculo));
            responseObserver.onCompleted();
        } catch (PatenteDuplicadaException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void actualizarVehiculo(SolicitudActualizarVehiculo request, StreamObserver<com.rentar.proto.vehiculo.Vehiculo> responseObserver) {
        try {
            var vehiculo = domainService.modificar(request.getId(), VehiculoGrpcMapper.fromActualizarRequest(request));
            responseObserver.onNext(VehiculoGrpcMapper.toProto(vehiculo));
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void darDeBajaVehiculo(SolicitudIdVehiculo request, StreamObserver<com.rentar.proto.vehiculo.Vehiculo> responseObserver) {
        try {
            domainService.bajaLogica(request.getId());
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(VehiculoGrpcMapper.toProto(vehiculo));
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void reactivarVehiculo(SolicitudIdVehiculo request, StreamObserver<com.rentar.proto.vehiculo.Vehiculo> responseObserver) {
        try {
            var vehiculo = domainService.reactivar(request.getId());
            responseObserver.onNext(VehiculoGrpcMapper.toProto(vehiculo));
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoYaActivoException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void consultarVehiculos(SolicitudConsultarVehiculos request, StreamObserver<RespuestaConsultarVehiculos> responseObserver) {
        Boolean activo = request.hasActivo() ? request.getActivo() : null;
        var tipo = request.hasTipoVehiculo() ? VehiculoGrpcMapper.mapTipo(request.getTipoVehiculo()) : null;
        var vehiculos = domainService.listar(activo, tipo, request.getIdsList());
        var builder = RespuestaConsultarVehiculos.newBuilder();
        vehiculos.forEach(v -> builder.addVehiculos(VehiculoGrpcMapper.toProto(v)));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void consultarVehiculo(SolicitudIdVehiculo request, StreamObserver<com.rentar.proto.vehiculo.Vehiculo> responseObserver) {
        try {
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(VehiculoGrpcMapper.toProto(vehiculo));
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void consultarDisponibilidad(SolicitudConsultarDisponibilidad request,
                                        StreamObserver<RespuestaConsultarDisponibilidad> responseObserver) {
        try {
            var vehiculos = domainService.consultarDisponibilidadSimple(
                    request.hasTipoVehiculo() ? request.getTipoVehiculo().name() : null,
                    request.hasMarca() ? request.getMarca() : null,
                    request.hasModelo() ? request.getModelo() : null,
                    request.hasPrecioMin() ? request.getPrecioMin() : null,
                    request.hasPrecioMax() ? request.getPrecioMax() : null,
                    request.getVehiculosOcupadosIdsList()
            );
            var builder = RespuestaConsultarDisponibilidad.newBuilder();
            vehiculos.forEach(v -> builder.addVehiculos(VehiculoGrpcMapper.toProto(v)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void actualizarEstadoVehiculo(SolicitudActualizarEstadoVehiculo request,
                                         StreamObserver<com.rentar.proto.vehiculo.Vehiculo> responseObserver) {
        try {
            com.rentar.vehicleservice.entities.EstadoVehiculo nuevoEstado =
                    com.rentar.vehicleservice.entities.EstadoVehiculo.valueOf(request.getEstado().name());
            domainService.actualizarEstado(request.getVehiculoId(), nuevoEstado);
            var vehiculo = domainService.consultar(request.getVehiculoId());
            responseObserver.onNext(VehiculoGrpcMapper.toProto(vehiculo));
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Estado inválido: " + request.getEstado())
                    .asRuntimeException());
        }
    }


}