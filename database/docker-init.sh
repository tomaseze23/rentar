#!/bin/bash
# Inicialización de PostgreSQL dentro de Docker (lo ejecuta la imagen oficial de postgres
# la primera vez que se crea el volumen). Corre los scripts de esta carpeta cada uno
# contra la base que le corresponde, en el orden del README.
set -e

psql_en() {
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$1" -f "$2"
}

psql_en postgres          /scripts/00_crear_bases.sql
psql_en rentar_clientes   /scripts/01_clientes.sql
psql_en rentar_vehiculos  /scripts/02_vehiculos.sql
psql_en rentar_alquileres /scripts/03_alquileres.sql
