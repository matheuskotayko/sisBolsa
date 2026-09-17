package dev.matheus.cadastroBolsistas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void naoBloqueiaInicialmente() {
        assertFalse(service.isBloqueado("teste@exemplo.com"));
        assertEquals(5, service.getTentativasRestantes("teste@exemplo.com"));
    }

    @Test
    void reduzTentativasAposFalha() {
        service.registrarFalha("teste@exemplo.com");
        assertEquals(4, service.getTentativasRestantes("teste@exemplo.com"));
        assertFalse(service.isBloqueado("teste@exemplo.com"));
    }

    @Test
    void bloqueiaAposCincoFalhas() {
        String email = "ataque@exemplo.com";
        for (int i = 0; i < 5; i++) {
            service.registrarFalha(email);
        }

        assertTrue(service.isBloqueado(email));
        assertEquals(0, service.getTentativasRestantes(email));
        assertTrue(service.getSegundosRestantesBloqueio(email) > 0);
    }

    @Test
    void sucessoResetaContadorEFalhas() {
        String email = "usuario@exemplo.com";
        service.registrarFalha(email);
        service.registrarFalha(email);
        assertEquals(3, service.getTentativasRestantes(email));

        service.registrarSucesso(email);
        assertFalse(service.isBloqueado(email));
        assertEquals(5, service.getTentativasRestantes(email));
    }
}
