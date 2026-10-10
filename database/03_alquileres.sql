CREATE TABLE IF NOT EXISTS reservas (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    vehiculo_id BIGINT NOT NULL,
    fecha_inicio TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fecha_fin TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    precio_diario NUMERIC(12,2) NOT NULL,
    importe_total NUMERIC(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',

    CONSTRAINT ck_reserva_fechas
        CHECK (fecha_fin > fecha_inicio),

    CONSTRAINT ck_reserva_precio_diario
        CHECK (precio_diario >= 0),

    CONSTRAINT ck_reserva_importe_total
        CHECK (importe_total >= 0),

    CONSTRAINT ck_reserva_estado
        CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA'))
);

CREATE INDEX IF NOT EXISTS idx_reserva_cliente
    ON reservas(cliente_id);

CREATE INDEX IF NOT EXISTS idx_reserva_vehiculo
    ON reservas(vehiculo_id);

CREATE INDEX IF NOT EXISTS idx_reserva_estado
    ON reservas(estado);

CREATE INDEX IF NOT EXISTS idx_reserva_periodo
    ON reservas(fecha_inicio, fecha_fin);

CREATE INDEX IF NOT EXISTS idx_reserva_vehiculo_periodo
    ON reservas(vehiculo_id, fecha_inicio, fecha_fin);
