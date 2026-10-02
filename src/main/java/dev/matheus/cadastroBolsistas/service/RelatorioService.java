package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ResumoGeralResponse;
import dev.matheus.cadastroBolsistas.repository.RelatorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * dados agregados da tela de relatorios. quem barra nao-admin e o
 * SecurityConfig, na regra hasRole("ADMIN") de /api/v1/relatorios/**.
 */
@Service
public class RelatorioService {

    private final RelatorioRepository repository;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;
    private final ProjetoService projetoService;

    @Autowired
    public RelatorioService(RelatorioRepository repository,
                            BolsistaService bolsistaService,
                            LaboratorioService laboratorioService,
                            ProjetoService projetoService) {
        this.repository = repository;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
        this.projetoService = projetoService;
    }

    public ResumoGeralResponse obterResumoGeral() {
        long totalBolsistas = bolsistaService.listarTodos().size();
        long totalLaboratorios = laboratorioService.listarTodos().size();
        long totalProjetos = projetoService.listarTodos().size();
        return new ResumoGeralResponse(totalBolsistas, totalLaboratorios, totalProjetos);
    }

    public List<RelatorioRepository.HorasBolsista> getHorasBolsistasMesCorrente() {
        return repository.horasBolsistasMesCorrente();
    }

    public List<RelatorioRepository.ProjetosPorLaboratorio> getProjetosAtivosPorLaboratorio() {
        return repository.projetosAtivosPorLaboratorio();
    }

    public List<RelatorioRepository.BolsistasPorCargo> getBolsistasPorCargo() {
        return repository.bolsistasPorCargo();
    }

    public List<RelatorioRepository.OcupacaoLaboratorio> getLaboratoriosOcupacao() {
        return repository.laboratoriosOcupacao();
    }
}
