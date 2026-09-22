package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ProjetoRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/*
 * regras de negocio de projetos com IDs em UUID.
 */
@Service
public class ProjetoService {

    @Autowired
    private ProjetoRepository repository;

    @Autowired
    private LaboratorioService laboratorioService;

    @Autowired
    private BolsistaRepository bolsistaRepository;

    public boolean cadastrar(Projeto p, Usuario logado) {
        if (!laboratorioService.podeGerenciar(logado, p.getLaboratorioId())) {
            throw new PermissaoNegadaException("Sem permissao para criar projeto neste laboratorio.");
        }
        p.setAtivo(true);
        repository.save(p);
        return true;
    }

    /* lookup + 404 (incluindo desativado) num so lugar, pra nenhum controller precisar checar null na mao. */
    public Projeto buscarOuFalhar(UUID id) {
        Projeto p = buscarPorId(id);
        if (p == null || !p.isAtivo()) {
            throw new RecursoNaoEncontradoException("Projeto nao encontrado.");
        }
        return p;
    }

    /* so quem gerencia o laboratorio do projeto pode editar/excluir/(des)vincular membros. */
    public Projeto buscarExigindoGerencia(UUID id, Usuario logado) {
        Projeto p = buscarOuFalhar(id);
        if (!laboratorioService.podeGerenciar(logado, p.getLaboratorioId())) {
            throw new PermissaoNegadaException("Sem permissao para gerenciar projetos deste laboratorio.");
        }
        return p;
    }

    public void exigirPodeMoverPara(Usuario logado, UUID novoLaboratorioId) {
        if (!laboratorioService.podeGerenciar(logado, novoLaboratorioId)) {
            throw new PermissaoNegadaException("Sem permissao para mover o projeto para este laboratorio.");
        }
    }

    public ArrayList<Projeto> listarTodos() {
        return buscarProjetos(null, null);
    }

    public ArrayList<Projeto> buscarProjetos(String buscaNome, UUID labId) {
        String nome = buscaNome != null ? buscaNome.trim() : "";
        return new ArrayList<>(repository.findAll(Filtros.projeto(nome, labId), Sort.by("nome")));
    }

    public ArrayList<Projeto> listarPorLaboratorio(UUID labId) {
        if (labId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByLaboratorioIdAndAtivoTrueOrderByNome(labId));
    }

    public Projeto buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    public int contarMembros(UUID projetoId) {
        if (projetoId == null) return 0;
        return bolsistaRepository.countByProjetos_IdAndAtivoTrue(projetoId);
    }

    public boolean atualizar(Projeto p) {
        repository.save(p);
        return true;
    }

    public void aplicar(Projeto p, ProjetoRequest body) {
        p.setNome(StringUtil.limpar(body.nome()));
        p.setDescricao(body.descricao());
        p.setLaboratorioId(body.laboratorioId());
        p.setLinkRepositorio(StringUtil.limpar(body.linkRepositorio()));
        p.setLinkDocumentacao(StringUtil.limpar(body.linkDocumentacao()));
    }

    /* soft delete: carrega, marca ativo = false e deixa o JPA fazer o UPDATE. */
    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.findById(id).map(p -> {
            p.setAtivo(false);
            repository.save(p);
            return true;
        }).orElse(false);
    }

    /*
     * vinculo e desvinculo sao so alteracoes na colecao do lado dono; o Hibernate
     * traduz em INSERT/DELETE na bolsista_projeto no fim da transacao. o Set ja
     * torna o vinculo idempotente, no lugar do antigo ON CONFLICT DO NOTHING.
     */
    @Transactional
    public boolean vincularBolsista(UUID bolsistaId, UUID projetoId) {
        return alterarVinculo(bolsistaId, projetoId, true);
    }

    @Transactional
    public boolean desvincularBolsista(UUID bolsistaId, UUID projetoId) {
        return alterarVinculo(bolsistaId, projetoId, false);
    }

    private boolean alterarVinculo(UUID bolsistaId, UUID projetoId, boolean vincular) {
        if (bolsistaId == null || projetoId == null) return false;
        Projeto projeto = repository.findById(projetoId).orElse(null);
        Bolsista bolsista = bolsistaRepository.findById(bolsistaId).orElse(null);
        if (projeto == null || bolsista == null) return false;
        if (vincular) {
            projeto.getBolsistas().add(bolsista);
        } else {
            projeto.getBolsistas().remove(bolsista);
        }
        repository.save(projeto);
        return true;
    }

    public ArrayList<Projeto> listarPorBolsista(UUID bolsistaId) {
        if (bolsistaId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsistas_IdAndAtivoTrueOrderByNome(bolsistaId));
    }
}
