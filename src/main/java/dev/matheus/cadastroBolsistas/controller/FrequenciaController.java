package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.common.ArquivoDownloadUtil;
import dev.matheus.cadastroBolsistas.dto.response.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.request.FrequenciaRequest;
import dev.matheus.cadastroBolsistas.dto.response.FrequenciaResponse;
import dev.matheus.cadastroBolsistas.dto.response.ResumoHorasResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ComprovanteFrequenciaPdfService;
import dev.matheus.cadastroBolsistas.service.FrequenciaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "Frequência & Horas", description = "Controle de apontamento de horas, relatórios de produtividade, exportação CSV e emissão de comprovantes em PDF.")
@RestController
@RequestMapping("/api/v1/frequencia")
@SecurityRequirement(name = "bearerAuth")
public class FrequenciaController {

    private final FrequenciaService frequenciaService;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;
    private final ComprovanteFrequenciaPdfService comprovantePdfService;
    private final ProfessorService professorService;

    public FrequenciaController(FrequenciaService frequenciaService, BolsistaService bolsistaService,
                                LaboratorioService laboratorioService,
                                ComprovanteFrequenciaPdfService comprovantePdfService,
                                ProfessorService professorService) {
        this.frequenciaService = frequenciaService;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
        this.comprovantePdfService = comprovantePdfService;
        this.professorService = professorService;
    }

    @Operation(summary = "Listar frequências paginadas", description = "Retorna apontamentos de frequência com suporte a filtros de data e bolsista. Bolsistas visualizam somente seus próprios registros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de frequências"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<FrequenciaResponse>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @ParameterObject Pageable pageable,
            @Parameter(description = "ID público do bolsista (ex: bol_...)", example = "bol_a1b2c3d4e5f6g7h8i9j0") @RequestParam(required = false) String bolsistaId,
            @Parameter(description = "Data de início do intervalo") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data de término do intervalo") @RequestParam(required = false) LocalDate dataFim) {
        UUID filtro = null;
        if (usuario.isBolsista()) {
            filtro = usuario.getId();
        } else if (bolsistaId != null && !bolsistaId.isBlank()) {
            filtro = bolsistaService.buscarOuFalhar(bolsistaId).getId();
        }

        if (filtro != null) {
            frequenciaService.exigirAcesso(usuario, filtro);
        }

        return ResponseEntity.ok(frequenciaService.listar(usuario, filtro, dataInicio, dataFim, pageable).map(FrequenciaResponse::from));
    }

