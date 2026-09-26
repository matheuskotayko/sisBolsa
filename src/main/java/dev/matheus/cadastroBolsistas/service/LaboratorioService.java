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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/*
 * regras de negocio de laboratorios com IDs em UUID.
 */
@Service
public class LaboratorioService {

    @Autowired
    private LaboratorioRepository repository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private BolsistaRepository bolsistaRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    public boolean podeGerenciar(Usuario usuarioLogado, String labPublicId) {
        if (usuarioLogado == null || labPublicId == null || labPublicId.isBlank()) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (usuarioLogado.isProfessor()) {
            Laboratorio lab = repository.findByPublicId(labPublicId).orElse(null);
            return lab != null && Objects.equals(lab.getCoordenadorId(), usuarioLogado.getId());
        }
        return false;
    }

    public boolean podeGerenciar(Usuario usuarioLogado, UUID labId) {
        if (usuarioLogado == null || labId == null) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (usuarioLogado.isProfessor()) {
            Laboratorio lab = repository.findById(labId).orElse(null);
            return lab != null && Objects.equals(lab.getCoordenadorId(), usuarioLogado.getId());
        }
        return false;
    }

    /* quem barra nao-admin e o SecurityConfig, na regra POST /api/v1/laboratorios. */
    public boolean cadastrar(Laboratorio lab) {
        lab.setAtivo(true);
        repository.save(lab);
        return true;
    }

    /* lookup + 404 (incluindo desativado) num so lugar, pra nenhum controller precisar checar null na mao. */
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

    /* so quem gerencia (admin ou o professor coordenador) pode editar/excluir. */
    public Laboratorio buscarExigindoGerencia(String publicId, Usuario logado) {
        Laboratorio lab = buscarOuFalhar(publicId);
        if (!podeGerenciar(logado, publicId)) {
            throw new PermissaoNegadaException("Sem permissao para gerenciar este laboratorio.");
        }
        return lab;
    }

    public Laboratorio buscarExigindoGerencia(UUID id, Usuario logado) {
        Laboratorio lab = buscarOuFalhar(id);
        if (!podeGerenciar(logado, id)) {
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
        if (lab != null) {
            lab.setProjetos(new ArrayList<>(projetoRepository.findByLaboratorio_PublicIdAndAtivoTrueOrderByNome(publicId)));
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

    /* soft delete: carrega, marca ativo = false e deixa o JPA fazer o UPDATE. */
    @Transactional
    public boolean excluir(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        return repository.findByPublicId(publicId).map(lab -> {
            lab.setAtivo(false);
            repository.save(lab);
            return true;
        }).orElse(false);
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

    public boolean temVaga(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        Laboratorio lab = repository.findByPublicId(publicId).orElse(null);
        if (lab == null) return false;
        return contarBolsistasNoLaboratorio(lab.getId()) < lab.getCapacidade();
    }

    public boolean temVaga(UUID labId) {
        if (labId == null) return false;
        Laboratorio lab = repository.findById(labId).orElse(null);
        if (lab == null) return false;
        return contarBolsistasNoLaboratorio(labId) < lab.getCapacidade();
    }

    public int contarBolsistasNoLaboratorio(String publicId) {
        if (publicId == null || publicId.isBlank()) return 0;
        Laboratorio lab = repository.findByPublicId(publicId).orElse(null);
        return lab != null ? contarBolsistasNoLaboratorio(lab.getId()) : 0;
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

    /* professor coordena poucos laboratorios - filtra em memoria em vez de virar mais uma query no banco */
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
