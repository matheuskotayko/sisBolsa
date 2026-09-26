package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Curso;
import dev.matheus.cadastroBolsistas.repository.CursoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class CursoService {

    private final CursoRepository repository;

    public CursoService(CursoRepository repository) {
        this.repository = repository;
    }

    public ArrayList<Curso> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public boolean existePorNome(String nome) {
        return repository.existsByNomeIgnoreCaseAndAtivoTrue(nome);
    }

    /* quem barra nao-admin e o SecurityConfig; aqui so sobra o nome unico (ativo). */
    public Curso cadastrar(String nome) {
        if (existePorNome(nome)) {
            throw new IllegalArgumentException("Este curso ja esta cadastrado.");
        }
        Curso curso = new Curso();
        curso.setNome(nome);
        curso.setAtivo(true);
        return repository.save(curso);
    }

    public Curso buscarPorId(String idOuPublicId) {
        if (idOuPublicId == null || idOuPublicId.isBlank()) return null;
        return repository.findByPublicIdAndAtivoTrue(idOuPublicId)
                .or(() -> {
                    try {
                        return repository.findById(UUID.fromString(idOuPublicId)).filter(Curso::isAtivo);
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    public Curso buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findById(id).filter(Curso::isAtivo).orElse(null);
    }

    public Curso buscarOuFalhar(String idOuPublicId) {
        Curso c = buscarPorId(idOuPublicId);
        if (c == null) {
            throw new RecursoNaoEncontradoException("Curso nao encontrado.");
        }
        return c;
    }

    public Curso buscarOuFalhar(UUID id) {
        Curso c = buscarPorId(id);
        if (c == null) {
            throw new RecursoNaoEncontradoException("Curso nao encontrado.");
        }
        return c;
    }
}
