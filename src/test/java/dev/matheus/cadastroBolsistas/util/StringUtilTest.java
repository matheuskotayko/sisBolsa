package dev.matheus.cadastroBolsistas.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void limpar_removeEspacosDoInicio() {
        assertEquals("abc", StringUtil.limpar("  abc"));
    }

    @Test
    void limpar_removeEspacosDoFim() {
        assertEquals("abc", StringUtil.limpar("abc  "));
    }

    @Test
    void limpar_removeEspacosDosDoisLados() {
        assertEquals("abc", StringUtil.limpar("  abc  "));
    }

    @Test
    void limpar_retornaVazioParaNull() {
        assertEquals("", StringUtil.limpar(null));
    }

    @Test
    void limpar_retornaVazioParaStringVazia() {
        assertEquals("", StringUtil.limpar(""));
    }

    @Test
    void limpar_naoAlteraStringLimpa() {
        assertEquals("abc", StringUtil.limpar("abc"));
    }

    @Test
    void estaVazio_trueParaNull() {
        assertTrue(StringUtil.estaVazio(null));
    }

    @Test
    void estaVazio_trueParaStringVazia() {
        assertTrue(StringUtil.estaVazio(""));
    }

    @Test
    void estaVazio_trueParaSoEspacos() {
        assertTrue(StringUtil.estaVazio("   "));
    }

    @Test
    void estaVazio_falseParaStringComConteudo() {
        assertFalse(StringUtil.estaVazio("a"));
    }

    @Test
    void estaVazio_falseParaStringComEspacosEConteudo() {
        assertFalse(StringUtil.estaVazio(" a "));
    }
}
