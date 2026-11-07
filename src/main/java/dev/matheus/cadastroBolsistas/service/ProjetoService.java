package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.request.ProjetoRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import dev.matheus.cadastroBolsistas.dto.response.ProjetoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public Page<ProjetoResponse> buscarProjetos(String buscaNome, String labPublicId, Pageable pageable) {
        Page<Projeto> pagina = repository.findAll(Filtros.projeto(buscaNome, labPublicId), Filtros.ordenado(pageable, Sort.by("nome")));
        Map<UUID, Integer> membros = contarMembros(pagina.getContent());
        return pagina.map(p -> ProjetoResponse.from(p, membros.getOrDefault(p.getId(), 0)));
    }

    public ProjetoResponse detalhar(String id) {
        return responder(buscarOuFalhar(id));
    }

    /** uma consulta agrupada para todos os projetos da lista. */
    private Map<UUID, Integer> contarMembros(List<Projeto> projetos) {
        if (projetos.isEmpty()) {
            return Map.of();
        }
        return bolsistaRepository.contarPorProjetos(projetos.stream().map(Projeto::getId).toList()).stream()
                .collect(Collectors.toMap(BolsistaRepository.Contagem::getChave, c -> (int) c.getTotal()));
    }

    private ProjetoResponse responder(Projeto p) {
        return ProjetoResponse.from(p, contarMembros(p.getId()));
    }

    public ArrayList<Projeto> buscarProjetos(String buscaNome, String labPublicId) {
        String nome = buscaNome != null ? buscaNome.trim() : "";
        return new ArrayList<>(repository.findAll(Filtros.projeto(nome, labPublicId), Sort.by("nome")));
    }

    /** 404 se o laboratorio nao existe. */
    public List<ProjetoResponse> listarPorLaboratorio(String labPublicId) {
        laboratorioService.buscarOuFalhar(labPublicId);
        List<Projeto> projetos = repository.findByLaboratorio_PublicIdAndAtivoTrueOrderByNome(labPublicId);
        Map<UUID, Integer> membros = contarMembros(projetos);
        return projetos.stream().map(p -> ProjetoResponse.from(p, membros.getOrDefault(p.getId(), 0))).toList();
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
        // campos já normalizados pelo compact constructor de ProjetoRequest
        p.setNome(body.nome());
        p.setDescricao(body.descricao());
        String labPublicId = body.laboratorioId();
        if (labPublicId != null && !labPublicId.isBlank()) {
            Laboratorio lab = laboratorioService.buscarOuFalhar(labPublicId);
            p.setLaboratorio(lab);
        }
        p.setLinkRepositorio(body.linkRepositorio());
        p.setLinkDocumentacao(body.linkDocumentacao());
    }

    @Transactional
    public ProjetoResponse criar(ProjetoRequest body, Usuario usuario) {
        Projeto p = new Projeto();
        aplicar(p, body);
        cadastrar(p, usuario);
        return responder(p);
    }

    @Transactional
    public ProjetoResponse atualizar(String id, ProjetoRequest body, Usuario usuario) {
        Projeto p = buscarExigindoGerencia(id, usuario);
        exigirPodeMoverPara(usuario, body.laboratorioId());
        aplicar(p, body);
        p.setAtivo(true);
        atualizar(p);
        return responder(p);
    }

    /** exclusao logica; exige permissao de gerencia sobre o laboratorio do projeto. */
    @Transactional
    public void excluir(String id, Usuario logado) {
        Projeto p = buscarExigindoGerencia(id, logado);
        p.setAtivo(false);
        repository.save(p);
    }

    /** 404 se o bolsista nao existe ou esta inativo. */
    @Transactional
    public void vincularBolsista(String projetoId, String bolsistaId, Usuario logado) {
        Projeto projeto = buscarExigindoGerencia(projetoId, logado);
        projeto.getBolsistas().add(buscarBolsistaAtivo(bolsistaId));
        repository.save(projeto);
    }

    /** idempotente: desvincular quem nao esta na equipe nao e erro. */
    @Transactional
    public void desvincularBolsista(String projetoId, String bolsistaId, Usuario logado) {
        Projeto projeto = buscarExigindoGerencia(projetoId, logado);
        bolsistaRepository.findByPublicIdAndAtivoTrue(bolsistaId).ifPresent(projeto.getBolsistas()::remove);
        repository.save(projeto);
    }

    private Bolsista buscarBolsistaAtivo(String bolsistaId) {
        return bolsistaRepository.findByPublicIdAndAtivoTrue(bolsistaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bolsista nao encontrado."));
    }

    /** 404 se o projeto nao existe. */
    public ArrayList<Bolsista> listarMembros(String projetoPublicId) {
        buscarOuFalhar(projetoPublicId);
        return new ArrayList<>(bolsistaRepository.findByProjetosPublicIdAndAtivoTrueOrderByNome(projetoPublicId));
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
