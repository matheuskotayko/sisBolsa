package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

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

    @Test
    void buscarPorEmail_retornaUsuarioDoRepositorio() {
        Usuario u = new Usuario();
        u.setEmail("admin@sisbolsa.com");
        when(usuarioRepository.findByEmailAndAtivoTrue("admin@sisbolsa.com")).thenReturn(Optional.of(u));

        Usuario encontrado = service.buscarPorEmail("admin@sisbolsa.com");
        assertNotNull(encontrado);
        assertEquals("admin@sisbolsa.com", encontrado.getEmail());
    }

    @Test
    void autenticar_credenciaisCorretas_retornaUsuario() {
        Usuario u = new Usuario();
        u.setEmail("user@sisbolsa.com");
        u.setSenha("encoded_pass");
        when(usuarioRepository.findByEmailAndAtivoTrue("user@sisbolsa.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);

        Usuario autenticado = service.autenticar("user@sisbolsa.com", "123456");
        assertNotNull(autenticado);
        assertEquals("user@sisbolsa.com", autenticado.getEmail());
    }

    @Test
    void autenticar_senhaIncorreta_retornaNull() {
        Usuario u = new Usuario();
        u.setEmail("user@sisbolsa.com");
        u.setSenha("encoded_pass");
        when(usuarioRepository.findByEmailAndAtivoTrue("user@sisbolsa.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        Usuario autenticado = service.autenticar("user@sisbolsa.com", "wrong_pass");
        assertNull(autenticado);
    }
}
