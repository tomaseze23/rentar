// Valores "de negocio" que expone la API REST/GraphQL del gateway (SEDAN, CONFIRMADA...).
// En los .proto los valores de enum llevan el nombre del enum como prefijo
// (TIPO_VEHICULO_SEDAN, ESTADO_RESERVA_CONFIRMADA), requisito de Buf Lint. Este
// módulo traduce entre ambos mundos para que la API pública no cambie.

export type TipoVehiculo = 'SEDAN' | 'SUV' | 'PICKUP' | 'COUPE' | 'HATCHBACK';
export type EstadoVehiculo = 'DISPONIBLE' | 'RESERVADO' | 'EN_ALQUILER';
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';
export type EstadoHistorial = 'FINALIZADA' | 'CANCELADA';
export type Verificacion = 'PENDIENTE' | 'VALIDADO' | 'ROBADO';
export type ResultadoValidacion = 'PENDIENTE' | 'VALIDADO' | 'RECHAZADO';
export type EstadoSolicitud = 'PENDIENTE' | 'RECHAZADA' | 'INCORPORADA';
export type Mantenimiento = 'PENDIENTE' | 'REALIZADO' | 'NO_REALIZADO';

const P_TIPO = 'TIPO_VEHICULO_';
const P_ESTADO_VEHICULO = 'ESTADO_VEHICULO_';
const P_ESTADO_RESERVA = 'ESTADO_RESERVA_';
const P_ESTADO_HISTORIAL = 'ESTADO_HISTORIAL_';
const P_VERIFICACION = 'ESTADO_VERIFICACION_';
const P_VALIDACION = 'RESULTADO_VALIDACION_';
const P_SOLICITUD = 'ESTADO_SOLICITUD_INCORPORACION_';
const P_MANTENIMIENTO = 'RESULTADO_MANTENIMIENTO_';

// Del valor público al valor del .proto (undefined se conserva: filtro ausente).
export const aProto = {
    tipo: (v?: TipoVehiculo) => (v === undefined ? undefined : `${P_TIPO}${v}`),
    estadoVehiculo: (v: EstadoVehiculo) => `${P_ESTADO_VEHICULO}${v}`,
    estadoReserva: (v?: EstadoReserva) => (v === undefined ? undefined : `${P_ESTADO_RESERVA}${v}`),
} as const;

// Del valor del .proto al valor público.
const quitar = (prefijo: string, v: string) => (v.startsWith(prefijo) ? v.slice(prefijo.length) : v);

// Los enums del Hito 3 tienen NO_ESPECIFICADO como valor 0 ("sin resultado todavía"). Mientras un
// servicio no setee el campo, llega 0: se informa como PENDIENTE, que es lo que significa para el cliente.
const quitarPendiente = (prefijo: string, v:string) => {
    const r = quitar(prefijo, v);
    return r === 'NO_ESPECIFICADO' ? 'PENDIENTE' : r;
};

export const deProto = {
    tipo: (v: string) => quitar(P_TIPO, v) as TipoVehiculo,
    estadoVehiculo: (v: string) => quitar(P_ESTADO_VEHICULO, v) as EstadoVehiculo,
    estadoReserva: (v: string) => quitar(P_ESTADO_RESERVA, v) as EstadoReserva,
    estadoHistorial: (v: string) => quitar(P_ESTADO_HISTORIAL, v) as EstadoHistorial,
    verificacion: (v: string) => quitarPendiente(P_VERIFICACION, v) as Verificacion,
    validacion: (v: string) => quitarPendiente(P_VALIDACION, v) as ResultadoValidacion,
    estadoSolicitud: (v: string) => quitarPendiente(P_SOLICITUD, v) as EstadoSolicitud,
    mantenimiento: (v: string) => quitarPendiente(P_MANTENIMIENTO, v) as Mantenimiento,
} as const;
