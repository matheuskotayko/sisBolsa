package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.AdministradorRequest;
import dev.matheus.cadastroBolsistas.dto.AdministradorResponse;
import dev.matheus.cadastroBolsistas.exceptions.LimiteAdminsAtingidoException;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.AdministradorRepository;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministradorServiceTest {

    @Mock
    private AdministradorRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdministradorService service;

    @Test
    void podeCriarAdmin_quandoMenosDe3_retornaTrue() {
        when(repository.countAtivos()).thenReturn(2);
        assertTrue(service.podeCriarAdmin());
    }

    @Test
    void podeCriarAdmin_quando3OuMais_retornaFalse() {
        when(repository.countAtivos()).thenReturn(3);
        assertFalse(service.podeCriarAdmin());
    }

    @Test
    void exigirPodeCriarAdmin_quandoNaoAdmin_lancaPermissaoNegada() {
        Usuario bolsista = new Usuario();
        bolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> service.exigirPodeCriarAdmin(bolsista));
    }

    @Test
    void exigirPodeCriarAdmin_quandoLimiteAtingido_lancaLimiteAdminsAtingido() {
        Usuario admin = new Usuario();
        admin.setTipoUsuario("ADMIN");
        when(repository.countAtivos()).thenReturn(3);

        assertThrows(LimiteAdminsAtingidoException.class, () -> service.exigirPodeCriarAdmin(admin));
    }

    @Test
    void criarAdminAutocadastro_quandoEmailJaExiste_lancaConflito() {
        when(repository.countAtivos()).thenReturn(1);
        when(usuarioRepository.existsByEmail("admin@teste.com")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> service.criarAdminAutocadastro("Admin", "admin@teste.com", "hash"));
    }

    @Test
    void desativar_quandoUnicoAdmin_lancaIllegalStateException() {
        UUID id = UUID.randomUUID();
        Usuario logado = new Usuario();
        logado.setTipoUsuario("ADMIN");

        Administrador admin = new Administrador();
        admin.setId(id);
        admin.setAtivo(true);

        when(repository.findById(id)).thenReturn(Optional.of(admin));
        when(repository.countAtivos()).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.desativar(id, logado));
    }

    @Test
    void desativar_quandoUnicoAdmin_porPublicId_lancaIllegalStateException() {
        String publicId = "adm_12345678901234567890";
        Usuario logado = new Usuario();
        logado.setTipoUsuario("ADMIN");

        Administrador admin = new Administrador();
        admin.setPublicId(publicId);
        admin.setAtivo(true);

        when(repository.findByPublicIdAndAtivoTrue(publicId)).thenReturn(Optional.of(admin));
        when(repository.countAtivos()).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.desativar(publicId, logado));
    }
}
