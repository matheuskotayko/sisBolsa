package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.RelatorioRepository;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
import dev.matheus.cadastroBolsistas.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * relatorios sao so para admin. o SecurityConfig ja barra a rota por role,
 * a checagem aqui e o cinto alem do suspensorio.
 */
@Tag(name = "Relatórios & Estatísticas", description = "Métricas consolidadas do sistema e indicadores de desempenho (exclusivo para perfil ADMIN).")
@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioApiController {

    private final RelatorioService relatorioService;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;
    private final ProjetoService projetoService;
    private final UsuarioLogado usuarioLogado;

    public RelatorioApiController(RelatorioService relatorioService, BolsistaService bolsistaService,
                                  LaboratorioService laboratorioService, ProjetoService projetoService,
                                  UsuarioLogado usuarioLogado) {
        this.relatorioService = relatorioService;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
        this.projetoService = projetoService;
        this.usuarioLogado = usuarioLogado;
    }

    @Operation(summary = "Resumo quantitativo global", description = "Retorna o total geral de bolsistas, laboratórios e projetos ativos cadastrados no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contadores globais"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para perfis não administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        exigirAdmin();
        return Map.of(
                "totalBolsistas", bolsistaService.listarTodos().size(),
                "totalLaboratorios", laboratorioService.listarTodos().size(),
                "totalProjetos", projetoService.listarTodos().size());
    }

    @Operation(summary = "Relatório de horas apontadas no mês", description = "Agrupa o total de horas trabalhadas por cada bolsista no mês corrente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de bolsistas e suas horas apontadas"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito ao perfil ADMIN", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/horas-mes")
    public List<RelatorioRepository.HorasBolsista> horasDoMes() {
        exigirAdmin();
        return relatorioService.getHorasBolsistasMesCorrente();
    }

    @Operation(summary = "Relatório de projetos por laboratório", description = "Distribuição da quantidade de projetos ativos alocados em cada laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade de projetos por laboratório"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito ao perfil ADMIN", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/projetos-por-laboratorio")
    public List<RelatorioRepository.ProjetosPorLaboratorio> projetosPorLaboratorio() {
        exigirAdmin();
        return relatorioService.getProjetosAtivosPorLaboratorio();
    }

    @Operation(summary = "Distribuição de bolsistas por cargo", description = "Contagem da distribuição de bolsistas por categoria e cargo de atuação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distribuição de bolsistas por cargo"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito ao perfil ADMIN", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/bolsistas-por-cargo")
    public List<RelatorioRepository.BolsistasPorCargo> bolsistasPorCargo() {
        exigirAdmin();
        return relatorioService.getBolsistasPorCargo();
    }

    @Operation(summary = "Taxa de ocupação de laboratórios", description = "Cálculo detalhado da ocupação e lotação percentual de cada laboratório em relação à capacidade física instalada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatísticas de ocupação dos laboratórios"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito ao perfil ADMIN", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/ocupacao")
    public List<RelatorioRepository.OcupacaoLaboratorio> ocupacao() {
        exigirAdmin();
        return relatorioService.getLaboratoriosOcupacao();
    }

    private void exigirAdmin() {
        Usuario logado = usuarioLogado.obrigatorio();
        usuarioLogado.exigirAdmin(logado);
    }
}
