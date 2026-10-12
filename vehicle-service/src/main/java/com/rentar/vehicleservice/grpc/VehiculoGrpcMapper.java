package com.rentar.vehicleservice.grpc;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;

public class VehiculoGrpcMapper {

    static TipoVehiculo mapTipo(com.rentar.proto.comun.TipoVehiculo t) {
        if (t == null
                || t == com.rentar.proto.comun.TipoVehiculo.TIPO_VEHICULO_NO_ESPECIFICADO
                || t == com.rentar.proto.comun.TipoVehiculo.UNRECOGNIZED) {
            return null;
        }
        return TipoVehiculo.valueOf(t.name());
    }

    public static com.rentar.proto.vehiculo.Vehiculo toProto(Vehiculo v) {
        var builder = com.rentar.proto.vehiculo.Vehiculo.newBuilder()
                .setId(v.getId())
                .setPatente(v.getPatente())
                .setMarca(v.getMarca())
                .setModelo(v.getModelo())
                .setAnio(v.getAnio())
                .setPrecioDiario(v.getPrecioDiario())
                .setActivo(v.getActivo());

        if (v.getColor() != null) {
            builder.setColor(v.getColor());
        }
        if (v.getTipoVehiculo() != null) {
            builder.setTipoVehiculo(com.rentar.proto.comun.TipoVehiculo.valueOf(v.getTipoVehiculo().name()));
        }
        if (v.getEstado() != null) {
            builder.setEstado(com.rentar.proto.comun.EstadoVehiculo.valueOf(v.getEstado().name()));
        }
        return builder.build();
    }

    public static Vehiculo fromCrearRequest(com.rentar.proto.vehiculo.SolicitudCrearVehiculo r) {
        Vehiculo v = new Vehiculo();
        v.setPatente(r.getPatente());
        v.setMarca(r.getMarca());
        v.setModelo(r.getModelo());
        v.setAnio(r.getAnio());
        v.setColor(r.hasColor() ? r.getColor() : null);
        v.setTipoVehiculo(mapTipo(r.getTipoVehiculo()));
        v.setPrecioDiario(r.getPrecioDiario());
        return v;
    }

    public static Vehiculo fromActualizarRequest(com.rentar.proto.vehiculo.SolicitudActualizarVehiculo r) {
        Vehiculo v = new Vehiculo();
        v.setMarca(r.getMarca());
        v.setModelo(r.getModelo());
        v.setAnio(r.getAnio());
        v.setColor(r.hasColor() ? r.getColor() : null);
        v.setTipoVehiculo(mapTipo(r.getTipoVehiculo()));
        v.setPrecioDiario(r.getPrecioDiario());
        return v;
    }
}