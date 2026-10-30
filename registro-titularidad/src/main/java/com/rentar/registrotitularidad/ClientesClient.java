package com.rentar.registrotitularidad;

import com.rentar.proto.cliente.ServicioClientesGrpc;
import com.rentar.proto.cliente.SolicitudConsultarCliente;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Consulta al Customer Service el documento de un cliente. */
@Component
public class ClientesClient {

    @GrpcClient("clientes")
    private ServicioClientesGrpc.ServicioClientesBlockingStub stub;

    /**
     * Documento del cliente, o vacío si el cliente no existe.
     * Cualquier otro error (servicio caído, timeout) se propaga para que RabbitMQ reintente el mensaje.
     */
    public Optional<String> documentoDe(long clienteId) {
        try {
            var cliente = stub.consultarCliente(SolicitudConsultarCliente.newBuilder().setId(clienteId).build()).getCliente();
            return Optional.of(cliente.getDocumento());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
