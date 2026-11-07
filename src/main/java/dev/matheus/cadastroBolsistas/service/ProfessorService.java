package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.common.StringUtil;
import dev.matheus.cadastroBolsistas.dto.request.ProfessorRequest;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.LaboratorioRepository;
import dev.matheus.cadastroBolsistas.repository.ProfessorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * Regras de negócio de professores coordenadores com suporte a publicId e UUIDs.
 */
@Service
public class ProfessorService {

    private final ProfessorRepository repository;
    private final LaboratorioRepository laboratorioRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ProfessorService(ProfessorRepository repository,
                            LaboratorioRepository laboratorioRepository,
                            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.laboratorioRepository = laboratorioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfessorService(ProfessorRepository repository) {
        this(repository, null, null);
    }

    public boolean inserir(Professor p) {
        repository.save(p);
        return true;
    }

    public void aplicarComuns(Professor p, ProfessorRequest body) {
        // nome e email já vêm normalizados pelo compact constructor de ProfessorRequest
        p.setNome(body.nome());
        p.setEmail(body.email());
        p.setFotoUrl(body.fotoUrl());
        p.setBio(body.bio());
        p.setAtivo(true);
    }

    public ArrayList<Professor> listarTodos() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByNome());
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

    /** exclusao logica; o controller ja restringe a administradores. */
    @Transactional
    public void excluir(String id) {
        Professor p = buscarOuFalhar(id);
        p.setAtivo(false);
        repository.save(p);
    }

    public Page<Professor> listarComLaboratorio(String buscaNome, Pageable pageable) {
        Page<Professor> pagina = repository.findAll(Filtros.professor(buscaNome), Filtros.ordenado(pageable, Sort.by("usuario.nome")));
        enriquecerComLaboratorios(pagina.getContent());
        return pagina;
    }

    public Professor buscarComLaboratorio(String id) {
        Professor p = buscarOuFalhar(id);
        enriquecerComLaboratorio(p);
        return p;
    }

    public void enriquecerComLaboratorios(List<Professor> professores) {
        if (professores == null || professores.isEmpty() || laboratorioRepository == null) return;
        List<Laboratorio> labs = laboratorioRepository.findByAtivoTrueOrderByNome();
        Map<UUID, String> labPorCoordenador = labs.stream()
                .filter(l -> l.getCoordenadorId() != null)
                .collect(Collectors.toMap(
                        Laboratorio::getCoordenadorId,
                        Laboratorio::getNome,
                        (existente, novo) -> existente
                ));
        for (Professor p : professores) {
            p.setNomeLaboratorio(labPorCoordenador.get(p.getId()));
        }
    }

    public void enriquecerComLaboratorio(Professor professor) {
        if (professor == null || laboratorioRepository == null) return;
        laboratorioRepository.findByCoordenadorIdAndAtivoTrueOrderByNome(professor.getId())
                .stream().findFirst()
                .ifPresent(lab -> professor.setNomeLaboratorio(lab.getNome()));
    }

    @Transactional
    public Professor criar(ProfessorRequest body) {
        validarSenha(body.senha(), true);
        Professor p = new Professor();
        aplicarComuns(p, body);
        if (passwordEncoder != null && !StringUtil.estaVazio(body.senha())) {
            p.setSenha(passwordEncoder.encode(body.senha()));
        }
        inserir(p);
        return p;
    }

    @Transactional
    public Professor atualizar(String id, ProfessorRequest body) {
        validarSenha(body.senha(), false);
        Professor p = buscarOuFalhar(id);
        aplicarComuns(p, body);
        if (passwordEncoder != null && !StringUtil.estaVazio(body.senha())) {
            p.setSenha(passwordEncoder.encode(body.senha()));
        }
        atualizar(p);
        enriquecerComLaboratorio(p);
        return p;
    }

    /** tamanho minimo fica no @Size do request; aqui so a obrigatoriedade no cadastro. */
    public void validarSenha(String senha, boolean exigirSenha) {
        if (exigirSenha && StringUtil.estaVazio(senha)) {
            throw new IllegalArgumentException("Senha e obrigatoria.");
        }
    }
}
