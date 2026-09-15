package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.LaboratorioRequest;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.repository.ProjetoRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
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

    public boolean podeGerenciar(Usuario usuarioLogado, UUID labId) {
        if (usuarioLogado == null || labId == null) return false;
        if (usuarioLogado.isAdmin()) return true;
        if (usuarioLogado.isProfessor()) {
            Laboratorio lab = repository.findById(labId).orElse(null);
            return lab != null && Objects.equals(lab.getCoordenadorId(), usuarioLogado.getId());
        }
        return false;
    }

    public boolean cadastrar(Laboratorio lab) {
        lab.setAtivo(true);
        repository.save(lab);
        return true;
    }

    public ArrayList<Laboratorio> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public ArrayList<Laboratorio> buscarLaboratorios(String buscaNome) {
        String nome = buscaNome != null ? buscaNome.trim() : "";
        return new ArrayList<>(repository.buscarLaboratorios(nome));
    }

    public ArrayList<Laboratorio> listarPorCoordenador(UUID professorId) {
        if (professorId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorCoordenador(professorId));
    }

    public Laboratorio buscarPorId(UUID id) {
        if (id == null) return null;
        Laboratorio lab = repository.findById(id).orElse(null);
        if (lab != null) {
            lab.setProjetos(new ArrayList<>(projetoRepository.buscarPorLaboratorio(id)));
        }
        return lab;
    }

    public boolean atualizar(Laboratorio lab) {
        repository.save(lab);
        return true;
    }

    /* soft delete */
    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.desativar(id) > 0;
    }

    public boolean temVaga(UUID labId) {
        if (labId == null) return false;
        Laboratorio lab = repository.findById(labId).orElse(null);
        if (lab == null) return false;
        return repository.contarBolsistasAtivos(labId) < lab.getCapacidade();
    }

    public int contarBolsistasNoLaboratorio(UUID labId) {
        if (labId == null) return 0;
        return repository.contarBolsistasAtivos(labId);
    }

    public void aplicar(Laboratorio lab, LaboratorioRequest body) {
        lab.setNome(StringUtil.limpar(body.nome()));
        lab.setAreaPesquisa(body.areaPesquisa());
        lab.setStatus(StringUtil.estaVazio(body.status()) ? "Ativo" : body.status());
        lab.setCapacidade(body.capacidade());
        lab.setCoordenadorId(body.coordenadorId());
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
