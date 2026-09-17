package dev.matheus.cadastroBolsistas.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void bolsistaComTipoAdmin_isAdminRetornaTrue() {
        Bolsista b = new Bolsista();
        b.setTipoUsuario("ADMIN");
        assertTrue(b.isAdmin());
    }

    @Test
    void bolsistaComTipoBolsista_isBolsistaRetornaTrue() {
        Bolsista b = new Bolsista();
        b.setTipoUsuario("BOLSISTA");
        assertTrue(b.isBolsista());
    }

    @Test
    void bolsistaComTipoBolsista_isAdminRetornaFalse() {
        Bolsista b = new Bolsista();
        b.setTipoUsuario("BOLSISTA");
        assertFalse(b.isAdmin());
    }

    @Test
    void professorComTipoProfessor_isProfessorRetornaTrue() {
        Professor p = new Professor();
        p.setTipoUsuario("PROFESSOR");
        assertTrue(p.isProfessor());
    }

    @Test
    void professorComTipoProfessor_isAdminRetornaFalse() {
        Professor p = new Professor();
        p.setTipoUsuario("PROFESSOR");
        assertFalse(p.isAdmin());
    }

    @Test
    void usuario_isAtivoRefleteCampo() {
        Bolsista b = new Bolsista();
        b.setAtivo(false);
        assertFalse(b.isAtivo());
        b.setAtivo(true);
        assertTrue(b.isAtivo());
    }
}
