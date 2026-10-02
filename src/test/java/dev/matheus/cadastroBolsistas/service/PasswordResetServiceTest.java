package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordResetService(usuarioRepository, passwordEncoder);
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

    @Test
    void redefinirSenha_comDadosValidos_atualizaSenhaEInvalidaCodigo() {
        String email = "usuario@sisbolsa.com";
        String codigo = service.gerarCodigo(email);

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha("senhaAntigaHash");

        when(usuarioRepository.findByEmailAndAtivoTrue(email)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("novaSenhaHash");

        service.redefinirSenha(email, codigo, "novaSenha123", "novaSenha123");

        assertEquals("novaSenhaHash", usuario.getSenha());
        verify(usuarioRepository).save(usuario);
        assertFalse(service.validarCodigo(email, codigo));
    }

    @Test
    void redefinirSenha_comCodigoInvalido_lancaExcecao() {
        String email = "usuario@sisbolsa.com";
        service.gerarCodigo(email);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.redefinirSenha(email, "999999", "novaSenha123", "novaSenha123"));

        assertTrue(ex.getMessage().contains("Código de verificação inválido"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void redefinirSenha_comConfirmacaoDiferente_lancaExcecao() {
        String email = "usuario@sisbolsa.com";
        String codigo = service.gerarCodigo(email);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.redefinirSenha(email, codigo, "novaSenha123", "outraSenha"));

        assertTrue(ex.getMessage().contains("não conferem"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void redefinirSenha_quandoUsuarioNaoExiste_lancaRecursoNaoEncontrado() {
        String email = "naoexiste@sisbolsa.com";
        String codigo = service.gerarCodigo(email);

        when(usuarioRepository.findByEmailAndAtivoTrue(email)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () ->
                service.redefinirSenha(email, codigo, "novaSenha123", "novaSenha123"));

        verify(usuarioRepository, never()).save(any());
    }
}
