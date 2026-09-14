package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.FrequenciaRequest;
import dev.matheus.cadastroBolsistas.dto.FrequenciaResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ComprovanteFrequenciaPdfService;
import dev.matheus.cadastroBolsistas.service.FrequenciaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "Frequência & Horas", description = "Controle de apontamento de horas, relatórios de produtividade, exportação CSV e emissão de comprovantes em PDF.")
@RestController
@RequestMapping("/api/frequencias")
public class FrequenciaApiController {

    private static final int TAMANHO_PAGINA = 10;

    private final FrequenciaService frequenciaService;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;
    private final UsuarioLogado usuarioLogado;
    private final AuditoriaService auditoriaService;
    private final ComprovanteFrequenciaPdfService comprovantePdfService;
    private final ProfessorService professorService;

    public FrequenciaApiController(FrequenciaService frequenciaService, BolsistaService bolsistaService,
                                   LaboratorioService laboratorioService, UsuarioLogado usuarioLogado,
                                   AuditoriaService auditoriaService,
                                   ComprovanteFrequenciaPdfService comprovantePdfService,
                                   ProfessorService professorService) {
        this.frequenciaService = frequenciaService;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
        this.usuarioLogado = usuarioLogado;
        this.auditoriaService = auditoriaService;
        this.comprovantePdfService = comprovantePdfService;
        this.professorService = professorService;
    }

    @Operation(summary = "Listar frequências paginadas", description = "Retorna apontamentos de frequência com suporte a filtros de data e bolsista. Bolsistas visualizam somente seus próprios registros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de frequências"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public PaginaResponse<FrequenciaResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "ID do bolsista (UUID)") @RequestParam(required = false) UUID bolsistaId,
            @Parameter(description = "Data de início do intervalo", example = "2026-08-01") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data de término do intervalo", example = "2026-08-31") @RequestParam(required = false) LocalDate dataFim) {
        Usuario logado = usuarioLogado.obrigatorio();
        UUID filtro = logado.isBolsista() ? logado.getId() : bolsistaId;

        if (filtro != null) {
            exigirPermissao(logado, filtro);
        }

        int total;
        List<Frequencia> pagina1;
        int atual;

