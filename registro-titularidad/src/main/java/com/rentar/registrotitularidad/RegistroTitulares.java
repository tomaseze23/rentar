package com.rentar.registrotitularidad;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro propio que SIMULA el registro de la propiedad automotor: qué documento es el titular de cada patente.
 * Se carga desde un archivo con líneas PATENTE;DOCUMENTO (las vacías y las que empiezan con # se ignoran).
 */
@Component
public class RegistroTitulares {

    private static final Logger log = LoggerFactory.getLogger(RegistroTitulares.class);

    private final Resource archivo;
    private final Map<String, String> titularPorPatente = new HashMap<>();

    public RegistroTitulares(@Value("${registro.titulares:classpath:titulares.txt}") Resource archivo) {
        this.archivo = archivo;
    }

    @PostConstruct
    void cargar() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            r.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .forEach(this::agregar);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el registro de titularidad", e);
        }
        log.info("Registro de titularidad cargado: {} patentes", titularPorPatente.size());
    }

    private void agregar(String linea) {
        String[] partes = linea.split(";");
        if (partes.length != 2 || partes[0].isBlank() || partes[1].isBlank()) {
            throw new IllegalStateException("Línea inválida en el registro de titularidad: " + linea);
        }
        titularPorPatente.put(normalizarPatente(partes[0]), partes[1].trim());
    }

    /** Documento del titular de la patente, o vacío si la patente no figura en el registro. */
    public Optional<String> titularDe(String patente) {
        return patente == null ? Optional.empty() : Optional.ofNullable(titularPorPatente.get(normalizarPatente(patente)));
    }

    // Misma normalización que hace el Vehicle Service al guardar la patente
    static String normalizarPatente(String patente) {
        return patente.trim().toUpperCase().replace(" ", "");
    }
}
