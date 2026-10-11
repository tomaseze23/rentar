import { status } from '@grpc/grpc-js';
import { proto } from '../grpc/loader';
import { grpcError, serve } from './server';
import type { ServicioClientesHandlers } from '../generated/rentar/cliente/v1/ServicioClientes';

interface Usuario { id: number; email: string; password: string; rol: 'ADMINISTRADOR' | 'CLIENTE'; activo: boolean }
interface Cliente {
  id: number; usuarioId: number; email: string; documento: string; nombre: string; apellido: string;
  telefono?: string; fechaNacimiento?: string; activo: boolean;
}

// Datos de prueba (en el mock las contraseñas van en texto plano; el servicio real usa BCrypt).
// admin@rentar.com / admin1234   |   cliente1@mail.com y cliente2@mail.com / cliente1234
const usuarios: Usuario[] = [
  { id: 1, email: 'admin@rentar.com', password: 'admin1234', rol: 'ADMINISTRADOR', activo: true },
  { id: 2, email: 'cliente1@mail.com', password: 'cliente1234', rol: 'CLIENTE', activo: true },
  { id: 3, email: 'cliente2@mail.com', password: 'cliente1234', rol: 'CLIENTE', activo: true },
];
const clientes: Cliente[] = [
  { id: 1, usuarioId: 2, email: 'cliente1@mail.com', documento: '30111222', nombre: 'Ana', apellido: 'Pérez', activo: true },
  { id: 2, usuarioId: 3, email: 'cliente2@mail.com', documento: '30333444', nombre: 'Luis', apellido: 'Gómez', activo: true },
];

const buscar = (id: string | number) => clientes.find((c) => String(c.id) === String(id));

const handlers = {
    ValidarCredencialesLogin: (call, cb) => {
        const u = usuarios.find((x) => x.email === call.request.email);
        // Credenciales incorrectas o usuario inactivo: mismo resultado (el gateway responde 401).
        if (!u || !u.activo || u.password !== call.request.password) {
          return cb(null, { credencialesValidas: false });
        }
        const c = clientes.find((x) => x.usuarioId === u.id);
        cb(null, { credencialesValidas: true, usuarioId: u.id, clienteId: c?.id, email: u.email, rol: u.rol });
    },

    ConsultarCliente: (call, cb) => {
        const c = buscar (call.request.id);
        c ? cb (null, c) : cb (grpcError(status.NOT_FOUND, `Cliente no encontrado con ID: ${call.request.id}`));
    },

    VerificarClienteActivo: (call, cb) => {
        const c = buscar(call.request.id);
        cb(null, { existe: !!c, activo: !!c?.activo });
    },
} as ServicioClientesHandlers;

export const startClienteMock = (addr = '0.0.0.0:9092') =>
    serve(addr, proto.rentar.cliente.v1.ServicioClientes.service, handlers);