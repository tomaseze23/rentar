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
    public void crearVehiculo(SolicitudCrearVehiculo request, StreamObserver<RespuestaCrearVehiculo> responseObserver) {
        try {
            var vehiculo = domainService.crear(VehiculoGrpcMapper.fromCrearRequest(request));
            responseObserver.onNext(RespuestaCrearVehiculo.newBuilder().setVehiculo(VehiculoGrpcMapper.toProto(vehiculo)).build());
            responseObserver.onCompleted();
        } catch (PatenteDuplicadaException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void actualizarVehiculo(SolicitudActualizarVehiculo request, StreamObserver<RespuestaActualizarVehiculo> responseObserver) {
        try {
            var vehiculo = domainService.modificar(request.getId(), VehiculoGrpcMapper.fromActualizarRequest(request));
            responseObserver.onNext(RespuestaActualizarVehiculo.newBuilder().setVehiculo(VehiculoGrpcMapper.toProto(vehiculo)).build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (VehiculoInvalidoException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void darDeBajaVehiculo(SolicitudDarDeBajaVehiculo request, StreamObserver<RespuestaDarDeBajaVehiculo> responseObserver) {
        try {
            domainService.bajaLogica(request.getId());
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(RespuestaDarDeBajaVehiculo.newBuilder().setVehiculo(VehiculoGrpcMapper.toProto(vehiculo)).build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void reactivarVehiculo(SolicitudReactivarVehiculo request, StreamObserver<RespuestaReactivarVehiculo> responseObserver) {
        try {
            var vehiculo = domainService.reactivar(request.getId());
            responseObserver.onNext(RespuestaReactivarVehiculo.newBuilder().setVehiculo(VehiculoGrpcMapper.toProto(vehiculo)).build());
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
    public void consultarVehiculo(SolicitudConsultarVehiculo request, StreamObserver<RespuestaConsultarVehiculo> responseObserver) {
        try {
            var vehiculo = domainService.consultar(request.getId());
            responseObserver.onNext(RespuestaConsultarVehiculo.newBuilder().setVehiculo(VehiculoGrpcMapper.toProto(vehiculo)).build());
            responseObserver.onCompleted();
        } catch (VehiculoNoEncontradoException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void consultarDisponibilidad(SolicitudConsultarDisponibilidad request,
                                        StreamObserver<RespuestaConsultarDisponibilidad> responseObserver) {
        try {
            var tipo = request.hasTipoVehiculo() ? VehiculoGrpcMapper.mapTipo(request.getTipoVehiculo()) : null;
            var vehiculos = domainService.consultarDisponibilidadSimple(
                    tipo != null ? tipo.name() : null,
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
                                         StreamObserver<RespuestaActualizarEstadoVehiculo> responseObserver) {
        try {
            com.rentar.vehicleservice.entities.EstadoVehiculo nuevoEstado =
                    VehiculoGrpcMapper.mapEstado(request.getEstado());
            domainService.actualizarEstado(request.getVehiculoId(), nuevoEstado);
            var vehiculo = domainService.consultar(request.getVehiculoId());
            responseObserver.onNext(RespuestaActualizarEstadoVehiculo.newBuilder().setVehiculo(VehiculoGrpcMapper.toProto(vehiculo)).build());
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