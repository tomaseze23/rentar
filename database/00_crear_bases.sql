SELECT 'CREATE DATABASE rentar_clientes'
    WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = 'rentar_clientes'
)


SELECT 'CREATE DATABASE rentar_vehiculos'
    WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = 'rentar_vehiculos'
)


SELECT 'CREATE DATABASE rentar_alquileres'
    WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = 'rentar_alquileres'
)
