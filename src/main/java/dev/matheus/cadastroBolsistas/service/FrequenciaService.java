package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.Filtros;
import dev.matheus.cadastroBolsistas.repository.FrequenciaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/*
 * Regras de negócio de frequência com suporte a identificadores públicos e UUIDs.
 */
@Service
public class FrequenciaService {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "data");

    private final FrequenciaRepository repository;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;

    public FrequenciaService(FrequenciaRepository repository,
                             BolsistaService bolsistaService,
                             LaboratorioService laboratorioService) {
        this.repository = repository;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
    }

    public boolean registrar(Frequencia f) {
        f.setAtivo(true);
        repository.save(f);
        f.setBolsista(bolsistaService.buscarPorId(f.getBolsistaId()));
        return true;
    }

    public Frequencia buscarPorId(String publicId) {
        if (publicId == null || publicId.isBlank()) return null;
        Frequencia f = repository.findByPublicIdAndAtivoTrue(publicId).orElse(null);
        if (f == null) {
            try {
                UUID uuid = UUID.fromString(publicId);
                return repository.findByIdAndAtivoTrue(uuid).orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return f;
    }

    public Frequencia buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findByIdAndAtivoTrue(id).orElse(null);
    }

    public Frequencia buscarOuFalhar(String publicId) {
        Frequencia f = buscarPorId(publicId);
        if (f == null) {
            throw new RecursoNaoEncontradoException("Registro de frequencia nao encontrado.");
        }
        return f;
    }

    public Frequencia buscarOuFalhar(UUID id) {
        Frequencia f = buscarPorId(id);
        if (f == null) {
            throw new RecursoNaoEncontradoException("Registro de frequencia nao encontrado.");
        }
        return f;
    }

    public boolean podeAcessar(Usuario logado, Bolsista b) {
        if (logado == null || b == null) return false;
        if (logado.isAdmin()) return true;
        if (Objects.equals(logado.getId(), b.getId())) return true;
        if (logado.isProfessor()) {
            return b.getLaboratorioId() != null
                    && laboratorioService.podeGerenciar(logado, b.getLaboratorioId());
        }
        return false;
    }

    public boolean podeAcessar(Usuario logado, String bolsistaPublicId) {
        if (bolsistaPublicId == null || bolsistaPublicId.isBlank()) return false;
        Bolsista b = bolsistaService.buscarPorId(bolsistaPublicId);
        return podeAcessar(logado, b);
    }

    public boolean podeAcessar(Usuario logado, UUID bolsistaId) {
        if (logado == null || bolsistaId == null) return false;
        if (logado.isAdmin() || Objects.equals(logado.getId(), bolsistaId)) return true;
        Bolsista b = bolsistaService.buscarPorId(bolsistaId);
        return podeAcessar(logado, b);
    }

    public void exigirAcesso(Usuario logado, String bolsistaPublicId) {
        if (!podeAcessar(logado, bolsistaPublicId)) {
            throw new PermissaoNegadaException("Sem permissao para acessar as frequencias deste usuario.");
        }
    }

    public void exigirAcesso(Usuario logado, UUID bolsistaId) {
        if (!podeAcessar(logado, bolsistaId)) {
            throw new PermissaoNegadaException("Sem permissao para acessar as frequencias deste usuario.");
        }
    }

    public Frequencia buscarComPermissao(String publicId, Usuario logado) {
        Frequencia f = buscarOuFalhar(publicId);
        exigirAcesso(logado, f.getBolsistaId());
        return f;
    }

    public Frequencia buscarComPermissao(UUID id, Usuario logado) {
        Frequencia f = buscarOuFalhar(id);
        exigirAcesso(logado, f.getBolsistaId());
        return f;
    }

    public boolean atualizar(Frequencia f) {
        repository.save(f);
        return true;
    }

    public ArrayList<Frequencia> listarPorBolsista(String bolsistaPublicId) {
        if (bolsistaPublicId == null || bolsistaPublicId.isBlank()) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsista_PublicIdAndAtivoTrueOrderByDataDesc(bolsistaPublicId));
    }

    public ArrayList<Frequencia> listarPorBolsista(UUID bolsistaId) {
        if (bolsistaId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsistaIdAndAtivoTrueOrderByDataDesc(bolsistaId));
    }

    public ArrayList<Frequencia> listarPorLaboratorio(String labPublicId) {
        if (labPublicId == null || labPublicId.isBlank()) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsista_Laboratorio_PublicIdAndAtivoTrueOrderByDataDesc(labPublicId));
    }

    public ArrayList<Frequencia> listarPorLaboratorio(UUID labId) {
        if (labId == null) return new ArrayList<>();
        return new ArrayList<>(repository.findByBolsista_LaboratorioIdAndAtivoTrueOrderByDataDesc(labId));
    }

    public ArrayList<Frequencia> listarTodas() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByDataDesc());
    }

    public ArrayList<Frequencia> buscarFrequencias(UUID bolsistaId, LocalDate dataInicio, LocalDate dataFim, Integer limit, Integer offset) {
        Specification<Frequencia> filtro = Filtros.frequencia(bolsistaId, dataInicio, dataFim);
        return new ArrayList<>(repository.findAll(filtro, paginar(limit, offset)).getContent());
    }

    public ArrayList<Frequencia> buscarFrequencias(UUID bolsistaId, Integer limit, Integer offset) {
        return buscarFrequencias(bolsistaId, null, null, limit, offset);
    }

    public ArrayList<Frequencia> buscarPorBolsistas(List<UUID> ids, LocalDate dataInicio, LocalDate dataFim, Integer limit, Integer offset) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Specification<Frequencia> filtro = Filtros.frequenciaDeVarios(ids, dataInicio, dataFim);
        return new ArrayList<>(repository.findAll(filtro, paginar(limit, offset)).getContent());
    }

    public ArrayList<Frequencia> buscarPorBolsistas(List<UUID> ids, Integer limit, Integer offset) {
        return buscarPorBolsistas(ids, null, null, limit, offset);
    }

    private Pageable paginar(Integer limit, Integer offset) {
        if (limit != null && limit > 0 && offset != null && offset >= 0) {
            return PageRequest.of(offset / limit, limit, MAIS_RECENTES);
        }
        return Pageable.unpaged(MAIS_RECENTES);
    }

    public int contarPorBolsistas(List<UUID> ids, LocalDate dataInicio, LocalDate dataFim) {
        if (ids == null || ids.isEmpty()) return 0;
        return (int) repository.count(Filtros.frequenciaDeVarios(ids, dataInicio, dataFim));
    }

    public int contarPorBolsistas(List<UUID> ids) {
        return contarPorBolsistas(ids, null, null);
    }

    public int contarFrequencias(UUID bolsistaId, LocalDate dataInicio, LocalDate dataFim) {
        return (int) repository.count(Filtros.frequencia(bolsistaId, dataInicio, dataFim));
    }

    public int contarFrequencias(UUID bolsistaId) {
        return contarFrequencias(bolsistaId, null, null);
    }

    @Transactional
    public boolean excluir(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        return repository.findByPublicId(publicId).map(f -> {
            f.setAtivo(false);
            repository.save(f);
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
        return repository.findById(id).map(f -> {
            f.setAtivo(false);
            repository.save(f);
            return true;
        }).orElse(false);
    }

    public Bolsista resolverBolsistaAlvo(Usuario logado, String bolsistaPublicId) {
        if (logado.isBolsista()) {
            return bolsistaService.buscarOuFalhar(logado.getId());
        }
        if (bolsistaPublicId == null || bolsistaPublicId.isBlank()) {
            throw new IllegalArgumentException("Informe o bolsista para o qual o registro esta sendo feito.");
        }
        return bolsistaService.buscarOuFalhar(bolsistaPublicId);
    }

    public UUID resolverBolsistaAlvo(Usuario logado, UUID bolsistaId) {
        if (logado.isBolsista()) {
            return logado.getId();
        }
        if (bolsistaId == null) {
            throw new IllegalArgumentException("Informe o bolsista para o qual o registro esta sendo feito.");
        }
        return bolsistaId;
    }
}
