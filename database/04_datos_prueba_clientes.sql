-- Datos de prueba para rentar_clientes: 8 clientes (con su usuario para iniciar sesión).
-- Contraseña de todos: cliente1234 (guardada con BCrypt, igual que el resto de las claves).
-- Es repetible: lo que ya existe (mismo email o documento) se saltea.
-- Se ejecuta DESPUÉS de 01_clientes.sql. El admin (admin@rentar.com) lo crea el Customer Service al arrancar.

INSERT INTO usuario (email, password, rol, activo)
VALUES
    ('ana.perez@mail.com',         '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('luis.gomez@mail.com',        '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('marta.rodriguez@mail.com',   '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('julian.fernandez@mail.com',  '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('sofia.lopez@mail.com',       '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('carlos.martinez@mail.com',   '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('valentina.diaz@mail.com',    '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE),
    ('diego.sosa@mail.com',        '$2a$10$drVcLpAkz/aC.Uu2XE2g1.OYg2kd8KSCQnuPmAxjiuBrVR4A73ncK', 'CLIENTE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- El orden de esta lista define los ids de cliente (1 a 8 en una base nueva);
-- 06_datos_prueba_alquileres.sql los usa.
INSERT INTO cliente (usuario_id, documento, nombre, apellido, telefono, fecha_nacimiento, activo)
SELECT u.id, d.documento, d.nombre, d.apellido, d.telefono, d.fecha_nacimiento::date, TRUE
FROM (VALUES
    ('ana.perez@mail.com',        '30111222', 'Ana',       'Pérez',      '1155550001', '1990-03-14'),
    ('luis.gomez@mail.com',       '30333444', 'Luis',      'Gómez',      '1155550002', '1985-07-02'),
    ('marta.rodriguez@mail.com',  '28999111', 'Marta',     'Rodríguez',  '1155550003', '1978-11-23'),
    ('julian.fernandez@mail.com', '35222333', 'Julián',    'Fernández',  '1155550004', '1996-01-30'),
    ('sofia.lopez@mail.com',      '37444555', 'Sofía',     'López',      '1155550005', '1999-09-09'),
    ('carlos.martinez@mail.com',  '25666777', 'Carlos',    'Martínez',   '1155550006', '1972-05-18'),
    ('valentina.diaz@mail.com',   '40888999', 'Valentina', 'Díaz',       '1155550007', '2001-12-04'),
    ('diego.sosa@mail.com',       '33777888', 'Diego',     'Sosa',       '1155550008', '1992-08-27')
) AS d (email, documento, nombre, apellido, telefono, fecha_nacimiento)
JOIN usuario u ON u.email = d.email
ON CONFLICT (documento) DO NOTHING;
