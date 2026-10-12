package com.rentar.customer.grpc;

import com.rentar.customer.dtos.ClienteRequest;
import com.rentar.customer.dtos.ClienteResponse;
import com.rentar.customer.dtos.ClienteUpdateRequest;
import com.rentar.customer.dtos.UsuarioAutenticado;
import com.rentar.customer.dtos.VerificacionCliente;
import com.rentar.customer.exceptions.InvalidArgumentException;
import com.rentar.proto.cliente.Cliente;
import com.rentar.proto.cliente.RespuestaValidarCredencialesLogin;
import com.rentar.proto.cliente.RespuestaVerificarClienteActivo;
import com.rentar.proto.cliente.SolicitudActualizarCliente;
import com.rentar.proto.cliente.SolicitudCrearCliente;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Conversión entre los mensajes del contrato (proto/cliente.proto) y los DTOs del dominio.
 * Los campos optional del proto ausentes equivalen a null en el dominio.
 */
final class ClienteMapper {

    private ClienteMapper() {
    }

    static Cliente toProto(ClienteResponse dto) {
        Cliente.Builder cliente = Cliente.newBuilder()
                .setId(dto.id())
                .setUsuarioId(dto.usuarioId())
                .setEmail(dto.email())
                .setDocumento(dto.documento())
                .setNombre(dto.nombre())
                .setApellido(dto.apellido())
                .setActivo(dto.activo());
        if (dto.telefono() != null) {
            cliente.setTelefono(dto.telefono());
        }
        if (dto.fechaNacimiento() != null) {
            cliente.setFechaNacimiento(dto.fechaNacimiento().toString());
        }
        return cliente.build();
    }

    static ClienteRequest toDomain(SolicitudCrearCliente solicitud) {
        return new ClienteRequest(
                solicitud.getEmail(),
                solicitud.getPassword(),
                solicitud.getDocumento(),
                solicitud.getNombre(),
                solicitud.getApellido(),
                solicitud.hasTelefono() ? solicitud.getTelefono() : null,
                solicitud.hasFechaNacimiento() ? parseFecha(solicitud.getFechaNacimiento()) : null);
    }

    static ClienteUpdateRequest toDomain(SolicitudActualizarCliente solicitud) {
        return new ClienteUpdateRequest(
                solicitud.getNombre(),
                solicitud.getApellido(),
                solicitud.hasTelefono() ? solicitud.getTelefono() : null,
                solicitud.hasFechaNacimiento() ? parseFecha(solicitud.getFechaNacimiento()) : null);
    }

    static RespuestaVerificarClienteActivo toProto(VerificacionCliente verificacion) {
        return RespuestaVerificarClienteActivo.newBuilder()
                .setExiste(verificacion.existe())
                .setActivo(verificacion.activo())
                .build();
    }

    static RespuestaValidarCredencialesLogin toProto(UsuarioAutenticado usuario) {
        RespuestaValidarCredencialesLogin.Builder respuesta = RespuestaValidarCredencialesLogin.newBuilder()
                .setCredencialesValidas(true)
                .setUsuarioId(usuario.usuarioId())
                .setEmail(usuario.email())
                .setRol(usuario.rol());
        if (usuario.clienteId() != null) {
            respuesta.setClienteId(usuario.clienteId());
        }
        return respuesta.build();
    }

    private static LocalDate parseFecha(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new InvalidArgumentException("Fecha inválida '" + valor + "', se espera el formato yyyy-MM-dd");
        }
    }
}
