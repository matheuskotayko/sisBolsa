package dev.matheus.cadastroBolsistas.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void usuarioComTipoAdmin_isAdminRetornaTrue() {
        Usuario u = new Usuario();
        u.setTipoUsuario("ADMIN");
        assertTrue(u.isAdmin());
        assertFalse(u.isBolsista());
        assertFalse(u.isProfessor());
    }

    @Test
    void usuarioComTipoBolsista_isBolsistaRetornaTrue() {
        Usuario u = new Usuario();
        u.setTipoUsuario("BOLSISTA");
        assertTrue(u.isBolsista());
        assertFalse(u.isAdmin());
        assertFalse(u.isProfessor());
    }

    @Test
    void usuarioComTipoProfessor_isProfessorRetornaTrue() {
        Usuario u = new Usuario();
        u.setTipoUsuario("PROFESSOR");
        assertTrue(u.isProfessor());
        assertFalse(u.isAdmin());
        assertFalse(u.isBolsista());
    }

    @Test
    void administradorEntity_isAdminRetornaTrue() {
        Administrador admin = new Administrador();
        assertTrue(admin.isAdmin());
        assertEquals("ADMIN", admin.getTipoUsuario());
    }

    @Test
    void bolsistaEntity_isBolsistaRetornaTrue() {
        Bolsista b = new Bolsista();
        assertTrue(b.isBolsista());
        assertEquals("BOLSISTA", b.getTipoUsuario());
    }

    @Test
    void professorEntity_isProfessorRetornaTrue() {
        Professor p = new Professor();
        assertTrue(p.isProfessor());
        assertEquals("PROFESSOR", p.getTipoUsuario());
    }

    @Test
    void usuario_isAtivoRefleteCampo() {
        Usuario u = new Usuario();
        u.setAtivo(false);
        assertFalse(u.isAtivo());
        u.setAtivo(true);
        assertTrue(u.isAtivo());
    }
}
