package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class ProfessorService {

    @Autowired
    private ProfessorRepository repository;

    public boolean inserir(Professor p) {
        repository.save(p);
        return true;
    }

    public ArrayList<Professor> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public ArrayList<Professor> buscarPorNome(String nome) {
        return new ArrayList<>(repository.findByNomeContainingIgnoreCaseAndAtivoTrueOrderByNome(nome));
    }

    public Professor buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    /* PoC de mover regra de acesso pro service: so admin ve professor, 404 se nao existir. */
    public Professor buscarExigindoAdmin(UUID id, Usuario logado) {
        if (!logado.isAdmin()) {
            throw new PermissaoNegadaException("Requer perfil de administrador.");
        }
        Professor p = buscarPorId(id);
        if (p == null) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado.");
        }
        return p;
    }

    public boolean atualizar(Professor p) {
        repository.save(p);
        return true;
    }

    /* soft delete */
    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.desativar(id) > 0;
    }
}
