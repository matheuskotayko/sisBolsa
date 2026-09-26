package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.BolsistaRequest;
import dev.matheus.cadastroBolsistas.dto.PerfilRequest;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.matheus.cadastroBolsistas.exceptions.LimiteAdminsAtingidoException;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BolsistaServiceTest {

    @Mock
    private BolsistaRepository repository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private LaboratorioService laboratorioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BolsistaService bolsistaService;

    private static BolsistaRequest bolsistaRequest(String laboratorioId) {
        return new BolsistaRequest("Lucas", "lucas@teste.com", "senha123", null, "Engenharia",
                "2024001", null, null, laboratorioId, "BOLSISTA", null, null, null, null, null, null, null);
    }

    @Test
    void podeGerenciar_adminSempreRetornaTrue() throws SQLException {
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setTipoUsuario("ADMIN");

        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());

        assertTrue(bolsistaService.podeGerenciar(admin, bolsista));
        verifyNoInteractions(laboratorioRepository);
    }

    @Test
    void podeGerenciar_professorCoordenadorDaboLaboratorio() throws SQLException {
        UUID profId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        Usuario professor = new Usuario();
        professor.setId(profId);
        professor.setTipoUsuario("PROFESSOR");

        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setLaboratorioId(labId);

        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCoordenadorId(profId);

        when(laboratorioRepository.findById(labId)).thenReturn(Optional.of(lab));

        assertTrue(bolsistaService.podeGerenciar(professor, bolsista));
        verify(laboratorioRepository).findById(labId);
    }

    @Test
    void podeGerenciar_professorDeOutroLaboratorio() throws SQLException {
        UUID profId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        Usuario professor = new Usuario();
        professor.setId(profId);
        professor.setTipoUsuario("PROFESSOR");

        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setLaboratorioId(labId);

        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setCoordenadorId(UUID.randomUUID());

        when(laboratorioRepository.findById(labId)).thenReturn(Optional.of(lab));

        assertFalse(bolsistaService.podeGerenciar(professor, bolsista));
        verify(laboratorioRepository).findById(labId);
    }

    @Test
    void podeGerenciar_professorEBolsistaSemLaboratorio() throws SQLException {
        Usuario professor = new Usuario();
        professor.setId(UUID.randomUUID());
        professor.setTipoUsuario("PROFESSOR");

        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setLaboratorioId(null);

        assertFalse(bolsistaService.podeGerenciar(professor, bolsista));
        verifyNoInteractions(laboratorioRepository);
    }

    @Test
    void podeGerenciar_bolsistaRetornaFalse() throws SQLException {
        Usuario bolsistaLogado = new Usuario();
        bolsistaLogado.setId(UUID.randomUUID());
        bolsistaLogado.setTipoUsuario("BOLSISTA");

        Bolsista bolsistaAlvo = new Bolsista();
        bolsistaAlvo.setId(UUID.randomUUID());

        assertFalse(bolsistaService.podeGerenciar(bolsistaLogado, bolsistaAlvo));
        verifyNoInteractions(laboratorioRepository);
    }

    @Test
    void podeGerenciar_usuarioNullRetornaFalse() throws SQLException {
        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());

        assertFalse(bolsistaService.podeGerenciar(null, bolsista));
        verifyNoInteractions(laboratorioRepository);
    }

    @Test
    void podeGerenciar_bolsistaAlvoNullRetornaFalse() throws SQLException {
        Usuario professor = new Usuario();
        professor.setId(UUID.randomUUID());
        professor.setTipoUsuario("PROFESSOR");

        assertFalse(bolsistaService.podeGerenciar(professor, null));
        verifyNoInteractions(laboratorioRepository);
    }

    @Test
    void buscarComPermissaoDeVisualizacao_naoEncontrado_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Usuario admin = new Usuario();
        admin.setTipoUsuario("ADMIN");

        assertThrows(RecursoNaoEncontradoException.class,
                () -> bolsistaService.buscarComPermissaoDeVisualizacao(id, admin));
    }

    @Test
    void buscarComPermissaoDeVisualizacao_donoDoProprioCadastro_dispensaPodeGerenciar() {
        UUID id = UUID.randomUUID();
        Bolsista alvo = new Bolsista();
        alvo.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(alvo));

        Usuario logado = new Usuario();
        logado.setId(id);
        logado.setTipoUsuario("BOLSISTA");

        assertSame(alvo, bolsistaService.buscarComPermissaoDeVisualizacao(id, logado));
        verifyNoInteractions(laboratorioRepository);
    }

    @Test
    void buscarComPermissaoDeVisualizacao_semPermissao_lancaPermissaoNegada() {
        UUID id = UUID.randomUUID();
        Bolsista alvo = new Bolsista();
        alvo.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(alvo));

        Usuario outroBolsista = new Usuario();
        outroBolsista.setId(UUID.randomUUID());
        outroBolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class,
                () -> bolsistaService.buscarComPermissaoDeVisualizacao(id, outroBolsista));
    }

    @Test
    void buscarComPermissaoDeEdicao_donoDoProprioCadastro_dispensaPodeGerenciar() {
        UUID id = UUID.randomUUID();
        Bolsista alvo = new Bolsista();
        alvo.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(alvo));

        Usuario logado = new Usuario();
        logado.setId(id);
        logado.setTipoUsuario("BOLSISTA");

        assertSame(alvo, bolsistaService.buscarComPermissaoDeEdicao(id, logado));
    }

    @Test
    void buscarComPermissaoDeEdicao_semPermissao_lancaPermissaoNegada() {
        UUID id = UUID.randomUUID();
        Bolsista alvo = new Bolsista();
        alvo.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(alvo));

        Usuario outroBolsista = new Usuario();
        outroBolsista.setId(UUID.randomUUID());
        outroBolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class,
                () -> bolsistaService.buscarComPermissaoDeEdicao(id, outroBolsista));
    }

    @Test
    void buscarComPermissaoDeExclusao_donoDoProprioCadastro_naoBastaSerDono() {
        UUID id = UUID.randomUUID();
        Bolsista alvo = new Bolsista();
        alvo.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(alvo));

        Usuario logado = new Usuario();
        logado.setId(id);
        logado.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class,
                () -> bolsistaService.buscarComPermissaoDeExclusao(id, logado));
    }

    @Test
    void buscarComPermissaoDeExclusao_quemGerencia_retornaOAlvo() {
        UUID id = UUID.randomUUID();
        Bolsista alvo = new Bolsista();
        alvo.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(alvo));

        Usuario admin = new Usuario();
        admin.setTipoUsuario("ADMIN");

        assertSame(alvo, bolsistaService.buscarComPermissaoDeExclusao(id, admin));
    }

    @Test
    void exigirPodeCadastrarUsuario_bolsista_lancaPermissaoNegada() {
        Usuario bolsista = new Usuario();
        bolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> bolsistaService.exigirPodeCadastrarUsuario(bolsista));
    }

    @Test
    void exigirPodeCadastrarUsuario_professor_naoLancaNada() {
        Usuario professor = new Usuario();
        professor.setTipoUsuario("PROFESSOR");

        assertDoesNotThrow(() -> bolsistaService.exigirPodeCadastrarUsuario(professor));
    }

    @Test
    void exigirPodeExportarUsuarios_bolsista_lancaPermissaoNegada() {
        Usuario bolsista = new Usuario();
        bolsista.setTipoUsuario("BOLSISTA");

        assertThrows(PermissaoNegadaException.class, () -> bolsistaService.exigirPodeExportarUsuarios(bolsista));
    }

    @Test
    void inserir_setaAtivoTrueAntesDeSalvar() throws SQLException {
        Bolsista bolsista = new Bolsista();
        bolsista.setAtivo(false);

        boolean result = bolsistaService.inserir(bolsista);

        assertTrue(result);
        ArgumentCaptor<Bolsista> captor = ArgumentCaptor.forClass(Bolsista.class);
        verify(repository).save(captor.capture());
        
        Bolsista capturado = captor.getValue();
        assertTrue(capturado.isAtivo());
    }

    @Test
    void validarSenha_exigindoESemSenha_lanca() {
        assertThrows(IllegalArgumentException.class, () -> bolsistaService.validarSenha("", true));
    }

    @Test
    void validarSenha_exigindoESenhaCurta_lanca() {
        assertThrows(IllegalArgumentException.class, () -> bolsistaService.validarSenha("123", true));
    }

    @Test
    void validarSenha_naoExigindoESemSenha_naoLanca() {
        assertDoesNotThrow(() -> bolsistaService.validarSenha("", false));
    }

    @Test
    void validarSenha_naoExigindoMasSenhaCurtaInformada_lanca() {
        assertThrows(IllegalArgumentException.class, () -> bolsistaService.validarSenha("123", false));
    }

    @Test
    void calcularNovaSenha_semCamposDeSenha_retornaNull() {
        Usuario logado = new Usuario();
        logado.setSenha("hashAtual");

        assertNull(bolsistaService.calcularNovaSenha(logado, "", "", ""));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void calcularNovaSenha_senhaAtualErrada_lanca() {
        Usuario logado = new Usuario();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("errada", "hashAtual")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> bolsistaService.calcularNovaSenha(logado, "errada", "novaSenha1", "novaSenha1"));
    }

    @Test
    void calcularNovaSenha_confirmacaoDiferente_lanca() {
        Usuario logado = new Usuario();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> bolsistaService.calcularNovaSenha(logado, "senha123", "novaSenha1", "outraCoisa"));
    }

    @Test
    void calcularNovaSenha_novaSenhaCurta_lanca() {
        Usuario logado = new Usuario();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> bolsistaService.calcularNovaSenha(logado, "senha123", "123", "123"));
    }

    @Test
    void calcularNovaSenha_valida_retornaHashCodificado() {
        Usuario logado = new Usuario();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha1")).thenReturn("hashNovo");

        assertEquals("hashNovo", bolsistaService.calcularNovaSenha(logado, "senha123", "novaSenha1", "novaSenha1"));
    }

    @Test
    void aplicarDadosPerfil_semTrocarSenha_mantemHashAtual() {
        Usuario u = new Usuario();
        u.setSenha("hashAtual");
        PerfilRequest body = new PerfilRequest("Novo Nome", "novo@teste.com", null, null, null, null, null);

        bolsistaService.aplicarDadosPerfil(u, body, null);

        assertEquals("Novo Nome", u.getNome());
        assertEquals("novo@teste.com", u.getEmail());
        assertEquals("hashAtual", u.getSenha());
    }

    @Test
    void aplicarCamposDeBolsista_semPermissaoNoLaboratorio_lanca() {
        String labPublicId = "lab_test12345678901234";
        UUID labId = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setPublicId(labPublicId);
        when(laboratorioRepository.findByPublicIdAndAtivoTrue(labPublicId)).thenReturn(Optional.of(lab));

        Usuario logado = new Usuario();
        logado.setId(UUID.randomUUID());
        logado.setTipoUsuario("PROFESSOR");
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(false);

        Bolsista b = new Bolsista();
        BolsistaRequest body = bolsistaRequest(labPublicId);

        assertThrows(PermissaoNegadaException.class, () -> bolsistaService.aplicarCamposDeBolsista(b, body, logado));
    }

    @Test
    void aplicarCamposDeBolsista_comPermissao_aplicaCampos() {
        String labPublicId = "lab_test12345678901234";
        UUID labId = UUID.randomUUID();
        Laboratorio lab = new Laboratorio();
        lab.setId(labId);
        lab.setPublicId(labPublicId);
        when(laboratorioRepository.findByPublicIdAndAtivoTrue(labPublicId)).thenReturn(Optional.of(lab));

        Usuario logado = new Usuario();
        logado.setId(UUID.randomUUID());
        logado.setTipoUsuario("PROFESSOR");
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(true);

        Bolsista b = new Bolsista();
        BolsistaRequest body = bolsistaRequest(labPublicId);

        bolsistaService.aplicarCamposDeBolsista(b, body, logado);

        assertEquals(labId, b.getLaboratorioId());
        assertEquals("Engenharia", b.getCurso());
        assertEquals("BOLSISTA", b.getTipoUsuario());
    }

    @Test
    void filtrarPorTipo_semFiltro_devolveListaIntacta() {
        ArrayList<Bolsista> lista = new ArrayList<>(List.of(new Bolsista(), new Bolsista()));
        assertEquals(2, bolsistaService.filtrarPorTipo(lista, "").size());
    }

    @Test
    void filtrarPorTipo_comFiltro_removeQuemNaoBate() {
        Bolsista b1 = new Bolsista();
        b1.setTipoUsuario("BOLSISTA");
        Bolsista b2 = new Bolsista();
        b2.setTipoUsuario("OUTRO");
        ArrayList<Bolsista> lista = new ArrayList<>(List.of(b1, b2));

        ArrayList<Bolsista> filtrado = bolsistaService.filtrarPorTipo(lista, "bolsista");

        assertEquals(1, filtrado.size());
        assertSame(b1, filtrado.get(0));
    }

    @Test
    void idsDosBolsistasCoordenadosPor_combinaLaboratoriosDoProfessor() {
        UUID profId = UUID.randomUUID();
        Laboratorio lab1 = new Laboratorio();
        lab1.setId(UUID.randomUUID());
        Laboratorio lab2 = new Laboratorio();
        lab2.setId(UUID.randomUUID());
        when(laboratorioService.listarPorCoordenador(profId)).thenReturn(new ArrayList<>(List.of(lab1, lab2)));

        Bolsista b1 = new Bolsista();
        b1.setId(UUID.randomUUID());
        Bolsista b2 = new Bolsista();
        b2.setId(UUID.randomUUID());
        when(repository.findByLaboratorioIdAndAtivoTrueOrderByNome(lab1.getId())).thenReturn(List.of(b1));
        when(repository.findByLaboratorioIdAndAtivoTrueOrderByNome(lab2.getId())).thenReturn(List.of(b2));

        List<UUID> ids = bolsistaService.idsDosBolsistasCoordenadosPor(profId);

        assertEquals(List.of(b1.getId(), b2.getId()), ids);
    }
}
