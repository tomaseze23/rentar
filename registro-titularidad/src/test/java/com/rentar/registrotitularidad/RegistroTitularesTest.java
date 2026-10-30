package com.rentar.registrotitularidad;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistroTitularesTest {

    private static RegistroTitulares registro(String contenido) {
        RegistroTitulares r = new RegistroTitulares(new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8)));
        r.cargar();
        return r;
    }

    @Test
    void encuentraElTitularNormalizandoLaPatente() {
        RegistroTitulares r = registro("# comentario\n\nAB123CD;30111222\n");

        assertEquals(Optional.of("30111222"), r.titularDe("ab 123cd"));
        assertEquals(Optional.empty(), r.titularDe("ZZ999ZZ"));
        assertEquals(Optional.empty(), r.titularDe(null));
    }

    @Test
    void fallaSiUnaLineaEstaMalFormada() {
        assertThrows(IllegalStateException.class, () -> registro("AB123CD 30111222\n"));
    }
}
