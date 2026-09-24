package dev.matheus.cadastroBolsistas.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private BolsistaRepository bolsistaRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LoginService service;

    @Test
    void rateLimiting_naoBloqueiaInicialmente() {
        assertFalse(service.isBloqueado("teste@exemplo.com"));
        assertEquals(5, service.getTentativasRestantes("teste@exemplo.com"));
    }

    @Test
    void rateLimiting_reduzTentativasAposFalha() {
        service.registrarFalha("teste@exemplo.com");
        assertEquals(4, service.getTentativasRestantes("teste@exemplo.com"));
        assertFalse(service.isBloqueado("teste@exemplo.com"));
    }

    @Test
    void rateLimiting_bloqueiaAposCincoFalhas() {
        String email = "ataque@exemplo.com";
        for (int i = 0; i < 5; i++) {
            service.registrarFalha(email);
        }

        assertTrue(service.isBloqueado(email));
        assertEquals(0, service.getTentativasRestantes(email));
        assertTrue(service.getSegundosRestantesBloqueio(email) > 0);
    }

    @Test
    void rateLimiting_sucessoResetaContadorEFalhas() {
        String email = "usuario@exemplo.com";
        service.registrarFalha(email);
        service.registrarFalha(email);
        assertEquals(3, service.getTentativasRestantes(email));

        service.registrarSucesso(email);
        assertFalse(service.isBloqueado(email));
        assertEquals(5, service.getTentativasRestantes(email));
    }
}
