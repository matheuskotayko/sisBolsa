package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.AuditoriaResponse;
import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.model.Auditoria;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.util.ArquivoDownloadUtil;
import dev.matheus.cadastroBolsistas.util.CsvUtil;
import dev.matheus.cadastroBolsistas.util.PaginacaoUtil;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "Auditoria", description = "Trilha de auditoria para rastreamento de acessos, alterações de cadastros, logins e emissões de comprovantes.")
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaApiController {

    private static final int TAMANHO_PAGINA = 15;

    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public AuditoriaApiController(AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    @Operation(summary = "Listar logs de auditoria paginados", description = "Retorna o histórico de atividades e logs de segurança filtrados por entidade, tipo de ação ou intervalo de datas (restrito a Administradores e Professores).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de logs"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public PaginaResponse<AuditoriaResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Filtro por entidade afetada", example = "AUTH") @RequestParam(required = false) String entidade,
            @Parameter(description = "Filtro por ação realizada", example = "LOGIN") @RequestParam(required = false) String acao,
            @Parameter(description = "Data de início", example = "2026-08-01") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data de término", example = "2026-08-31") @RequestParam(required = false) LocalDate dataFim) {
        Usuario logado = usuarioLogado.obrigatorio();
        auditoriaService.exigirAcesso(logado);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        String ent = StringUtil.estaVazio(entidade) ? null : entidade.trim();
        String ac = StringUtil.estaVazio(acao) ? null : acao.trim();

        int total = auditoriaService.contarLogs(ent, ac, inicio, fim);
        int totalPaginas = PaginacaoUtil.totalPaginas(total, TAMANHO_PAGINA);
        int atual = PaginacaoUtil.paginaValida(pagina, totalPaginas);

        List<Auditoria> itens = auditoriaService.buscarLogs(ent, ac, inicio, fim, TAMANHO_PAGINA, (atual - 1) * TAMANHO_PAGINA);
        return new PaginaResponse<>(itens.stream().map(AuditoriaResponse::de).toList(), atual, totalPaginas, total);
    }

    @Operation(summary = "Exportar logs de auditoria em CSV", description = "Gera um relatório em arquivo CSV contendo os logs de auditoria filtrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo CSV gerado", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "403", description = "Acesso negado para bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @Parameter(description = "Filtro por entidade afetada") @RequestParam(required = false) String entidade,
            @Parameter(description = "Filtro por ação") @RequestParam(required = false) String acao,
            @Parameter(description = "Data inicial") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final") @RequestParam(required = false) LocalDate dataFim) {
        Usuario logado = usuarioLogado.obrigatorio();
        auditoriaService.exigirAcesso(logado);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        String ent = StringUtil.estaVazio(entidade) ? null : entidade.trim();
        String ac = StringUtil.estaVazio(acao) ? null : acao.trim();

        List<Auditoria> lista = auditoriaService.buscarLogs(ent, ac, inicio, fim, null, null);

        StringBuilder sb = new StringBuilder("Data/Hora,Usuario,Acao,Entidade,Detalhes,IP\n");
        for (Auditoria a : lista) {
            sb.append(String.join(",",
                    a.getDataHora() != null ? a.getDataHora().toString() : "",
                    CsvUtil.escapar(a.getUsuarioNome()),
                    CsvUtil.escapar(a.getAcao()),
                    CsvUtil.escapar(a.getEntidade()),
                    CsvUtil.escapar(a.getDetalhes()),
                    CsvUtil.escapar(a.getIpOrigem()))).append("\n");
        }

        return ArquivoDownloadUtil.csv("auditoria.csv", sb.toString());
    }
}