        if (filtro == null && logado.isProfessor()) {
            List<UUID> ids = idsDosMeusBolsistas(logado);
            total = frequenciaService.contarPorBolsistas(ids, dataInicio, dataFim);
            atual = paginaValida(pagina, total);
            pagina1 = frequenciaService.buscarPorBolsistas(ids, dataInicio, dataFim, TAMANHO_PAGINA, (atual - 1) * TAMANHO_PAGINA);
        } else {
            total = frequenciaService.contarFrequencias(filtro, dataInicio, dataFim);
            atual = paginaValida(pagina, total);
            pagina1 = frequenciaService.buscarFrequencias(filtro, dataInicio, dataFim, TAMANHO_PAGINA, (atual - 1) * TAMANHO_PAGINA);
        }

        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) TAMANHO_PAGINA));
        return new PaginaResponse<>(pagina1.stream().map(FrequenciaResponse::de).toList(), atual, totalPaginas, total);
    }

    @Operation(summary = "Resumo mensal de horas", description = "Calcula o total de horas trabalhadas no mês vigente e o histórico total acumulado do bolsista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo de horas calculado")
    })
    @GetMapping("/resumo")
    public Map<String, Double> resumo(@Parameter(description = "ID do bolsista (UUID)") @RequestParam(required = false) UUID bolsistaId) {
        Usuario logado = usuarioLogado.obrigatorio();
        UUID alvo = logado.isBolsista() ? logado.getId()
                 : (bolsistaId != null ? bolsistaId : logado.getId());
        exigirPermissao(logado, alvo);

        List<Frequencia> todas = frequenciaService.listarPorBolsista(alvo);
        LocalDate hoje = LocalDate.now();
        double mes = todas.stream()
                .filter(f -> f.getData() != null
                        && f.getData().getMonthValue() == hoje.getMonthValue()
                        && f.getData().getYear() == hoje.getYear())
                .mapToDouble(Frequencia::getHorasTrabalhadas).sum();
        double total = todas.stream().mapToDouble(Frequencia::getHorasTrabalhadas).sum();
        return Map.of("horasMes", mes, "horasTotal", total);
    }

    @Operation(summary = "Exportar frequências em CSV", description = "Gera um arquivo CSV contendo os apontamentos de frequência filtrados por intervalo de datas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo CSV gerado")
    })
    @GetMapping("/exportar")
    public void exportar(
            @Parameter(description = "ID do bolsista") @RequestParam(required = false) UUID bolsistaId,
            @Parameter(description = "Data inicial") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final") @RequestParam(required = false) LocalDate dataFim,
            HttpServletResponse response) throws java.io.IOException {
        Usuario logado = usuarioLogado.obrigatorio();
        UUID filtro = logado.isBolsista() ? logado.getId() : bolsistaId;
        if (filtro != null) {
            exigirPermissao(logado, filtro);
        }

        List<Frequencia> lista = (filtro == null && logado.isProfessor())
                ? frequenciaService.buscarPorBolsistas(idsDosMeusBolsistas(logado), dataInicio, dataFim, null, null)
                : frequenciaService.buscarFrequencias(filtro, dataInicio, dataFim, null, null);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=frequencias.csv");
        try (java.io.PrintWriter writer = response.getWriter()) {
            writer.println("ID,Bolsista,Data,Horas Trabalhadas,Descricao,LinkComprovante");
            for (Frequencia f : lista) {
                writer.println(String.join(",",
                        String.valueOf(f.getId()),
                        csv(f.getNomeBolsista()),
                        f.getData() != null ? f.getData().toString() : "",
                        String.valueOf(f.getHorasTrabalhadas()),
                        csv(f.getDescricao()),
                        csv(f.getLinkComprovante())));
            }
        }
    }

    private static String csv(String valor) {
        if (valor == null) {
            return "";
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    @Operation(summary = "Emitir comprovante de frequência em PDF", description = "Gera documento PDF formatado com dados cadastrais do bolsista, laboratório, orientador, tabela zebrada de horas e campos para assinatura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprovante oficial em PDF"),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/comprovante-pdf")
    public void comprovantePdf(
            @Parameter(description = "ID do bolsista") @RequestParam(required = false) UUID bolsistaId,
            @Parameter(description = "Data inicial de referência") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final de referência") @RequestParam(required = false) LocalDate dataFim,
            HttpServletResponse response) throws java.io.IOException {
        Usuario logado = usuarioLogado.obrigatorio();
        UUID alvo = resolverBolsistaAlvo(logado, bolsistaId);
        exigirPermissao(logado, alvo);

        Bolsista b = bolsistaService.buscarPorId(alvo);
        if (b == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsista nao encontrado.");
        }

        Laboratorio lab = b.getLaboratorioId() != null ? laboratorioService.buscarPorId(b.getLaboratorioId()) : null;
        Professor coord = (lab != null && lab.getCoordenadorId() != null) ? professorService.buscarPorId(lab.getCoordenadorId()) : null;

        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now().withDayOfMonth(1);
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Frequencia> frequencias = frequenciaService.buscarFrequencias(alvo, inicio, fim, null, null);

        try {
            byte[] pdfBytes = comprovantePdfService.gerarComprovante(b, lab, coord, frequencias, inicio, fim);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=comprovante_frequencia_" + b.getMatricula() + ".pdf");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

            auditoriaService.registrar(logado, "EMISSAO_COMPROVANTE_PDF", "FREQUENCIA", "Comprovante PDF emitido para bolsista " + b.getNome() + " referente ao período " + inicio + " a " + fim + ".", null);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar PDF do comprovante: " + e.getMessage());
        }
    }

    @Operation(summary = "Buscar frequência por ID", description = "Retorna um apontamento de frequência individual pelo identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da frequência", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public FrequenciaResponse buscar(@Parameter(description = "ID da frequência (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Frequencia f = exigirFrequencia(id);
        exigirPermissao(logado, f.getBolsistaId());
        return FrequenciaResponse.de(f);
    }

    @Operation(summary = "Registrar novo apontamento de frequência", description = "Aponta horas trabalhadas e descrição das atividades realizadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Frequência registrada com sucesso", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<FrequenciaResponse> registrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do apontamento de horas", required = true)
            @Valid @RequestBody FrequenciaRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();

        UUID alvo = resolverBolsistaAlvo(logado, body.bolsistaId());
        exigirPermissao(logado, alvo);

        Frequencia f = new Frequencia();
        f.setBolsistaId(alvo);
        f.setData(body.data());
        f.setHorasTrabalhadas(body.horasTrabalhadas());
        f.setDescricao(StringUtil.limpar(body.descricao()));
        f.setLinkComprovante(StringUtil.limpar(body.linkComprovante()));
        frequenciaService.registrar(f);
        auditoriaService.registrar(logado, "REGISTRAR_FREQUENCIA", "FREQUENCIA", "Apontamento de " + f.getHorasTrabalhadas() + "h para o dia " + f.getData() + ".", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(FrequenciaResponse.de(f));
    }

    @Operation(summary = "Atualizar apontamento de frequência", description = "Altera as horas, data, descrição ou link de entregável de um registro de frequência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frequência atualizada com sucesso", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public FrequenciaResponse atualizar(
            @Parameter(description = "ID da frequência (UUID)", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados da frequência", required = true)
            @Valid @RequestBody FrequenciaRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        Frequencia f = exigirFrequencia(id);
        exigirPermissao(logado, f.getBolsistaId());

        f.setData(body.data());
        f.setHorasTrabalhadas(body.horasTrabalhadas());
        f.setDescricao(StringUtil.limpar(body.descricao()));
        f.setLinkComprovante(StringUtil.limpar(body.linkComprovante()));
        frequenciaService.atualizar(f);
        auditoriaService.registrar(logado, "ATUALIZAR_FREQUENCIA", "FREQUENCIA", "Apontamento de frequência atualizado (" + f.getHorasTrabalhadas() + "h em " + f.getData() + ").", null);
        return FrequenciaResponse.de(f);
    }

    @Operation(summary = "Desativar frequência (Soft Delete)", description = "Desativa um registro de apontamento de horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Frequência desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID da frequência (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Frequencia f = exigirFrequencia(id);
        exigirPermissao(logado, f.getBolsistaId());
        frequenciaService.excluir(id);
        auditoriaService.registrar(logado, "EXCLUIR_FREQUENCIA", "FREQUENCIA", "Registro de frequência de " + f.getHorasTrabalhadas() + "h do dia " + f.getData() + " desativado.", null);
        return ResponseEntity.noContent().build();
    }

    private int paginaValida(int pedida, int total) {
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) TAMANHO_PAGINA));
        return Math.min(Math.max(pedida, 1), totalPaginas);
    }

    private List<UUID> idsDosMeusBolsistas(Usuario professor) {
        return laboratorioService.listarPorCoordenador(professor.getId()).stream()
                .flatMap(lab -> bolsistaService.buscarPorLaboratorio(lab.getId()).stream())
                .map(Usuario::getId)
                .toList();
    }

    private UUID resolverBolsistaAlvo(Usuario logado, UUID bolsistaId) {
        if (logado.isBolsista()) {
            return logado.getId();
        }
        if (bolsistaId == null) {
            throw new IllegalArgumentException("Informe o bolsista para o qual o registro esta sendo feito.");
        }
        return bolsistaId;
    }

    private void exigirPermissao(Usuario logado, UUID bolsistaId) {
        if (logado.isAdmin()) {
            return;
        }
        if (Objects.equals(logado.getId(), bolsistaId)) {
            return;
        }
        if (logado.isProfessor()) {
            Bolsista b = bolsistaService.buscarPorId(bolsistaId);
            if (b != null && b.getLaboratorioId() != null
                    && laboratorioService.podeGerenciar(logado, b.getLaboratorioId())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissao para acessar as frequencias deste usuario.");
    }

    private Frequencia exigirFrequencia(UUID id) {
        Frequencia f = frequenciaService.buscarPorId(id);
        if (f == null || !f.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de frequencia nao encontrado.");
        }
        return f;
    }

}
