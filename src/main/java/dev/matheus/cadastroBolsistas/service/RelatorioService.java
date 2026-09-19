package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.exceptions.PermissaoNegadaException;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.RelatorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * dados agregados da tela de relatorios. so admin chega aqui - o SecurityConfig
 * ja barra a rota por role, exigirAdmin aqui e o cinto alem do suspensorio.
 */
@Service
public class RelatorioService {

    @Autowired
    private RelatorioRepository repository;

    public void exigirAdmin(Usuario logado) {
        if (!logado.isAdmin()) {
            throw new PermissaoNegadaException("Requer perfil de administrador.");
        }
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
