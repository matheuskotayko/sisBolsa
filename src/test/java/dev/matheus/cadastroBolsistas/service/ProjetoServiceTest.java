package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository repository;

    @Mock
    private LaboratorioService laboratorioService;

    @Mock
    private BolsistaRepository bolsistaRepository;

    @InjectMocks
    private ProjetoService projetoService;

    private Usuario admin() {
        Usuario p = new Usuario();
        p.setTipoUsuario("ADMIN");
        return p;
    }

    @Test
    void cadastrar_semPermissaoNoLaboratorio_lancaPermissaoNegadaSemSalvar() {
        Usuario logado = admin();
        Projeto p = new Projeto();
        UUID labId = UUID.randomUUID();
        p.setLaboratorioId(labId);
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(false);

        assertThrows(PermissaoNegadaException.class, () -> projetoService.cadastrar(p, logado));
        verify(repository, never()).save(any());
    }

    @Test
    void cadastrar_comPermissao_ativaESalva() {
        Usuario logado = admin();
        Projeto p = new Projeto();
        UUID labId = UUID.randomUUID();
        p.setLaboratorioId(labId);
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(true);

        assertTrue(projetoService.cadastrar(p, logado));
        assertTrue(p.isAtivo());
        verify(repository).save(p);
    }

    @Test
    void buscarOuFalhar_projetoInexistente_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> projetoService.buscarOuFalhar(id));
    }

    @Test
    void buscarOuFalhar_projetoDesativado_lancaRecursoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        Projeto p = new Projeto();
        p.setId(id);
        p.setAtivo(false);
        when(repository.findById(id)).thenReturn(Optional.of(p));

        assertThrows(RecursoNaoEncontradoException.class, () -> projetoService.buscarOuFalhar(id));
    }

    @Test
    void buscarExigindoGerencia_semPermissaoNoLabDoProjeto_lancaPermissaoNegada() {
        UUID id = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Projeto p = new Projeto();
        p.setId(id);
        p.setAtivo(true);
        p.setLaboratorioId(labId);
        when(repository.findById(id)).thenReturn(Optional.of(p));
        when(laboratorioService.podeGerenciar(any(), eq(labId))).thenReturn(false);

        assertThrows(PermissaoNegadaException.class, () -> projetoService.buscarExigindoGerencia(id, admin()));
    }

    private Projeto projetoGerenciavel() {
        Projeto p = new Projeto();
        p.setPublicId("prj_1");
        p.setAtivo(true);
        p.setLaboratorioId(UUID.randomUUID());
        when(repository.findByPublicIdAndAtivoTrue("prj_1")).thenReturn(Optional.of(p));
        when(laboratorioService.podeGerenciar(any(), eq(p.getLaboratorioId()))).thenReturn(true);
        return p;
    }

    @Test
    void excluir_semPermissao_lancaENaoDesativa() {
        Projeto p = new Projeto();
        p.setPublicId("prj_1");
        p.setAtivo(true);
        p.setLaboratorioId(UUID.randomUUID());
        when(repository.findByPublicIdAndAtivoTrue("prj_1")).thenReturn(Optional.of(p));
        when(laboratorioService.podeGerenciar(any(), eq(p.getLaboratorioId()))).thenReturn(false);

        assertThrows(PermissaoNegadaException.class, () -> projetoService.excluir("prj_1", admin()));
        assertTrue(p.isAtivo());
        verify(repository, never()).save(any());
    }

    @Test
    void excluir_comPermissao_desativaESalva() {
        Projeto p = projetoGerenciavel();

        projetoService.excluir("prj_1", admin());

        assertFalse(p.isAtivo());
        verify(repository).save(p);
    }

    @Test
    void vincularBolsista_bolsistaInexistente_lancaRecursoNaoEncontrado() {
        projetoGerenciavel();
        when(bolsistaRepository.findByPublicIdAndAtivoTrue("bol_x")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> projetoService.vincularBolsista("prj_1", "bol_x", admin()));
        verify(repository, never()).save(any());
    }

    @Test
    void vincularEDesvincular_alteramAEquipeDoProjeto() {
        Projeto p = projetoGerenciavel();
        Bolsista b = new Bolsista();
        b.setId(UUID.randomUUID());
        when(bolsistaRepository.findByPublicIdAndAtivoTrue("bol_1")).thenReturn(Optional.of(b));

        projetoService.vincularBolsista("prj_1", "bol_1", admin());
        assertTrue(p.getBolsistas().contains(b));

        projetoService.desvincularBolsista("prj_1", "bol_1", admin());
        assertFalse(p.getBolsistas().contains(b));
    }

    @Test
    void desvincularBolsista_quemNaoExiste_naoELancaErro() {
        projetoGerenciavel();
        when(bolsistaRepository.findByPublicIdAndAtivoTrue("bol_x")).thenReturn(Optional.empty());

        projetoService.desvincularBolsista("prj_1", "bol_x", admin());
        verify(repository).save(any(Projeto.class));
    }

    @Test
    void listarPorLaboratorio_laboratorioInexistente_lancaRecursoNaoEncontrado() {
        when(laboratorioService.buscarOuFalhar("lab_x")).thenThrow(new RecursoNaoEncontradoException("Laboratorio nao encontrado."));

        assertThrows(RecursoNaoEncontradoException.class, () -> projetoService.listarPorLaboratorio("lab_x"));
        verify(repository, never()).findByLaboratorio_PublicIdAndAtivoTrueOrderByNome(any());
    }

    @Test
    void listarMembros_devolveOsBolsistasDoProjeto() {
        Projeto p = new Projeto();
        p.setAtivo(true);
        when(repository.findByPublicIdAndAtivoTrue("prj_1")).thenReturn(Optional.of(p));
        Bolsista b = new Bolsista();
        when(bolsistaRepository.findByProjetosPublicIdAndAtivoTrueOrderByNome("prj_1")).thenReturn(java.util.List.of(b));

        assertEquals(java.util.List.of(b), projetoService.listarMembros("prj_1"));
    }

    @Test
    void listarMembros_projetoInexistente_lancaRecursoNaoEncontrado() {
        when(repository.findByPublicIdAndAtivoTrue("prj_x")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> projetoService.listarMembros("prj_x"));
    }

    @Test
    void buscarExigindoGerencia_comPermissao_retornaOProjeto() {
        UUID id = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Projeto p = new Projeto();
        p.setId(id);
        p.setAtivo(true);
        p.setLaboratorioId(labId);
        when(repository.findById(id)).thenReturn(Optional.of(p));
        when(laboratorioService.podeGerenciar(any(), eq(labId))).thenReturn(true);

        assertSame(p, projetoService.buscarExigindoGerencia(id, admin()));
    }

    @Test
    void exigirPodeMoverPara_semPermissaoNoLabDestino_lancaPermissaoNegada() {
        UUID novoLabId = UUID.randomUUID();
        Usuario logado = admin();
        when(laboratorioService.podeGerenciar(logado, novoLabId)).thenReturn(false);

        assertThrows(PermissaoNegadaException.class, () -> projetoService.exigirPodeMoverPara(logado, novoLabId));
    }

    @Test
    void exigirPodeMoverPara_comPermissao_naoLancaNada() {
        UUID novoLabId = UUID.randomUUID();
        Usuario logado = admin();
        when(laboratorioService.podeGerenciar(logado, novoLabId)).thenReturn(true);

        assertDoesNotThrow(() -> projetoService.exigirPodeMoverPara(logado, novoLabId));
    }
}
