-- Datos de prueba para rentar_alquileres: 17 reservas.
--   * 5 alquileres terminados (CONFIRMADA con período ya pasado): aparecen en el historial como FINALIZADA.
--   * 3 reservas CANCELADA (una en el pasado y dos futuras).
--   * 9 reservas CONFIRMADA futuras, sin superposición entre reservas del mismo vehículo.
-- Las fechas son relativas al momento de la carga (hoy a las 10:00 más/menos N días).
--
-- cliente_id y vehiculo_id corresponden al orden de 04_datos_prueba_clientes.sql y
-- 05_datos_prueba_vehiculos.sql en una base nueva (clientes 1-8, vehículos 1-14).
-- No hay foreign keys: son ids de otros servicios.
-- Es repetible: solo carga si la tabla reservas está vacía.
-- Se ejecuta DESPUÉS de 03_alquileres.sql.

INSERT INTO reservas (cliente_id, vehiculo_id, fecha_inicio, fecha_fin, precio_diario, importe_total, estado)
SELECT r.cliente_id,
       r.vehiculo_id,
       base.inicio + make_interval(days => r.desde),
       base.inicio + make_interval(days => r.desde + r.dias),
       r.precio_diario,
       r.dias * r.precio_diario,   -- días facturados x precio diario (ceil de 24 h: acá son días exactos)
       r.estado
FROM (SELECT date_trunc('day', now()) + interval '10 hours' AS inicio) AS base,
     (VALUES
        -- cliente, vehículo, desde (días, negativo = pasado), duración (días), precio diario, estado
        -- Alquileres terminados (historial)
        (1,  1, -30, 3,  38000, 'CONFIRMADA'),
        (2,  4, -25, 2,  65000, 'CONFIRMADA'),
        (3,  7, -20, 5,  78000, 'CONFIRMADA'),
        (1, 10, -12, 2,  28000, 'CONFIRMADA'),
        (4,  2,  -9, 3,  42000, 'CONFIRMADA'),
        -- Cancelada en el pasado
        (5,  5, -15, 2,  58000, 'CANCELADA'),
        -- Futuras confirmadas
        (1,  1,   2, 3,  38000, 'CONFIRMADA'),
        (2,  1,  10, 2,  38000, 'CONFIRMADA'),
        (3,  2,   3, 5,  42000, 'CONFIRMADA'),
        (4,  4,   1, 4,  65000, 'CONFIRMADA'),
        (5,  4,   8, 3,  65000, 'CONFIRMADA'),
        (6,  7,   5, 7,  78000, 'CONFIRMADA'),
        (7, 10,   2, 2,  28000, 'CONFIRMADA'),
        (8, 13,   4, 1, 120000, 'CONFIRMADA'),
        (1, 11,  15, 3,  30000, 'CONFIRMADA'),
        -- Futuras canceladas
        (2,  5,   6, 2,  58000, 'CANCELADA'),
        (3,  8,  12, 4,  72000, 'CANCELADA')
     ) AS r (cliente_id, vehiculo_id, desde, dias, precio_diario, estado)
WHERE NOT EXISTS (SELECT 1 FROM reservas);
