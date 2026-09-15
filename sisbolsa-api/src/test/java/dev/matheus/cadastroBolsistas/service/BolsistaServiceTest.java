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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

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

    private static BolsistaRequest bolsistaRequest(UUID laboratorioId) {
        return new BolsistaRequest("Lucas", "lucas@teste.com", "senha123", null, "Engenharia",
                "2024001", null, null, laboratorioId, "BOLSISTA", null, null, null, null, null, null, null);
    }

    @Test
    void podeGerenciar_adminSempreRetornaTrue() throws SQLException {
        Professor admin = new Professor();
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

        Professor professor = new Professor();
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

        Professor professor = new Professor();
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
        Professor professor = new Professor();
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
        Bolsista bolsistaLogado = new Bolsista();
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
        Professor professor = new Professor();
        professor.setId(UUID.randomUUID());
        professor.setTipoUsuario("PROFESSOR");

        assertFalse(bolsistaService.podeGerenciar(professor, null));
        verifyNoInteractions(laboratorioRepository);
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
    void podeCriarAdmin_abaixoDoLimite_true() {
        when(repository.countByTipoUsuarioAndAtivoTrue("ADMIN")).thenReturn(2);
        assertTrue(bolsistaService.podeCriarAdmin());
    }

    @Test
    void podeCriarAdmin_noLimite_false() {
        when(repository.countByTipoUsuarioAndAtivoTrue("ADMIN")).thenReturn(3);
        assertFalse(bolsistaService.podeCriarAdmin());
    }

    @Test
    void criarAdmin_montaBolsistaComDadosFixos() {
        Bolsista admin = bolsistaService.criarAdmin("Ana Admin", "ana@teste.com", "hashPronto");

        assertEquals("Ana Admin", admin.getNome());
        assertEquals("ana@teste.com", admin.getEmail());
        assertEquals("hashPronto", admin.getSenha());
        assertEquals("ADMIN", admin.getTipoUsuario());
        assertTrue(admin.isAtivo());
        verify(repository).save(admin);
    }

    @Test
    void calcularNovaSenha_semCamposDeSenha_retornaNull() {
        Bolsista logado = new Bolsista();
        logado.setSenha("hashAtual");

        assertNull(bolsistaService.calcularNovaSenha(logado, "", "", ""));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void calcularNovaSenha_senhaAtualErrada_lanca() {
        Bolsista logado = new Bolsista();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("errada", "hashAtual")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> bolsistaService.calcularNovaSenha(logado, "errada", "novaSenha1", "novaSenha1"));
    }

    @Test
    void calcularNovaSenha_confirmacaoDiferente_lanca() {
        Bolsista logado = new Bolsista();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> bolsistaService.calcularNovaSenha(logado, "senha123", "novaSenha1", "outraCoisa"));
    }

    @Test
    void calcularNovaSenha_novaSenhaCurta_lanca() {
        Bolsista logado = new Bolsista();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> bolsistaService.calcularNovaSenha(logado, "senha123", "123", "123"));
    }

    @Test
    void calcularNovaSenha_valida_retornaHashCodificado() {
        Bolsista logado = new Bolsista();
        logado.setSenha("hashAtual");
        when(passwordEncoder.matches("senha123", "hashAtual")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha1")).thenReturn("hashNovo");

        assertEquals("hashNovo", bolsistaService.calcularNovaSenha(logado, "senha123", "novaSenha1", "novaSenha1"));
    }

    @Test
    void aplicarDadosPerfil_semTrocarSenha_mantemHashAtual() {
        Bolsista u = new Bolsista();
        u.setSenha("hashAtual");
        PerfilRequest body = new PerfilRequest("Novo Nome", "novo@teste.com", null, null, null, null, null);

        bolsistaService.aplicarDadosPerfil(u, body, null);

        assertEquals("Novo Nome", u.getNome());
        assertEquals("novo@teste.com", u.getEmail());
        assertEquals("hashAtual", u.getSenha());
    }

    @Test
    void aplicarCamposDeBolsista_semPermissaoNoLaboratorio_lanca() {
        UUID labId = UUID.randomUUID();
        Usuario logado = new Professor();
        logado.setId(UUID.randomUUID());
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(false);

        Bolsista b = new Bolsista();
        BolsistaRequest body = bolsistaRequest(labId);

        assertThrows(ResponseStatusException.class, () -> bolsistaService.aplicarCamposDeBolsista(b, body, logado));
    }

    @Test
    void aplicarCamposDeBolsista_comPermissao_aplicaCampos() {
        UUID labId = UUID.randomUUID();
        Usuario logado = new Professor();
        logado.setId(UUID.randomUUID());
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(true);

        Bolsista b = new Bolsista();
        BolsistaRequest body = bolsistaRequest(labId);

        bolsistaService.aplicarCamposDeBolsista(b, body, logado);

        assertEquals(labId, b.getLaboratorioId());
        assertEquals("Engenharia", b.getCurso());
        assertEquals("BOLSISTA", b.getTipoUsuario());
    }

    @Test
    void filtrarPorTipo_semFiltro_devolveListaIntacta() {
        ArrayList<Usuario> lista = new ArrayList<>(List.of(new Bolsista(), new Professor()));
        assertEquals(2, bolsistaService.filtrarPorTipo(lista, "").size());
    }

    @Test
    void filtrarPorTipo_comFiltro_removeQuemNaoBate() {
        Bolsista bolsista = new Bolsista();
        bolsista.setTipoUsuario("BOLSISTA");
        Professor professor = new Professor();
        professor.setTipoUsuario("PROFESSOR");
        ArrayList<Usuario> lista = new ArrayList<>(List.of(bolsista, professor));

        ArrayList<Usuario> filtrado = bolsistaService.filtrarPorTipo(lista, "professor");

        assertEquals(1, filtrado.size());
        assertSame(professor, filtrado.get(0));
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
        when(repository.buscarPorLaboratorio(lab1.getId())).thenReturn(List.of(b1));
        when(repository.buscarPorLaboratorio(lab2.getId())).thenReturn(List.of(b2));

        List<UUID> ids = bolsistaService.idsDosBolsistasCoordenadosPor(profId);

        assertEquals(List.of(b1.getId(), b2.getId()), ids);
    }
}
