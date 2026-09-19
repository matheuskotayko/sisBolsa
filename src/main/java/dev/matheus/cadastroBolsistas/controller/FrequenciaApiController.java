package dev.matheus.cadastroBolsistas.controller;

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
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Frequência & Horas", description = "Controle de apontamento de horas, relatórios de produtividade, exportação CSV e emissão de comprovantes em PDF.")
@RestController
@RequestMapping("/api/v1/frequencias")
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
            frequenciaService.exigirAcesso(logado, filtro);
        }

        int total;
        List<Frequencia> pagina1;
        int atual;

        if (filtro == null && logado.isProfessor()) {
            List<UUID> ids = bolsistaService.idsDosBolsistasCoordenadosPor(logado.getId());
            total = frequenciaService.contarPorBolsistas(ids, dataInicio, dataFim);
            atual = PaginacaoUtil.paginaValida(pagina, PaginacaoUtil.totalPaginas(total, TAMANHO_PAGINA));
            pagina1 = frequenciaService.buscarPorBolsistas(ids, dataInicio, dataFim, TAMANHO_PAGINA, (atual - 1) * TAMANHO_PAGINA);
        } else {
            total = frequenciaService.contarFrequencias(filtro, dataInicio, dataFim);
            atual = PaginacaoUtil.paginaValida(pagina, PaginacaoUtil.totalPaginas(total, TAMANHO_PAGINA));
            pagina1 = frequenciaService.buscarFrequencias(filtro, dataInicio, dataFim, TAMANHO_PAGINA, (atual - 1) * TAMANHO_PAGINA);
        }

        int totalPaginas = PaginacaoUtil.totalPaginas(total, TAMANHO_PAGINA);
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
        frequenciaService.exigirAcesso(logado, alvo);

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
            @ApiResponse(responseCode = "200", description = "Arquivo CSV gerado", content = @Content(mediaType = "text/csv"))
    })
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @Parameter(description = "ID do bolsista") @RequestParam(required = false) UUID bolsistaId,
            @Parameter(description = "Data inicial") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final") @RequestParam(required = false) LocalDate dataFim) {
        Usuario logado = usuarioLogado.obrigatorio();
        UUID filtro = logado.isBolsista() ? logado.getId() : bolsistaId;
        if (filtro != null) {
            frequenciaService.exigirAcesso(logado, filtro);
        }

        List<Frequencia> lista = (filtro == null && logado.isProfessor())
                ? frequenciaService.buscarPorBolsistas(bolsistaService.idsDosBolsistasCoordenadosPor(logado.getId()), dataInicio, dataFim, null, null)
                : frequenciaService.buscarFrequencias(filtro, dataInicio, dataFim, null, null);

        StringBuilder sb = new StringBuilder("ID,Bolsista,Data,Horas Trabalhadas,Descricao,LinkComprovante\n");
        for (Frequencia f : lista) {
            sb.append(String.join(",",
                    String.valueOf(f.getId()),
                    CsvUtil.escapar(f.getNomeBolsista()),
                    f.getData() != null ? f.getData().toString() : "",
                    String.valueOf(f.getHorasTrabalhadas()),
                    CsvUtil.escapar(f.getDescricao()),
                    CsvUtil.escapar(f.getLinkComprovante()))).append("\n");
        }

        return ArquivoDownloadUtil.csv("frequencias.csv", sb.toString());
    }

    @Operation(summary = "Emitir comprovante de frequência em PDF", description = "Gera documento PDF formatado com dados cadastrais do bolsista, laboratório, orientador, tabela zebrada de horas e campos para assinatura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprovante oficial em PDF", content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/comprovante-pdf")
    public ResponseEntity<byte[]> comprovantePdf(
            @Parameter(description = "ID do bolsista") @RequestParam(required = false) UUID bolsistaId,
            @Parameter(description = "Data inicial de referência") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final de referência") @RequestParam(required = false) LocalDate dataFim) {
        Usuario logado = usuarioLogado.obrigatorio();
        UUID alvo = frequenciaService.resolverBolsistaAlvo(logado, bolsistaId);
        frequenciaService.exigirAcesso(logado, alvo);
        Bolsista b = bolsistaService.buscarOuFalhar(alvo);

        Laboratorio lab = b.getLaboratorioId() != null ? laboratorioService.buscarPorId(b.getLaboratorioId()) : null;
        Professor coord = (lab != null && lab.getCoordenadorId() != null) ? professorService.buscarPorId(lab.getCoordenadorId()) : null;

        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now().withDayOfMonth(1);
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Frequencia> frequencias = frequenciaService.buscarFrequencias(alvo, inicio, fim, null, null);

        try {
            byte[] pdfBytes = comprovantePdfService.gerarComprovante(b, lab, coord, frequencias, inicio, fim);
            auditoriaService.registrar(logado, "EMISSAO_COMPROVANTE_PDF", "FREQUENCIA", "Comprovante PDF emitido para bolsista " + b.getNome() + " referente ao período " + inicio + " a " + fim + ".", null);
            return ArquivoDownloadUtil.pdf("comprovante_frequencia_" + b.getMatricula() + ".pdf", pdfBytes);
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
    public EntityModel<FrequenciaResponse> buscar(@Parameter(description = "ID da frequência (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Frequencia f = frequenciaService.buscarComPermissao(id, logado);
        return comLinks(FrequenciaResponse.de(f));
    }

    @Operation(summary = "Registrar novo apontamento de frequência", description = "Aponta horas trabalhadas e descrição das atividades realizadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Frequência registrada com sucesso", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<FrequenciaResponse>> registrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do apontamento de horas", required = true)
            @Valid @RequestBody FrequenciaRequest body,
            UriComponentsBuilder uriBuilder) {
        Usuario logado = usuarioLogado.obrigatorio();

        UUID alvo = frequenciaService.resolverBolsistaAlvo(logado, body.bolsistaId());
        frequenciaService.exigirAcesso(logado, alvo);

        Frequencia f = new Frequencia();
        f.setBolsistaId(alvo);
        f.setData(body.data());
        f.setHorasTrabalhadas(body.horasTrabalhadas());
        f.setDescricao(StringUtil.limpar(body.descricao()));
        f.setLinkComprovante(StringUtil.limpar(body.linkComprovante()));
        frequenciaService.registrar(f);
        auditoriaService.registrar(logado, "REGISTRAR_FREQUENCIA", "FREQUENCIA", "Apontamento de " + f.getHorasTrabalhadas() + "h para o dia " + f.getData() + ".", null);
        URI uri = uriBuilder.replacePath("/api/v1/frequencias/{id}").buildAndExpand(f.getId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(FrequenciaResponse.de(f)));
    }

    @Operation(summary = "Atualizar apontamento de frequência", description = "Altera as horas, data, descrição ou link de entregável de um registro de frequência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frequência atualizada com sucesso", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public EntityModel<FrequenciaResponse> atualizar(
            @Parameter(description = "ID da frequência (UUID)", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados da frequência", required = true)
            @Valid @RequestBody FrequenciaRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        Frequencia f = frequenciaService.buscarComPermissao(id, logado);

        f.setData(body.data());
        f.setHorasTrabalhadas(body.horasTrabalhadas());
        f.setDescricao(StringUtil.limpar(body.descricao()));
        f.setLinkComprovante(StringUtil.limpar(body.linkComprovante()));
        frequenciaService.atualizar(f);
        auditoriaService.registrar(logado, "ATUALIZAR_FREQUENCIA", "FREQUENCIA", "Apontamento de frequência atualizado (" + f.getHorasTrabalhadas() + "h em " + f.getData() + ").", null);
        return comLinks(FrequenciaResponse.de(f));
    }

    @Operation(summary = "Desativar frequência (Soft Delete)", description = "Desativa um registro de apontamento de horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Frequência desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID da frequência (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Frequencia f = frequenciaService.buscarComPermissao(id, logado);
        frequenciaService.excluir(id);
        auditoriaService.registrar(logado, "EXCLUIR_FREQUENCIA", "FREQUENCIA", "Registro de frequência de " + f.getHorasTrabalhadas() + "h do dia " + f.getData() + " desativado.", null);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<FrequenciaResponse> comLinks(FrequenciaResponse resp) {
        return EntityModel.of(resp,
                Link.of("/api/v1/frequencias/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/bolsistas/" + resp.bolsistaId()).withRel("bolsista"));
    }
}
