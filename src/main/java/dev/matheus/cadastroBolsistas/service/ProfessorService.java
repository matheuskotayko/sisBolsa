package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ProfessorRequest;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
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

    public void aplicarComuns(Professor p, ProfessorRequest body) {
        p.setNome(StringUtil.limpar(body.nome()));
        p.setEmail(StringUtil.limpar(body.email()));
        p.setFotoUrl(body.fotoUrl());
        p.setBio(body.bio());
        p.setAtivo(true);
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

    /* lookup + 404 num so lugar, pra nenhum controller precisar checar null na mao. */
    public Professor buscarOuFalhar(UUID id) {
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
}
