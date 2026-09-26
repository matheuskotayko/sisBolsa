package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ProfessorRequest;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/*
 * Regras de negócio de professores coordenadores com suporte a publicId e UUIDs.
 */
@Service
public class ProfessorService {

    private final ProfessorRepository repository;

    public ProfessorService(ProfessorRepository repository) {
        this.repository = repository;
    }

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

    public Professor buscarPorId(String publicId) {
        if (publicId == null || publicId.isBlank()) return null;
        Professor p = repository.findByPublicIdAndAtivoTrue(publicId).orElse(null);
        if (p == null) {
            try {
                UUID uuid = UUID.fromString(publicId);
                return repository.findById(uuid).filter(Professor::isAtivo).orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return p;
    }

    public Professor buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).filter(Professor::isAtivo).orElse(null);
    }

    public Professor buscarOuFalhar(String publicId) {
        Professor p = buscarPorId(publicId);
        if (p == null) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado.");
        }
        return p;
    }

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
}
