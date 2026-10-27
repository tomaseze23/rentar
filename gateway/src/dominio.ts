// Valores "de negocio" que expone la API REST/GraphQL del gateway (SEDAN, CONFIRMADA...).
// En los .proto los valores de enum llevan el nombre del enum como prefijo
// (TIPO_VEHICULO_SEDAN, ESTADO_RESERVA_CONFIRMADA), requisito de Buf Lint. Este
// módulo traduce entre ambos mundos para que la API pública no cambie.

export type TipoVehiculo = 'SEDAN' | 'SUV' | 'PICKUP' | 'COUPE' | 'HATCHBACK';
export type EstadoVehiculo = 'DISPONIBLE' | 'RESERVADO' | 'EN_ALQUILER';
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';
export type EstadoHistorial = 'FINALIZADA' | 'CANCELADA';

const P_TIPO = 'TIPO_VEHICULO_';
const P_ESTADO_VEHICULO = 'ESTADO_VEHICULO_';
const P_ESTADO_RESERVA = 'ESTADO_RESERVA_';
const P_ESTADO_HISTORIAL = 'ESTADO_HISTORIAL_';

// Del valor público al valor del .proto (undefined se conserva: filtro ausente).
export const aProto = {
    tipo: (v?: TipoVehiculo) => (v === undefined ? undefined : `${P_TIPO}${v}`),
    estadoVehiculo: (v: EstadoVehiculo) => `${P_ESTADO_VEHICULO}${v}`,
    estadoReserva: (v?: EstadoReserva) => (v === undefined ? undefined : `${P_ESTADO_RESERVA}${v}`),
} as const;

// Del valor del .proto al valor público.
const quitar = (prefijo: string, v: string) => (v.startsWith(prefijo) ? v.slice(prefijo.length) : v);
export const deProto = {
    tipo: (v: string) => quitar(P_TIPO, v) as TipoVehiculo,
    estadoVehiculo: (v: string) => quitar(P_ESTADO_VEHICULO, v) as EstadoVehiculo,
    estadoReserva: (v: string) => quitar(P_ESTADO_RESERVA, v) as EstadoReserva,
    estadoHistorial: (v: string) => quitar(P_ESTADO_HISTORIAL, v) as EstadoHistorial,
} as const;
