package dev.matheus.cadastroBolsistas.service;

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

    @Autowired
    private RelatorioRepository repository;

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
