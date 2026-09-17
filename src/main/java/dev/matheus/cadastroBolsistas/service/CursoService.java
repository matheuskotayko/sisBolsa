package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Curso;
import dev.matheus.cadastroBolsistas.repository.CursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CursoService {

    @Autowired
    private CursoRepository repository;

    public ArrayList<Curso> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
    }

    public boolean existePorNome(String nome) {
        return repository.existsByNomeIgnoreCaseAndAtivoTrue(nome);
    }

    public Curso cadastrar(String nome) {
        Curso curso = new Curso();
        curso.setNome(nome);
        curso.setAtivo(true);
        return repository.save(curso);
    }
}
