package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.FrequenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FrequenciaService {

    @Autowired
    private FrequenciaRepository repository;

    @Autowired
    private BolsistaService bolsistaService;

    @Autowired
    private LaboratorioService laboratorioService;

    public boolean registrar(Frequencia f) {
        f.setAtivo(true);
        repository.save(f);
        return true;
    }

    public Frequencia buscarPorId(UUID id) {
        if (id == null) return null;
        return repository.findByIdAndAtivoTrue(id).orElse(null);
    }

    public boolean atualizar(Frequencia f) {
        repository.save(f);
        return true;
    }

    public ArrayList<Frequencia> listarPorBolsista(UUID bolsistaId) {
        if (bolsistaId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorBolsista(bolsistaId));
    }

    public ArrayList<Frequencia> listarPorLaboratorio(UUID labId) {
        if (labId == null) return new ArrayList<>();
        return new ArrayList<>(repository.buscarPorLaboratorio(labId));
    }

    public ArrayList<Frequencia> listarTodas() {
        return new ArrayList<>(repository.findByAtivoTrueOrderByDataDesc());
    }

    public ArrayList<Frequencia> buscarFrequencias(UUID bolsistaId, LocalDate dataInicio, LocalDate dataFim, Integer limit, Integer offset) {
        Pageable pageable = Pageable.unpaged();
        if (limit != null && limit > 0 && offset != null && offset >= 0) {
            pageable = PageRequest.of(offset / limit, limit);
        }
        return new ArrayList<>(repository.buscarFrequencias(bolsistaId, dataInicio, dataFim, pageable));
    }

    public ArrayList<Frequencia> buscarFrequencias(UUID bolsistaId, Integer limit, Integer offset) {
        return buscarFrequencias(bolsistaId, null, null, limit, offset);
    }

    public ArrayList<Frequencia> buscarPorBolsistas(List<UUID> ids, LocalDate dataInicio, LocalDate dataFim, Integer limit, Integer offset) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Pageable pageable = Pageable.unpaged();
        if (limit != null && limit > 0 && offset != null && offset >= 0) {
            pageable = PageRequest.of(offset / limit, limit);
        }
        return new ArrayList<>(repository.buscarPorBolsistas(ids, dataInicio, dataFim, pageable));
    }

    public ArrayList<Frequencia> buscarPorBolsistas(List<UUID> ids, Integer limit, Integer offset) {
        return buscarPorBolsistas(ids, null, null, limit, offset);
    }

    public int contarPorBolsistas(List<UUID> ids, LocalDate dataInicio, LocalDate dataFim) {
        return (ids == null || ids.isEmpty()) ? 0 : repository.contarPorBolsistas(ids, dataInicio, dataFim);
    }

    public int contarPorBolsistas(List<UUID> ids) {
        return contarPorBolsistas(ids, null, null);
    }

    public int contarFrequencias(UUID bolsistaId, LocalDate dataInicio, LocalDate dataFim) {
        return repository.contarFrequencias(bolsistaId, dataInicio, dataFim);
    }

    public int contarFrequencias(UUID bolsistaId) {
        return contarFrequencias(bolsistaId, null, null);
    }

    /* soft delete */
    @Transactional
    public boolean excluir(UUID id) {
        if (id == null) return false;
        return repository.desativar(id) > 0;
    }

    /* admin ve tudo; usuario ve o proprio; professor ve quem esta no laboratorio que coordena */
    public boolean podeAcessar(Usuario logado, UUID bolsistaId) {
        if (logado.isAdmin()) {
            return true;
        }
        if (Objects.equals(logado.getId(), bolsistaId)) {
            return true;
        }
        if (logado.isProfessor()) {
            Bolsista b = bolsistaService.buscarPorId(bolsistaId);
            return b != null && b.getLaboratorioId() != null
                    && laboratorioService.podeGerenciar(logado, b.getLaboratorioId());
        }
        return false;
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
