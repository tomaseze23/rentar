-- Datos de prueba para rentar_vehiculos: 14 vehículos con patentes fijas.
-- Las patentes son las "patentes de prueba" compartidas por el grupo.
-- Es repetible: una patente que ya existe se saltea.
-- Se ejecuta DESPUÉS de 02_vehiculos.sql.

-- El orden de esta lista define los ids de vehículo (1 a 14 en una base nueva);
-- 06_datos_prueba_alquileres.sql los usa.
INSERT INTO vehiculo (patente, marca, modelo, anio, color, tipo_vehiculo, precio_diario, estado, activo)
VALUES
    ('AB123CD', 'Chevrolet',  'Cruze',     '2022', 'Azul',     'SEDAN',     38000, 'DISPONIBLE', TRUE),
    ('AC234DE', 'Toyota',     'Corolla',   '2023', 'Blanco',   'SEDAN',     42000, 'DISPONIBLE', TRUE),
    ('AD345EF', 'Volkswagen', 'Vento',     '2021', 'Gris',     'SEDAN',     36000, 'DISPONIBLE', TRUE),
    ('AE456FG', 'Toyota',     'RAV4',      '2023', 'Negro',    'SUV',       65000, 'DISPONIBLE', TRUE),
    ('AF567GH', 'Ford',       'Territory', '2022', 'Rojo',     'SUV',       58000, 'DISPONIBLE', TRUE),
    ('AG678HI', 'Jeep',       'Renegade',  '2021', 'Verde',    'SUV',       54000, 'DISPONIBLE', TRUE),
    ('AH789IJ', 'Toyota',     'Hilux',     '2023', 'Blanco',   'PICKUP',    78000, 'DISPONIBLE', TRUE),
    ('AI890JK', 'Ford',       'Ranger',    '2022', 'Gris',     'PICKUP',    72000, 'DISPONIBLE', TRUE),
    ('AJ901KL', 'Volkswagen', 'Amarok',    '2021', 'Negro',    'PICKUP',    70000, 'DISPONIBLE', TRUE),
    ('AK012LM', 'Fiat',       'Cronos',    '2024', 'Rojo',     'HATCHBACK', 28000, 'DISPONIBLE', TRUE),
    ('AL123MN', 'Peugeot',    '208',       '2023', 'Azul',     'HATCHBACK', 30000, 'DISPONIBLE', TRUE),
    ('AM234NO', 'Renault',    'Sandero',   '2022', 'Blanco',   'HATCHBACK', 26000, 'DISPONIBLE', TRUE),
    ('AN345OP', 'Ford',       'Mustang',   '2022', 'Amarillo', 'COUPE',    120000, 'DISPONIBLE', TRUE),
    ('AO456PQ', 'Chevrolet',  'Camaro',    '2021', 'Negro',    'COUPE',    115000, 'DISPONIBLE', TRUE)
ON CONFLICT (patente) DO NOTHING;
