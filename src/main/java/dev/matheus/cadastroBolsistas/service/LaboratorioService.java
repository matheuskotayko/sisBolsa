package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.LaboratorioRequest;
import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.BolsistaRepository;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/*
 * Regras de negócio de laboratórios com suporte a identificadores públicos e UUIDs.
 */
@Service
public class LaboratorioService {

    private final LaboratorioRepository repository;
    private final ProjetoRepository projetoRepository;
    private final BolsistaRepository bolsistaRepository;
    private final ProfessorRepository professorRepository;

    public LaboratorioService(LaboratorioRepository repository,
                              ProjetoRepository projetoRepository,
                              BolsistaRepository bolsistaRepository,
                              ProfessorRepository professorRepository) {
        this.repository = repository;
        this.projetoRepository = projetoRepository;
        this.bolsistaRepository = bolsistaRepository;
        this.professorRepository = professorRepository;
    }

    public boolean podeGerenciar(Usuario usuarioLogado, Laboratorio lab) {
        if (usuarioLogado == null || lab == null) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (usuarioLogado.isProfessor()) {
            return Objects.equals(lab.getCoordenadorId(), usuarioLogado.getId());
        }
        return false;
    }

    public boolean podeGerenciar(Usuario usuarioLogado, String labPublicId) {
        if (usuarioLogado == null || labPublicId == null || labPublicId.isBlank()) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (!usuarioLogado.isProfessor()) return false;
        Laboratorio lab = repository.findByPublicId(labPublicId).orElse(null);
        return podeGerenciar(usuarioLogado, lab);
    }

    public boolean podeGerenciar(Usuario usuarioLogado, UUID labId) {
        if (usuarioLogado == null || labId == null) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (!usuarioLogado.isProfessor()) return false;
        Laboratorio lab = repository.findById(labId).orElse(null);
        return podeGerenciar(usuarioLogado, lab);
    }

    public boolean cadastrar(Laboratorio lab) {
        lab.setAtivo(true);
        repository.save(lab);
        return true;
    }

    public Laboratorio buscarOuFalhar(String publicId) {
        Laboratorio lab = buscarPorId(publicId);
        if (lab == null || !lab.isAtivo()) {
            throw new RecursoNaoEncontradoException("Laboratorio nao encontrado.");
        }
        return lab;
    }

    public Laboratorio buscarOuFalhar(UUID id) {
        Laboratorio lab = buscarPorId(id);
        if (lab == null || !lab.isAtivo()) {
            throw new RecursoNaoEncontradoException("Laboratorio nao encontrado.");
        }
        return lab;
    }

    public Laboratorio buscarExigindoGerencia(String publicId, Usuario logado) {
        Laboratorio lab = buscarOuFalhar(publicId);
        if (!podeGerenciar(logado, lab)) {
            throw new PermissaoNegadaException("Sem permissao para gerenciar este laboratorio.");
        }
        return lab;
    }

    public Laboratorio buscarExigindoGerencia(UUID id, Usuario logado) {
        Laboratorio lab = buscarOuFalhar(id);
        if (!podeGerenciar(logado, lab)) {
            throw new PermissaoNegadaException("Sem permissao para gerenciar este laboratorio.");
        }
        return lab;
    }

    public ArrayList<Laboratorio> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public ArrayList<Laboratorio> buscarLaboratorios(String buscaNome) {
        String nome = buscaNome != null ? buscaNome.trim() : "";
        return new ArrayList<>(repository.findAll(Filtros.laboratorio(nome), Sort.by("nome")));
    }

    public ArrayList<Laboratorio> listarPorCoordenador(UUID professorId) {
        if (professorId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByCoordenadorIdAndAtivoTrueOrderByNome(professorId));
    }

    public Laboratorio buscarPorId(String publicId) {
        if (publicId == null || publicId.isBlank()) return null;
        Laboratorio lab = repository.findByPublicIdAndAtivoTrue(publicId).orElse(null);
        if (lab == null) {
            try {
                UUID uuid = UUID.fromString(publicId);
                lab = repository.findById(uuid).filter(Laboratorio::isAtivo).orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (lab != null) {
            lab.setProjetos(new ArrayList<>(projetoRepository.findByLaboratorio_PublicIdAndAtivoTrueOrderByNome(lab.getPublicId())));
        }
        return lab;
    }

    public Laboratorio buscarPorId(UUID id) {
        if (id == null) return null;
        Laboratorio lab = repository.findById(id).orElse(null);
        if (lab != null) {
            lab.setProjetos(new ArrayList<>(projetoRepository.findByLaboratorioIdAndAtivoTrueOrderByNome(id)));
        }
        return lab;
    }

    public boolean atualizar(Laboratorio lab) {
        repository.save(lab);
        return true;
    }

    @Transactional
    public boolean excluir(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        return repository.findByPublicId(publicId).map(lab -> {
            lab.setAtivo(false);
            repository.save(lab);
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
        return repository.findById(id).map(lab -> {
            lab.setAtivo(false);
            repository.save(lab);
            return true;
        }).orElse(false);
    }

    public boolean temVaga(Laboratorio lab) {
        return lab != null && contarBolsistasNoLaboratorio(lab.getId()) < lab.getCapacidade();
    }

    public boolean temVaga(String publicId) {
        return temVaga(buscarPorId(publicId));
    }

    public boolean temVaga(UUID labId) {
        return temVaga(buscarPorId(labId));
    }

    public int contarBolsistasNoLaboratorio(Laboratorio lab) {
        return lab != null && lab.getId() != null ? contarBolsistasNoLaboratorio(lab.getId()) : 0;
    }

    public int contarBolsistasNoLaboratorio(String publicId) {
        return contarBolsistasNoLaboratorio(buscarPorId(publicId));
    }

    public int contarBolsistasNoLaboratorio(UUID labId) {
        if (labId == null) return 0;
        return bolsistaRepository.countByLaboratorioIdAndAtivoTrue(labId);
    }

    public void aplicar(Laboratorio lab, LaboratorioRequest body) {
        lab.setNome(StringUtil.limpar(body.nome()));
        lab.setAreaPesquisa(body.areaPesquisa());
        lab.setStatus(StringUtil.estaVazio(body.status()) ? "Ativo" : body.status());
        lab.setCapacidade(body.capacidade());
        String coordId = body.coordenadorId();
        if (coordId != null && !coordId.isBlank()) {
            Professor prof = professorRepository.findByPublicIdAndAtivoTrue(coordId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Professor coordenador nao encontrado com id: " + coordId));
            lab.setCoordenadorProfessor(prof);
        } else {
            lab.setCoordenadorProfessor(null);
            lab.setCoordenadorId(null);
        }
    }

    public List<Laboratorio> filtrarPorTermo(List<Laboratorio> labs, String buscaNome) {
        if (StringUtil.estaVazio(buscaNome)) {
            return labs;
        }
        String termo = buscaNome.trim().toLowerCase();
        return labs.stream()
                .filter(l -> contem(l.getNome(), termo) || contem(l.getAreaPesquisa(), termo) || contem(l.getCoordenador(), termo))
                .toList();
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    public void preencherLabsDosProfessores(List<Usuario> lista) {
        for (Usuario u : lista) {
            if (u.isProfessor()) {
                List<Laboratorio> labs = listarPorCoordenador(u.getId());
                u.setNomeLaboratorio(labs.isEmpty()
                        ? "Nenhum"
                        : labs.stream().map(Laboratorio::getNome).reduce((a, b) -> a + ", " + b).orElse("Nenhum"));
            }
        }
    }
}
