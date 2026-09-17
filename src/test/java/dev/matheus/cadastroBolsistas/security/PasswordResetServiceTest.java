package dev.matheus.cadastroBolsistas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetServiceTest {

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService();
    }

    @Test
    void geraCodigoNumericoDeSeisDigitos() {
        String codigo = service.gerarCodigo("aluno@sisbolsa.com");
        assertNotNull(codigo);
        assertEquals(6, codigo.length());
        assertTrue(codigo.matches("\\d{6}"));
    }

    @Test
    void validaCodigoCorreto() {
        String email = "pesquisador@sisbolsa.com";
        String codigo = service.gerarCodigo(email);

        assertTrue(service.validarCodigo(email, codigo));
        assertTrue(service.validarCodigo(email.toUpperCase(), codigo));
    }

    @Test
    void rejeitaCodigoIncorreto() {
        String email = "admin@sisbolsa.com";
        service.gerarCodigo(email);

        assertFalse(service.validarCodigo(email, "000000"));
        assertFalse(service.validarCodigo("outro@email.com", "123456"));
    }

    @Test
    void invalidaCodigoAposUso() {
        String email = "usuario@sisbolsa.com";
        String codigo = service.gerarCodigo(email);
        assertTrue(service.validarCodigo(email, codigo));

        service.invalidarCodigo(email);
        assertFalse(service.validarCodigo(email, codigo));
    }
}
