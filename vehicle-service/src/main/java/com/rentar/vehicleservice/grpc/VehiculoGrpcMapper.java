package com.rentar.vehicleservice.grpc;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;

public class VehiculoGrpcMapper {

    public static com.rentar.vehicleservice.grpc.Vehiculo toProto(Vehiculo v) {
        return com.rentar.vehicleservice.grpc.Vehiculo.newBuilder()
                .setId(v.getId())
                .setPatente(v.getPatente())
                .setMarca(v.getMarca())
                .setModelo(v.getModelo())
                .setAnio(v.getAnio())
                .setColor(v.getColor() != null ? v.getColor() : "")
                .setTipoVehiculo(com.rentar.vehicleservice.grpc.TipoVehiculo.valueOf(v.getTipoVehiculo().name()))
                .setPrecioDiario(v.getPrecioDiario())
                .setEstado(com.rentar.vehicleservice.grpc.EstadoVehiculo.valueOf(v.getEstado().name()))
                .setActivo(v.getActivo())
                .build();
    }

    public static Vehiculo fromCrearRequest(CrearVehiculoRequest r) {
        Vehiculo v = new Vehiculo();
        v.setPatente(r.getPatente());
        v.setMarca(r.getMarca());
        v.setModelo(r.getModelo());
        v.setAnio(r.getAnio());
        v.setColor(r.getColor());
        v.setTipoVehiculo(TipoVehiculo.valueOf(r.getTipoVehiculo().name()));
        v.setPrecioDiario(r.getPrecioDiario());
        return v;
    }

    public static Vehiculo fromModificarRequest(ModificarVehiculoRequest r) {
        Vehiculo v = new Vehiculo();
        v.setMarca(r.getMarca());
        v.setModelo(r.getModelo());
        v.setAnio(r.getAnio());
        v.setColor(r.getColor());
        v.setTipoVehiculo(TipoVehiculo.valueOf(r.getTipoVehiculo().name()));
        v.setPrecioDiario(r.getPrecioDiario());
        return v;
    }
}