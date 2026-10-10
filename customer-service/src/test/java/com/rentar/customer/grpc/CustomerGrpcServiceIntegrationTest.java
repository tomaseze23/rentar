package com.rentar.customer.grpc;

import com.rentar.proto.cliente.Cliente;
import com.rentar.proto.cliente.RespuestaValidarCredencialesLogin;
import com.rentar.proto.cliente.RespuestaVerificarClienteActivo;
import com.rentar.proto.cliente.ServicioClientesGrpc;
import com.rentar.proto.cliente.SolicitudActualizarCliente;
import com.rentar.proto.cliente.SolicitudConsultarClientes;
import com.rentar.proto.cliente.SolicitudCrearCliente;
import com.rentar.proto.cliente.SolicitudIdCliente;
import com.rentar.proto.cliente.SolicitudValidarCredencialesLogin;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba punta a punta del contrato: levanta el servicio completo (gRPC + JPA sobre H2
 * con el esquema de database/01_clientes.sql) y lo invoca con un stub gRPC real,
 * como lo haría el API Gateway.
 */
@SpringBootTest
class CustomerGrpcServiceIntegrationTest {

    private static ManagedChannel channel;
    private static ServicioClientesGrpc.ServicioClientesBlockingStub stub;

    @BeforeAll
    static void conectar() {
        channel = InProcessChannelBuilder.forName("customer-service-test").directExecutor().build();
        stub = ServicioClientesGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void desconectar() {
        channel.shutdownNow();
    }

    private static SolicitudCrearCliente alta(String email, String documento) {
        return SolicitudCrearCliente.newBuilder()
                .setEmail(email)
                .setPassword("secreto123")
                .setDocumento(documento)
                .setNombre("Juan")
                .setApellido("Pérez")
                .setFechaNacimiento("1990-05-20")
                .build();
    }

    private static SolicitudValidarCredencialesLogin login(String email, String password) {
        return SolicitudValidarCredencialesLogin.newBuilder().setEmail(email).setPassword(password).build();
    }

    private static Status.Code codigo(Runnable llamada) {
        try {
            llamada.run();
        } catch (StatusRuntimeException e) {
            return e.getStatus().getCode();
        }
        throw new AssertionError("Se esperaba un error gRPC");
    }

    @Test
    void cicloDeVidaCompletoDeUnCliente() {
        Cliente creado = stub.crearCliente(alta("juan@mail.com", "30123456"));
        assertThat(creado.getId()).isPositive();
        assertThat(creado.getActivo()).isTrue();
        assertThat(creado.getFechaNacimiento()).isEqualTo("1990-05-20");
        assertThat(creado.hasTelefono()).isFalse();

        SolicitudIdCliente id = SolicitudIdCliente.newBuilder().setId(creado.getId()).build();
        assertThat(stub.consultarCliente(id).getEmail()).isEqualTo("juan@mail.com");
        RespuestaVerificarClienteActivo verificacion = stub.verificarClienteActivo(id);
        assertThat(verificacion.getExiste()).isTrue();
        assertThat(verificacion.getActivo()).isTrue();

        Cliente actualizado = stub.actualizarCliente(SolicitudActualizarCliente.newBuilder()
                .setId(creado.getId()).setNombre("Juan Carlos").setApellido("Pérez").setTelefono("1122334455")
                .build());
        assertThat(actualizado.getNombre()).isEqualTo("Juan Carlos");
        assertThat(actualizado.getTelefono()).isEqualTo("1122334455");
        assertThat(actualizado.hasFechaNacimiento()).isFalse();

        RespuestaValidarCredencialesLogin credenciales = stub.validarCredencialesLogin(
                login("juan@mail.com", "secreto123"));
        assertThat(credenciales.getCredencialesValidas()).isTrue();
        assertThat(credenciales.getRol()).isEqualTo("CLIENTE");
        assertThat(credenciales.getClienteId()).isEqualTo(creado.getId());

        assertThat(stub.darDeBajaCliente(id).getActivo()).isFalse();
        verificacion = stub.verificarClienteActivo(id);
        assertThat(verificacion.getExiste()).isTrue();
        assertThat(verificacion.getActivo()).isFalse();
        assertThat(stub.consultarClientes(SolicitudConsultarClientes.getDefaultInstance()).getClientesList())
                .noneMatch(c -> c.getId() == creado.getId());
        assertThat(stub.consultarClientes(SolicitudConsultarClientes.newBuilder().setIncluirInactivos(true).build())
                .getClientesList())
                .anyMatch(c -> c.getId() == creado.getId());
        // Un cliente dado de baja no puede iniciar sesión
        assertThat(stub.validarCredencialesLogin(login("juan@mail.com", "secreto123")).getCredencialesValidas())
                .isFalse();

        assertThat(stub.reactivarCliente(id).getActivo()).isTrue();
        assertThat(codigo(() -> stub.reactivarCliente(id))).isEqualTo(Status.Code.FAILED_PRECONDITION);
    }

    @Test
    void erroresDelDominioSeTraducenACodigosGrpc() {
        stub.crearCliente(alta("ana@mail.com", "40111222"));

        assertThat(codigo(() -> stub.crearCliente(alta("ana@mail.com", "99999999"))))
                .isEqualTo(Status.Code.ALREADY_EXISTS);
        assertThat(codigo(() -> stub.crearCliente(alta("otra@mail.com", "40111222"))))
                .isEqualTo(Status.Code.ALREADY_EXISTS);
        assertThat(codigo(() -> stub.crearCliente(alta("otra@mail.com", "1").toBuilder().setNombre("").build())))
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(codigo(() -> stub.crearCliente(
                alta("otra@mail.com", "1").toBuilder().setFechaNacimiento("20/05/1990").build())))
                .isEqualTo(Status.Code.INVALID_ARGUMENT);

        SolicitudIdCliente inexistente = SolicitudIdCliente.newBuilder().setId(9999).build();
        assertThat(codigo(() -> stub.consultarCliente(inexistente))).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(stub.verificarClienteActivo(inexistente).getExiste()).isFalse();
    }

    @Test
    void credencialesInvalidasNoSonUnErrorGrpc() {
        RespuestaValidarCredencialesLogin respuesta = stub.validarCredencialesLogin(
                login("admin@rentar.com", "incorrecta"));

        assertThat(respuesta.getCredencialesValidas()).isFalse();
        assertThat(respuesta.hasUsuarioId()).isFalse();
        assertThat(stub.validarCredencialesLogin(login("", "")).getCredencialesValidas()).isFalse();
    }

    @Test
    void elAdminSembradoPuedeIniciarSesion() {
        RespuestaValidarCredencialesLogin admin = stub.validarCredencialesLogin(
                login("admin@rentar.com", "admin1234"));

        assertThat(admin.getCredencialesValidas()).isTrue();
        assertThat(admin.getRol()).isEqualTo("ADMINISTRADOR");
        assertThat(admin.hasClienteId()).isFalse();
    }
}
