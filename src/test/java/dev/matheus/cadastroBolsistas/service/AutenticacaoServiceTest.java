package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AutenticacaoService service;

    @Test
    void loadUserByUsername_devolveUserDetailsComPapel() {
        Usuario u = new Usuario();
        u.setEmail("a@a.com");
        u.setSenha("hash");
        u.setAtivo(true);
        u.setTipoUsuario("PROFESSOR");
        when(usuarioRepository.findByEmailAndAtivoTrue("a@a.com")).thenReturn(Optional.of(u));

        UserDetails d = service.loadUserByUsername("a@a.com");

        assertEquals("a@a.com", d.getUsername());
        assertEquals("hash", d.getPassword());
        assertTrue(d.isEnabled());
        assertEquals("ROLE_PROFESSOR", d.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_inexistente_lancaUsernameNotFound() {
        when(usuarioRepository.findByEmailAndAtivoTrue("x@x.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("x@x.com"));
    }
}
