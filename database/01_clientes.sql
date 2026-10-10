CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT ck_usuario_rol
        CHECK (rol IN ('ADMINISTRADOR', 'CLIENTE'))
);

CREATE TABLE IF NOT EXISTS cliente (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    documento VARCHAR(255) NOT NULL UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    apellido VARCHAR(255) NOT NULL,
    telefono VARCHAR(255),
    fecha_nacimiento DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_cliente_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_cliente_activo
    ON cliente(activo);

CREATE INDEX IF NOT EXISTS idx_cliente_apellido_nombre
    ON cliente(apellido, nombre);
