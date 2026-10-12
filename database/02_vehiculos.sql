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

    CONSTRAINT ck_vehiculo_tipo
        CHECK (
            tipo_vehiculo IS NULL
            OR tipo_vehiculo IN ('SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK')
        ),

    CONSTRAINT ck_vehiculo_estado
        CHECK (
            estado IN ('DISPONIBLE', 'RESERVADO', 'EN_ALQUILER')
        ),

    CONSTRAINT ck_vehiculo_precio
        CHECK (
            precio_diario IS NULL
            OR precio_diario >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_vehiculo_activo
    ON vehiculo(activo);

CREATE INDEX IF NOT EXISTS idx_vehiculo_tipo
    ON vehiculo(tipo_vehiculo);

CREATE INDEX IF NOT EXISTS idx_vehiculo_marca_modelo
    ON vehiculo(marca, modelo);

CREATE INDEX IF NOT EXISTS idx_vehiculo_estado
    ON vehiculo(estado);
