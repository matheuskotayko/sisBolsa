package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.request.PerfilRequest;
import dev.matheus.cadastroBolsistas.dto.response.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock private AdministradorService administradorService;
    @Mock private ProfessorService professorService;
    @Mock private BolsistaService bolsistaService;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PerfilService service;

    private Usuario usuario(String tipo) {
        Usuario u = new Usuario(UUID.randomUUID(), "Nome", "n@teste.com", "hashAtual", true, tipo, null, null);
        return u;
    }

    private PerfilRequest semSenha() {
        return new PerfilRequest("Novo Nome", "novo@teste.com", null, null, null, null, null);
    }

    @Test
    void atualizar_bolsistaSemTrocarSenha_gravaDadosEMantemHash() {
        Usuario logado = usuario("BOLSISTA");
        Bolsista b = new Bolsista(logado.getId(), logado);
        when(bolsistaService.buscarOuFalhar(logado.getId())).thenReturn(b);

        UsuarioResponse r = service.atualizar(logado, semSenha());

        assertEquals("Novo Nome", logado.getNome());
        assertEquals("novo@teste.com", logado.getEmail());
        assertEquals("hashAtual", logado.getSenha());
        assertEquals("novo@teste.com", r.email());
        verify(bolsistaService).atualizar(b);
        verifyNoInteractions(passwordEncoder, professorService, administradorService);
    }

    @Test
    void atualizar_professor_persistePeloServicoDeProfessor() {
        Usuario logado = usuario("PROFESSOR");
        Professor p = new Professor(logado.getId(), logado);
        when(professorService.buscarOuFalhar(logado.getId())).thenReturn(p);

        service.atualizar(logado, semSenha());

        verify(professorService).atualizar(p);
        verify(bolsistaService, never()).atualizar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void atualizar_admin_persistePeloServicoDeAdministrador() {
        Usuario logado = usuario("ADMIN");
        Administrador a = new Administrador();
        a.setId(logado.getId());
        a.setUsuario(logado);
        when(administradorService.buscarPorId(logado.getId())).thenReturn(a);

        service.atualizar(logado, semSenha());

        verify(administradorService).atualizar(a);
        verify(professorService, never()).atualizar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void atualizar_comSenhaAtualCorreta_gravaNovoHash() {
        Usuario logado = usuario("BOLSISTA");
        Bolsista b = new Bolsista(logado.getId(), logado);
        when(bolsistaService.buscarOuFalhar(logado.getId())).thenReturn(b);
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha1")).thenReturn("hashNovo");

        service.atualizar(logado, new PerfilRequest("Nome", "n@teste.com", null, null, "senha123", "novaSenha1", "novaSenha1"));

        assertEquals("hashNovo", logado.getSenha());
        verify(bolsistaService).atualizar(b);
    }

    @Test
    void atualizar_comSenhaAtualErrada_lancaENaoSalva() {
        Usuario logado = usuario("BOLSISTA");
        when(passwordEncoder.matches("errada", "hashAtual")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                service.atualizar(logado, new PerfilRequest("Nome", "n@teste.com", null, null, "errada", "novaSenha1", "novaSenha1")));

        verifyNoInteractions(bolsistaService, professorService, administradorService);
    }
}
