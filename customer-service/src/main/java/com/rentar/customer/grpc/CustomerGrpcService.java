package com.rentar.customer.grpc;

import com.rentar.customer.dtos.ClienteResponse;
import com.rentar.customer.exceptions.AuthenticationException;
import com.rentar.customer.services.AuthService;
import com.rentar.customer.services.ClienteService;
import com.rentar.proto.cliente.Cliente;
import com.rentar.proto.cliente.RespuestaConsultarClientes;
import com.rentar.proto.cliente.RespuestaValidarCredencialesLogin;
import com.rentar.proto.cliente.RespuestaVerificarClienteActivo;
import com.rentar.proto.cliente.ServicioClientesGrpc;
import com.rentar.proto.cliente.SolicitudActualizarCliente;
import com.rentar.proto.cliente.SolicitudConsultarClientes;
import com.rentar.proto.cliente.SolicitudCrearCliente;
import com.rentar.proto.cliente.SolicitudIdCliente;
import com.rentar.proto.cliente.SolicitudValidarCredencialesLogin;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio gRPC definido en proto/cliente.proto.
 * Sólo traduce mensajes: las reglas de negocio están en ClienteService y AuthService,
 * y las excepciones se convierten en códigos gRPC en GrpcErrorHandler.
 */
@Service
public class CustomerGrpcService extends ServicioClientesGrpc.ServicioClientesImplBase {

    private final ClienteService clienteService;
    private final AuthService authService;

    public CustomerGrpcService(ClienteService clienteService, AuthService authService) {
        this.clienteService = clienteService;
        this.authService = authService;
    }

    @Override
    public void crearCliente(SolicitudCrearCliente request, StreamObserver<Cliente> responseObserver) {
        ClienteResponse creado = clienteService.crear(ClienteMapper.toDomain(request));
        responder(responseObserver, ClienteMapper.toProto(creado));
    }

    @Override
    public void actualizarCliente(SolicitudActualizarCliente request, StreamObserver<Cliente> responseObserver) {
        ClienteResponse actualizado = clienteService.actualizar(request.getId(), ClienteMapper.toDomain(request));
        responder(responseObserver, ClienteMapper.toProto(actualizado));
    }

    @Override
    public void darDeBajaCliente(SolicitudIdCliente request, StreamObserver<Cliente> responseObserver) {
        responder(responseObserver, ClienteMapper.toProto(clienteService.darDeBaja(request.getId())));
    }

    @Override
    public void reactivarCliente(SolicitudIdCliente request, StreamObserver<Cliente> responseObserver) {
        responder(responseObserver, ClienteMapper.toProto(clienteService.reactivar(request.getId())));
    }

    @Override
    public void consultarClientes(SolicitudConsultarClientes request,
                                  StreamObserver<RespuestaConsultarClientes> responseObserver) {
        List<ClienteResponse> clientes = request.getIncluirInactivos()
                ? clienteService.listarTodos()
                : clienteService.listarActivos();

        RespuestaConsultarClientes.Builder respuesta = RespuestaConsultarClientes.newBuilder();
        clientes.forEach(c -> respuesta.addClientes(ClienteMapper.toProto(c)));
        responder(responseObserver, respuesta.build());
    }

    @Override
    public void consultarCliente(SolicitudIdCliente request, StreamObserver<Cliente> responseObserver) {
        responder(responseObserver, ClienteMapper.toProto(clienteService.buscarPorId(request.getId())));
    }

    @Override
    public void verificarClienteActivo(SolicitudIdCliente request,
                                       StreamObserver<RespuestaVerificarClienteActivo> responseObserver) {
        responder(responseObserver, ClienteMapper.toProto(clienteService.verificarActivo(request.getId())));
    }

    @Override
    public void validarCredencialesLogin(SolicitudValidarCredencialesLogin request,
                                         StreamObserver<RespuestaValidarCredencialesLogin> responseObserver) {
        // Según el contrato, credenciales inválidas no son un error gRPC sino credenciales_validas = false.
        RespuestaValidarCredencialesLogin respuesta;
        try {
            respuesta = ClienteMapper.toProto(authService.autenticar(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            respuesta = RespuestaValidarCredencialesLogin.newBuilder().setCredencialesValidas(false).build();
        }
        responder(responseObserver, respuesta);
    }

    private static <T> void responder(StreamObserver<T> responseObserver, T respuesta) {
        responseObserver.onNext(respuesta);
        responseObserver.onCompleted();
    }
}
