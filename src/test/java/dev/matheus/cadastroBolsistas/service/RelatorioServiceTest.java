package dev.matheus.cadastroBolsistas.service;

import dev.matheus.cadastroBolsistas.dto.ResumoGeralResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.repository.RelatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private RelatorioRepository repository;

    @Mock
    private BolsistaService bolsistaService;

    @Mock
    private LaboratorioService laboratorioService;

    @Mock
    private ProjetoService projetoService;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    @DisplayName("obterResumoGeral deve consolidar contagens dos serviços")
    void obterResumoGeral_DeveRetornarTotaisCorretos() {
        ArrayList<Bolsista> bolsistas = new ArrayList<>(List.of(new Bolsista(), new Bolsista(), new Bolsista()));
        ArrayList<Laboratorio> laboratorios = new ArrayList<>(List.of(new Laboratorio()));
        ArrayList<Projeto> projetos = new ArrayList<>(List.of(new Projeto(), new Projeto()));

        when(bolsistaService.listarTodos()).thenReturn(bolsistas);
        when(laboratorioService.listarTodos()).thenReturn(laboratorios);
        when(projetoService.listarTodos()).thenReturn(projetos);

        ResumoGeralResponse resumo = relatorioService.obterResumoGeral();

        assertThat(resumo).isNotNull();
        assertThat(resumo.totalBolsistas()).isEqualTo(3);
        assertThat(resumo.totalLaboratorios()).isEqualTo(1);
        assertThat(resumo.totalProjetos()).isEqualTo(2);

        verify(bolsistaService).listarTodos();
        verify(laboratorioService).listarTodos();
        verify(projetoService).listarTodos();
    }

    @Test
    @DisplayName("getHorasBolsistasMesCorrente deve delegar para o repositório")
    void getHorasBolsistasMesCorrente_DeveDelegarParaRepositorio() {
        when(repository.horasBolsistasMesCorrente()).thenReturn(List.of());

        List<RelatorioRepository.HorasBolsista> resultado = relatorioService.getHorasBolsistasMesCorrente();

        assertThat(resultado).isNotNull();
        verify(repository).horasBolsistasMesCorrente();
    }

    @Test
    @DisplayName("getProjetosAtivosPorLaboratorio deve delegar para o repositório")
    void getProjetosAtivosPorLaboratorio_DeveDelegarParaRepositorio() {
        when(repository.projetosAtivosPorLaboratorio()).thenReturn(List.of());

        List<RelatorioRepository.ProjetosPorLaboratorio> resultado = relatorioService.getProjetosAtivosPorLaboratorio();

        assertThat(resultado).isNotNull();
        verify(repository).projetosAtivosPorLaboratorio();
    }

    @Test
    @DisplayName("getBolsistasPorCargo deve delegar para o repositório")
    void getBolsistasPorCargo_DeveDelegarParaRepositorio() {
        when(repository.bolsistasPorCargo()).thenReturn(List.of());

        List<RelatorioRepository.BolsistasPorCargo> resultado = relatorioService.getBolsistasPorCargo();

        assertThat(resultado).isNotNull();
        verify(repository).bolsistasPorCargo();
    }

    @Test
    @DisplayName("getLaboratoriosOcupacao deve delegar para o repositório")
    void getLaboratoriosOcupacao_DeveDelegarParaRepositorio() {
        when(repository.laboratoriosOcupacao()).thenReturn(List.of());

        List<RelatorioRepository.OcupacaoLaboratorio> resultado = relatorioService.getLaboratoriosOcupacao();

        assertThat(resultado).isNotNull();
        verify(repository).laboratoriosOcupacao();
    }
}
