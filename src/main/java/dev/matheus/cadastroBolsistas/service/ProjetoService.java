package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ProjetoRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return new ArrayList<>(repository.buscarProjetos(nome, labId));
    }

    public ArrayList<Projeto> listarPorLaboratorio(UUID labId) {
        if (labId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorLaboratorio(labId));
    }

    public Projeto buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    public int contarMembros(UUID projetoId) {
        if (projetoId == null) return 0;
        return repository.contarMembros(projetoId);
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

    /* soft delete */
    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.desativar(id) > 0;
    }

    @Transactional
    public boolean vincularBolsista(UUID bolsistaId, UUID projetoId) {
        if (bolsistaId == null || projetoId == null) return false;
        repository.vincularBolsista(bolsistaId, projetoId);
        return true;
    }

    @Transactional
    public boolean desvincularBolsista(UUID bolsistaId, UUID projetoId) {
        if (bolsistaId == null || projetoId == null) return false;
        repository.desvincularBolsista(bolsistaId, projetoId);
        return true;
    }

    @Transactional
    public boolean desvincularBolsistaDeTodosProjetos(UUID bolsistaId) {
        if (bolsistaId == null) return false;
        repository.desvincularBolsistaDeTodosProjetos(bolsistaId);
        return true;
    }

    public ArrayList<Projeto> listarPorBolsista(UUID bolsistaId) {
        if (bolsistaId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorBolsista(bolsistaId));
    }

    public Map<UUID, ArrayList<Projeto>> getProjetosDosBolsistasDoLaboratorio(UUID labId) {
        if (labId == null) return new HashMap<>();
        List<Object[]> vinculos = repository.buscarVinculosDoLaboratorio(labId);
        if (vinculos.isEmpty()) {
            return new HashMap<>();
        }

        List<UUID> projetoIds = vinculos.stream()
                .map(v -> (UUID) v[1])
                .distinct()
                .toList();

        Map<UUID, Projeto> porId = new HashMap<>();
        for (Projeto p : repository.findAllById(projetoIds)) {
            porId.put(p.getId(), p);
        }

        Map<UUID, ArrayList<Projeto>> mapa = new HashMap<>();
        for (Object[] vinculo : vinculos) {
            UUID bolsistaId = (UUID) vinculo[0];
            Projeto projeto = porId.get((UUID) vinculo[1]);
            if (projeto != null) {
                mapa.computeIfAbsent(bolsistaId, k -> new ArrayList<>()).add(projeto);
            }
        }
        return mapa;
    }
}
