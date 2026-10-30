package com.rentar.vehicleservice.grpc;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.SolicitudIncorporacion;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;

public class VehiculoGrpcMapper {

    // Los valores del .proto llevan el nombre del enum como prefijo (TIPO_VEHICULO_SEDAN);
    // los enums de dominio no (SEDAN). Se traducen quitando o agregando el prefijo.
    private static final String PREFIJO_TIPO = "TIPO_VEHICULO_";
    private static final String PREFIJO_ESTADO = "ESTADO_VEHICULO_";
    private static final String PREFIJO_VERIFICACION = "ESTADO_VERIFICACION_";
    private static final String PREFIJO_SOLICITUD = "ESTADO_SOLICITUD_INCORPORACION_";
    private static final String PREFIJO_VALIDACION = "RESULTADO_VALIDACION_";

    static TipoVehiculo mapTipo(com.rentar.proto.comun.TipoVehiculo t) {
        if (t == null
                || t == com.rentar.proto.comun.TipoVehiculo.TIPO_VEHICULO_NO_ESPECIFICADO
                || t == com.rentar.proto.comun.TipoVehiculo.UNRECOGNIZED) {
            return null;
        }
        return TipoVehiculo.valueOf(t.name().substring(PREFIJO_TIPO.length()));
    }

    /** Estado del .proto a estado de dominio. Lanza IllegalArgumentException si no es un estado válido. */
    static EstadoVehiculo mapEstado(com.rentar.proto.comun.EstadoVehiculo e) {
        if (e == null
                || e == com.rentar.proto.comun.EstadoVehiculo.ESTADO_VEHICULO_NO_ESPECIFICADO
                || e == com.rentar.proto.comun.EstadoVehiculo.UNRECOGNIZED) {
            throw new IllegalArgumentException("Estado inválido: " + e);
        }
        return EstadoVehiculo.valueOf(e.name().substring(PREFIJO_ESTADO.length()));
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
            builder.setTipoVehiculo(com.rentar.proto.comun.TipoVehiculo.valueOf(PREFIJO_TIPO + v.getTipoVehiculo().name()));
        }
        if (v.getEstado() != null) {
            builder.setEstado(com.rentar.proto.comun.EstadoVehiculo.valueOf(PREFIJO_ESTADO + v.getEstado().name()));
        }
        if (v.getVerificacion() != null) {
            builder.setVerificacion(com.rentar.proto.vehiculo.EstadoVerificacion.valueOf(PREFIJO_VERIFICACION + v.getVerificacion().name()));
        }
        if (v.getPropietarioClienteId() != null) {
            builder.setPropietarioClienteId(v.getPropietarioClienteId());
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

    public static com.rentar.proto.vehiculo.IncorporacionExterna toProto(SolicitudIncorporacion s) {
        var builder = com.rentar.proto.vehiculo.IncorporacionExterna.newBuilder()
                .setId(s.getId())
                .setClienteId(s.getClienteId())
                .setPatente(s.getPatente())
                .setMarca(s.getMarca())
                .setModelo(s.getModelo())
                .setAnio(s.getAnio())
                .setTipoVehiculo(com.rentar.proto.comun.TipoVehiculo.valueOf(PREFIJO_TIPO + s.getTipoVehiculo().name()))
                .setPrecioDiario(s.getPrecioDiario())
                .setEstado(com.rentar.proto.vehiculo.EstadoSolicitudIncorporacion.valueOf(PREFIJO_SOLICITUD + s.getEstado().name()))
                .setValidacionPatente(com.rentar.proto.vehiculo.ResultadoValidacion.valueOf(PREFIJO_VALIDACION + s.getValidacionPatente().name()))
                .setValidacionTitularidad(com.rentar.proto.vehiculo.ResultadoValidacion.valueOf(PREFIJO_VALIDACION + s.getValidacionTitularidad().name()));
        if (s.getColor() != null) {
            builder.setColor(s.getColor());
        }
        if (s.getVehiculoId() != null) {
            builder.setVehiculoId(s.getVehiculoId());
        }
        return builder.build();
    }

    public static SolicitudIncorporacion fromIncorporarRequest(com.rentar.proto.vehiculo.SolicitudIncorporarVehiculoExterno r) {
        SolicitudIncorporacion s = new SolicitudIncorporacion();
        s.setClienteId(r.getClienteId());
        s.setPatente(r.getPatente());
        s.setMarca(r.getMarca());
        s.setModelo(r.getModelo());
        s.setAnio(r.getAnio());
        s.setColor(r.hasColor() ? r.getColor() : null);
        s.setTipoVehiculo(mapTipo(r.getTipoVehiculo()));
        s.setPrecioDiario(r.getPrecioDiario());
        return s;
    }
}
