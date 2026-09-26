package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ProjetoRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/*
 * Regras de negócio de projetos de pesquisa com identificadores públicos e UUIDs.
 */
@Service
public class ProjetoService {

    private final ProjetoRepository repository;
    private final LaboratorioService laboratorioService;
    private final BolsistaRepository bolsistaRepository;

    public ProjetoService(ProjetoRepository repository,
                          LaboratorioService laboratorioService,
                          BolsistaRepository bolsistaRepository) {
        this.repository = repository;
        this.laboratorioService = laboratorioService;
        this.bolsistaRepository = bolsistaRepository;
    }

    public boolean cadastrar(Projeto p, Usuario logado) {
        if (!laboratorioService.podeGerenciar(logado, p.getLaboratorioId())) {
            throw new PermissaoNegadaException("Sem permissao para criar projeto neste laboratorio.");
        }
        p.setAtivo(true);
        repository.save(p);
        return true;
    }

    public Projeto buscarOuFalhar(String publicId) {
        Projeto p = buscarPorId(publicId);
        if (p == null || !p.isAtivo()) {
            throw new RecursoNaoEncontradoException("Projeto nao encontrado.");
        }
        return p;
    }

    public Projeto buscarOuFalhar(UUID id) {
        Projeto p = buscarPorId(id);
        if (p == null || !p.isAtivo()) {
            throw new RecursoNaoEncontradoException("Projeto nao encontrado.");
        }
        return p;
    }

    public Projeto buscarExigindoGerencia(String publicId, Usuario logado) {
        Projeto p = buscarOuFalhar(publicId);
        if (!laboratorioService.podeGerenciar(logado, p.getLaboratorioId())) {
            throw new PermissaoNegadaException("Sem permissao para gerenciar projetos deste laboratorio.");
        }
        return p;
    }

    public Projeto buscarExigindoGerencia(UUID id, Usuario logado) {
        Projeto p = buscarOuFalhar(id);
        if (!laboratorioService.podeGerenciar(logado, p.getLaboratorioId())) {
            throw new PermissaoNegadaException("Sem permissao para gerenciar projetos deste laboratorio.");
        }
        return p;
    }

    public void exigirPodeMoverPara(Usuario logado, String novoLabPublicId) {
        if (!laboratorioService.podeGerenciar(logado, novoLabPublicId)) {
            throw new PermissaoNegadaException("Sem permissao para mover o projeto para este laboratorio.");
        }
    }

    public void exigirPodeMoverPara(Usuario logado, UUID novoLaboratorioId) {
        if (!laboratorioService.podeGerenciar(logado, novoLaboratorioId)) {
            throw new PermissaoNegadaException("Sem permissao para mover o projeto para este laboratorio.");
        }
    }

    public ArrayList<Projeto> listarTodos() {
        return buscarProjetos(null, (String) null);
    }

    public ArrayList<Projeto> buscarProjetos(String buscaNome, UUID labId) {
        String nome = buscaNome != null ? buscaNome.trim() : "";
        return new ArrayList<>(repository.findAll(Filtros.projeto(nome, labId), Sort.by("nome")));
    }

    public ArrayList<Projeto> buscarProjetos(String buscaNome, String labPublicId) {
        String nome = buscaNome != null ? buscaNome.trim() : "";
        return new ArrayList<>(repository.findAll(Filtros.projeto(nome, labPublicId), Sort.by("nome")));
    }

    public ArrayList<Projeto> listarPorLaboratorio(String labPublicId) {
        if (labPublicId == null || labPublicId.isBlank()) return new ArrayList<>();
        return new ArrayList<>(repository.findByLaboratorio_PublicIdAndAtivoTrueOrderByNome(labPublicId));
    }

    public ArrayList<Projeto> listarPorLaboratorio(UUID labId) {
        if (labId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByLaboratorioIdAndAtivoTrueOrderByNome(labId));
    }

    public Projeto buscarPorId(String publicId) {
        if (publicId == null || publicId.isBlank()) return null;
        Projeto p = repository.findByPublicIdAndAtivoTrue(publicId).orElse(null);
        if (p == null) {
            try {
                UUID uuid = UUID.fromString(publicId);
                return repository.findById(uuid).filter(Projeto::isAtivo).orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return p;
    }

    public Projeto buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    public int contarMembros(String publicId) {
        Projeto p = buscarPorId(publicId);
        return p != null ? contarMembros(p.getId()) : 0;
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
        String labPublicId = body.laboratorioId();
        if (labPublicId != null && !labPublicId.isBlank()) {
            Laboratorio lab = laboratorioService.buscarOuFalhar(labPublicId);
            p.setLaboratorio(lab);
        }
        p.setLinkRepositorio(StringUtil.limpar(body.linkRepositorio()));
        p.setLinkDocumentacao(StringUtil.limpar(body.linkDocumentacao()));
    }

    @Transactional
    public boolean excluir(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        return repository.findByPublicId(publicId).map(p -> {
            p.setAtivo(false);
            repository.save(p);
            return true;
        }).orElseGet(() -> {
            try {
                UUID uuid = UUID.fromString(publicId);
                return excluir(uuid);
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
    }

    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.findById(id).map(p -> {
            p.setAtivo(false);
            repository.save(p);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean vincularBolsista(String bolsistaPublicId, String projetoPublicId) {
        return alterarVinculo(bolsistaPublicId, projetoPublicId, true);
    }

    @Transactional
    public boolean vincularBolsista(UUID bolsistaId, UUID projetoId) {
        if (bolsistaId == null || projetoId == null) return false;
        return alterarVinculo(bolsistaId.toString(), projetoId.toString(), true);
    }

    @Transactional
    public boolean desvincularBolsista(String bolsistaPublicId, String projetoPublicId) {
        return alterarVinculo(bolsistaPublicId, projetoPublicId, false);
    }

    @Transactional
    public boolean desvincularBolsista(UUID bolsistaId, UUID projetoId) {
        if (bolsistaId == null || projetoId == null) return false;
        return alterarVinculo(bolsistaId.toString(), projetoId.toString(), false);
    }

    private boolean alterarVinculo(String bolsistaIdOuPublicId, String projetoIdOuPublicId, boolean vincular) {
        if (bolsistaIdOuPublicId == null || projetoIdOuPublicId == null) return false;
        Projeto projeto = buscarPorId(projetoIdOuPublicId);
        Bolsista bolsista = bolsistaRepository.findByPublicIdAndAtivoTrue(bolsistaIdOuPublicId)
                .orElseGet(() -> {
                    try {
                        return bolsistaRepository.findById(UUID.fromString(bolsistaIdOuPublicId)).filter(Bolsista::isAtivo).orElse(null);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });
        if (projeto == null || bolsista == null) return false;
        if (vincular) {
            projeto.getBolsistas().add(bolsista);
        } else {
            projeto.getBolsistas().remove(bolsista);
        }
        repository.save(projeto);
        return true;
    }

    public ArrayList<Projeto> listarPorBolsista(String bolsistaPublicId) {
        if (bolsistaPublicId == null || bolsistaPublicId.isBlank()) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsistas_PublicIdAndAtivoTrueOrderByNome(bolsistaPublicId));
    }

    public ArrayList<Projeto> listarPorBolsista(UUID bolsistaId) {
        if (bolsistaId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsistas_IdAndAtivoTrueOrderByNome(bolsistaId));
    }
}
