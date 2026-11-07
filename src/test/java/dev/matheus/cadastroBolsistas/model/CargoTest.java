package dev.matheus.cadastroBolsistas.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CargoTest {

    @Test
    void deString_retornaEnumCorretoParaValorValido() {
        assertEquals(Cargo.DESENVOLVEDOR, Cargo.deString("DESENVOLVEDOR"));
        assertEquals(Cargo.PESQUISADOR, Cargo.deString("PESQUISADOR"));
        assertEquals(Cargo.LIDER_TECNICO, Cargo.deString("LIDER_TECNICO"));
        assertEquals(Cargo.DESIGNER, Cargo.deString("DESIGNER"));
        assertEquals(Cargo.AUXILIAR, Cargo.deString("AUXILIAR"));
    }

    @Test
    void deString_retornaNullParaNull() {
        assertNull(Cargo.deString(null));
    }

    @Test
    void deString_retornaNullParaStringInvalida() {
        assertNull(Cargo.deString("INVALIDO"));
    }

    @Test
    void deString_retornaNullParaStringVazia() {
        assertNull(Cargo.deString(""));
    }
}
