package com.rentar.registropolicial;

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
import java.util.HashSet;
import java.util.Set;

/**
 * Registro propio que SIMULA la información policial: lista de patentes denunciadas como robadas,
 * cargada desde un archivo (una patente por línea; las líneas vacías y las que empiezan con # se ignoran).
 */
@Component
public class RegistroPatentes {

    private static final Logger log = LoggerFactory.getLogger(RegistroPatentes.class);

    private final Resource archivo;
    private final Set<String> denunciadas = new HashSet<>();

    public RegistroPatentes(@Value("${registro.patentes-denunciadas:classpath:patentes-denunciadas.txt}") Resource archivo) {
        this.archivo = archivo;
    }

    @PostConstruct
    void cargar() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            r.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(RegistroPatentes::normalizar)
                    .forEach(denunciadas::add);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el registro de patentes denunciadas", e);
        }
        log.info("Registro policial cargado: {} patentes denunciadas", denunciadas.size());
    }

    public boolean estaDenunciada(String patente) {
        return patente != null && denunciadas.contains(normalizar(patente));
    }

    // Misma normalización que hace el Vehicle Service al guardar la patente
    private static String normalizar(String patente) {
        return patente.trim().toUpperCase().replace(" ", "");
    }
}