    @Operation(summary = "Resumo mensal de horas", description = "Calcula o total de horas trabalhadas no mês vigente e o histórico total acumulado do bolsista.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo de horas calculado", content = @Content(schema = @Schema(implementation = ResumoHorasResponse.class)))
    })
    @GetMapping("/resumo")
    public ResponseEntity<ResumoHorasResponse> resumo(
            @Parameter(description = "ID público do bolsista (ex: bol_...)", example = "bol_a1b2c3d4e5f6g7h8i9j0") @RequestParam(required = false) String bolsistaId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(frequenciaService.calcularResumoHoras(usuario, bolsistaId));
    }

    @Operation(summary = "Exportar frequências em CSV", description = "Gera um arquivo CSV contendo os apontamentos de frequência filtrados por intervalo de datas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo CSV gerado", content = @Content(mediaType = "text/csv"))
    })
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @Parameter(description = "ID público do bolsista (ex: bol_...)", example = "bol_a1b2c3d4e5f6g7h8i9j0") @RequestParam(required = false) String bolsistaId,
            @Parameter(description = "Data inicial") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final") @RequestParam(required = false) LocalDate dataFim,
            @AuthenticationPrincipal Usuario usuario) {
        String csv = frequenciaService.gerarCsv(usuario, bolsistaId, dataInicio, dataFim);
        return ArquivoDownloadUtil.csv("frequencias.csv", csv);
    }

    @Operation(summary = "Emitir comprovante de frequência em PDF", description = "Gera documento PDF formatado com dados cadastrais do bolsista, laboratório, orientador, tabela zebrada de horas e campos para assinatura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprovante oficial em PDF", content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro ao gerar PDF", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/comprovante-pdf")
    public ResponseEntity<byte[]> comprovantePdf(
            @Parameter(description = "ID público do bolsista (ex: bol_...)", example = "bol_a1b2c3d4e5f6g7h8i9j0") @RequestParam(required = false) String bolsistaId,
            @Parameter(description = "Data inicial de referência") @RequestParam(required = false) LocalDate dataInicio,
            @Parameter(description = "Data final de referência") @RequestParam(required = false) LocalDate dataFim,
            @AuthenticationPrincipal Usuario usuario) {
        Bolsista b = frequenciaService.resolverBolsistaAlvo(usuario, bolsistaId);
        frequenciaService.exigirAcesso(usuario, b.getId());

        Laboratorio lab = b.getLaboratorioId() != null ? laboratorioService.buscarPorId(b.getLaboratorioId()) : null;
        Professor coord = (lab != null && lab.getCoordenadorId() != null) ? professorService.buscarPorId(lab.getCoordenadorId()) : null;

        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now().withDayOfMonth(1);
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<Frequencia> frequencias = frequenciaService.buscarFrequencias(b.getId(), inicio, fim, null, null);

        byte[] pdfBytes = comprovantePdfService.gerarComprovante(b, lab, coord, frequencias, inicio, fim);
        return ArquivoDownloadUtil.pdf("comprovante_frequencia_" + Objects.toString(b.getMatricula(), b.getPublicId()) + ".pdf", pdfBytes);
    }

    @Operation(summary = "Buscar frequência por ID", description = "Retorna um apontamento de frequência individual pelo identificador público (ex: frq_...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da frequência", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<FrequenciaResponse> buscar(
            @Parameter(description = "ID público da frequência (ex: frq_...)", required = true, example = "frq_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        Frequencia f = frequenciaService.buscarComPermissao(id, usuario);
        return ResponseEntity.ok(FrequenciaResponse.from(f));
    }

    @Operation(summary = "Registrar novo apontamento de frequência", description = "Aponta horas trabalhadas e descrição das atividades realizadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Frequência registrada com sucesso", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<FrequenciaResponse> registrar(
            @AuthenticationPrincipal Usuario usuario,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do apontamento de horas", required = true)
            @Valid @RequestBody FrequenciaRequest body,
            UriComponentsBuilder uriBuilder) {
        Bolsista alvo = frequenciaService.resolverBolsistaAlvo(usuario, body.bolsistaId());
        frequenciaService.exigirAcesso(usuario, alvo.getId());

        Frequencia f = frequenciaService.registrar(body, alvo);

        URI uri = uriBuilder.replacePath("/api/v1/frequencia/{id}").buildAndExpand(f.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(FrequenciaResponse.from(f));
    }

    @Operation(summary = "Atualizar apontamento de frequência", description = "Altera as horas, data, descrição ou link de entregável de um registro de frequência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frequência atualizada com sucesso", content = @Content(schema = @Schema(implementation = FrequenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<FrequenciaResponse> atualizar(
            @Parameter(description = "ID público da frequência (ex: frq_...)", required = true, example = "frq_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados da frequência", required = true)
            @Valid @RequestBody FrequenciaRequest body) {
        Frequencia f = frequenciaService.buscarComPermissao(id, usuario);

        f.setData(body.data());
        f.setHorasTrabalhadas(body.horasTrabalhadas());
        f.setDescricao(body.descricao());
        f.setLinkComprovante(body.linkComprovante());
        frequenciaService.atualizar(f);
        return ResponseEntity.ok(FrequenciaResponse.from(f));
    }

    @Operation(summary = "Desativar frequência (Soft Delete)", description = "Desativa um registro de apontamento de horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Frequência desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Frequência não encontrada", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID público da frequência (ex: frq_...)", required = true, example = "frq_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        frequenciaService.buscarComPermissao(id, usuario);
        frequenciaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
