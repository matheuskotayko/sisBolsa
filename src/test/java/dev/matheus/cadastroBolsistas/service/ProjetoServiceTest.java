package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Projeto;
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

    @InjectMocks
    private ProjetoService projetoService;

    private Professor admin() {
        Professor p = new Professor();
        p.setTipoUsuario("ADMIN");
        return p;
    }

    @Test
    void cadastrar_semPermissaoNoLaboratorio_lancaPermissaoNegadaSemSalvar() {
        Professor logado = admin();
        Projeto p = new Projeto();
        UUID labId = UUID.randomUUID();
        p.setLaboratorioId(labId);
        when(laboratorioService.podeGerenciar(logado, labId)).thenReturn(false);

        assertThrows(PermissaoNegadaException.class, () -> projetoService.cadastrar(p, logado));
        verify(repository, never()).save(any());
    }

    @Test
    void cadastrar_comPermissao_ativaESalva() {
        Professor logado = admin();
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
        Professor logado = admin();
        when(laboratorioService.podeGerenciar(logado, novoLabId)).thenReturn(false);

        assertThrows(PermissaoNegadaException.class, () -> projetoService.exigirPodeMoverPara(logado, novoLabId));
    }

    @Test
    void exigirPodeMoverPara_comPermissao_naoLancaNada() {
        UUID novoLabId = UUID.randomUUID();
        Professor logado = admin();
        when(laboratorioService.podeGerenciar(logado, novoLabId)).thenReturn(true);

        assertDoesNotThrow(() -> projetoService.exigirPodeMoverPara(logado, novoLabId));
    }
}
