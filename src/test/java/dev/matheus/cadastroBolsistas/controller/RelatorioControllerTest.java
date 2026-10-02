package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.ResumoGeralResponse;
import dev.matheus.cadastroBolsistas.repository.RelatorioRepository;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.RelatorioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelatorioController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class RelatorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService relatorioService;

    @Test
    @DisplayName("GET /api/v1/relatorio/resumo deve retornar ResumoGeralResponse com status 200")
    void resumo_DeveRetornar200EObjetoTipado() throws Exception {
        when(relatorioService.obterResumoGeral()).thenReturn(new ResumoGeralResponse(10, 3, 5));

        mockMvc.perform(get("/api/v1/relatorio/resumo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBolsistas").value(10))
                .andExpect(jsonPath("$.totalLaboratorios").value(3))
                .andExpect(jsonPath("$.totalProjetos").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/relatorio/horas-mes deve retornar 200")
    void horasDoMes_DeveRetornar200() throws Exception {
        when(relatorioService.getHorasBolsistasMesCorrente()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/relatorio/horas-mes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/relatorio/projetos-por-laboratorio deve retornar 200")
    void projetosPorLaboratorio_DeveRetornar200() throws Exception {
        when(relatorioService.getProjetosAtivosPorLaboratorio()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/relatorio/projetos-por-laboratorio"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/relatorio/bolsistas-por-cargo deve retornar 200")
    void bolsistasPorCargo_DeveRetornar200() throws Exception {
        when(relatorioService.getBolsistasPorCargo()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/relatorio/bolsistas-por-cargo"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/relatorio/ocupacao deve retornar 200")
    void ocupacao_DeveRetornar200() throws Exception {
        when(relatorioService.getLaboratoriosOcupacao()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/relatorio/ocupacao"))
                .andExpect(status().isOk());
    }
}
