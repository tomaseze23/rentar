CREATE TABLE IF NOT EXISTS vehiculo (
    id BIGSERIAL PRIMARY KEY,
    patente VARCHAR(255) NOT NULL UNIQUE,
    marca VARCHAR(255) NOT NULL,
    modelo VARCHAR(255) NOT NULL,
    anio VARCHAR(20) NOT NULL,
    color VARCHAR(255),
    tipo_vehiculo VARCHAR(30),
    precio_diario NUMERIC(12,2),
    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    verificacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    -- Cliente dueño si el vehículo entró por incorporación externa; NULL para la flota de Rentar
    propietario_cliente_id BIGINT,

    CONSTRAINT ck_vehiculo_tipo
        CHECK (
            tipo_vehiculo IS NULL
            OR tipo_vehiculo IN ('SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK')
        ),

    CONSTRAINT ck_vehiculo_estado
        CHECK (
            estado IN ('DISPONIBLE', 'RESERVADO', 'EN_ALQUILER')
        ),

    CONSTRAINT ck_vehiculo_verificacion
        CHECK (verificacion IN ('PENDIENTE', 'VALIDADO', 'ROBADO')),

    CONSTRAINT ck_vehiculo_precio
        CHECK (
            precio_diario IS NULL
            OR precio_diario >= 0
        )
);

-- Bases creadas antes del Hito 3: agrega la columna si falta (si ya existe, no hace nada).
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS verificacion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS propietario_cliente_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_vehiculo_activo
    ON vehiculo(activo);

CREATE INDEX IF NOT EXISTS idx_vehiculo_tipo
    ON vehiculo(tipo_vehiculo);

CREATE INDEX IF NOT EXISTS idx_vehiculo_marca_modelo
    ON vehiculo(marca, modelo);

CREATE INDEX IF NOT EXISTS idx_vehiculo_estado
    ON vehiculo(estado);

-- Solicitudes de incorporación de vehículos de propietarios externos (Hito 3).
-- El id es el vehiculoExternoId de los eventos vehiculos.externos.*.
CREATE TABLE IF NOT EXISTS solicitud_incorporacion (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    patente VARCHAR(255) NOT NULL,
    marca VARCHAR(255) NOT NULL,
    modelo VARCHAR(255) NOT NULL,
    anio VARCHAR(20) NOT NULL,
    color VARCHAR(255),
    tipo_vehiculo VARCHAR(30) NOT NULL,
    precio_diario NUMERIC(12,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    validacion_patente VARCHAR(30) NOT NULL,
    validacion_titularidad VARCHAR(30) NOT NULL,
    completada_publicada BOOLEAN NOT NULL DEFAULT FALSE,
    vehiculo_id BIGINT REFERENCES vehiculo(id),
    fecha_solicitud TIMESTAMP NOT NULL,

    CONSTRAINT ck_solicitud_tipo
        CHECK (tipo_vehiculo IN ('SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK')),

    CONSTRAINT ck_solicitud_estado
        CHECK (estado IN ('PENDIENTE', 'RECHAZADA', 'INCORPORADA')),

    CONSTRAINT ck_solicitud_validacion_patente
        CHECK (validacion_patente IN ('PENDIENTE', 'VALIDADO', 'RECHAZADO')),

    CONSTRAINT ck_solicitud_validacion_titularidad
        CHECK (validacion_titularidad IN ('PENDIENTE', 'VALIDADO', 'RECHAZADO')),

    CONSTRAINT ck_solicitud_precio
        CHECK (precio_diario > 0)
);

CREATE INDEX IF NOT EXISTS idx_solicitud_cliente
    ON solicitud_incorporacion(cliente_id);

CREATE INDEX IF NOT EXISTS idx_solicitud_patente_estado
    ON solicitud_incorporacion(patente, estado);
