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

// Contadores para asignar ids nuevos en las altas
let nextUsuario = 4;
let nextCliente = 3;

const buscar = (id: string | number) => clientes.find((c) => String(c.id) === String(id));
const noExiste = (id: string | number) => grpcError(status.NOT_FOUND, `Cliente no encontrado con ID: ${id}`);

const handlers = {
  CrearCliente: (call, cb) => {
    const r = call.request;
    if (!r.email || !r.password || !r.documento || !r.nombre || !r.apellido) {
      return cb(grpcError(status.INVALID_ARGUMENT, 'Faltan datos obligatorios'));
    }
    if (usuarios.some((u) => u.email === r.email)) {
      return cb(grpcError(status.ALREADY_EXISTS, `Ya existe un usuario con el email: ${r.email}`));
    }
    if (clientes.some((c) => c.documento === r.documento)) {
      return cb(grpcError(status.ALREADY_EXISTS, `Ya existe un cliente con el documento: ${r.documento}`));
    }
    const u: Usuario = { id: nextUsuario++, email: r.email, password: r.password, rol: 'CLIENTE', activo: true };
    const c: Cliente = {
      id: nextCliente++, usuarioId: u.id, email: r.email, documento: r.documento, nombre: r.nombre,
      apellido: r.apellido, telefono: r.telefono, fechaNacimiento: r.fechaNacimiento, activo: true,
    };
    usuarios.push(u);
    clientes.push(c);
    cb(null, c);
  },

  ActualizarCliente: (call, cb) => {
    const r = call.request;
    const c = buscar(r.id);
    if (!c) return cb(noExiste(r.id));
    Object.assign(c, { nombre: r.nombre, apellido: r.apellido, telefono: r.telefono, fechaNacimiento: r.fechaNacimiento });
    cb(null, c);
  },

  DarDeBajaCliente: (call, cb) => {
    const c = buscar(call.request.id);
    if (!c) return cb(noExiste(call.request.id));
    c.activo = false;
    const u = usuarios.find((x) => x.id === c.usuarioId);
    if (u) u.activo = false; // un cliente dado de baja no puede iniciar sesión
    cb(null, c);
  },

  ReactivarCliente: (call, cb) => {
    const c = buscar(call.request.id);
    if (!c) return cb(noExiste(call.request.id));
    if (c.activo) return cb(grpcError(status.FAILED_PRECONDITION, `El cliente ${c.id} ya se encuentra activo`));
    c.activo = true;
    const u = usuarios.find((x) => x.id === c.usuarioId);
    if (u) u.activo = true;
    cb(null, c);
  },

  ConsultarClientes: (call, cb) => {
    cb(null, { clientes: clientes.filter((c) => call.request.incluirInactivos || c.activo) });
  },

  ConsultarCliente: (call, cb) => {
    const c = buscar(call.request.id);
    c ? cb(null, c) : cb(noExiste(call.request.id));
  },

  VerificarClienteActivo: (call, cb) => {
    const c = buscar(call.request.id);
    cb(null, { existe: !!c, activo: !!c?.activo });
  },

  ValidarCredencialesLogin: (call, cb) => {
    const u = usuarios.find((x) => x.email === call.request.email);
    // Credenciales incorrectas o usuario inactivo: mismo resultado (el gateway responde 401).
    if (!u || !u.activo || u.password !== call.request.password) {
      return cb(null, { credencialesValidas: false });
    }
    const c = clientes.find((x) => x.usuarioId === u.id);
    cb(null, { credencialesValidas: true, usuarioId: u.id, clienteId: c?.id, email: u.email, rol: u.rol });
  },
} as ServicioClientesHandlers;

export const startClienteMock = (addr = '0.0.0.0:9092') =>
  serve(addr, proto.rentar.cliente.v1.ServicioClientes.service, handlers);