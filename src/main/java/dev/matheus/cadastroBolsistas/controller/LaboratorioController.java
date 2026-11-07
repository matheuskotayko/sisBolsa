package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.response.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.request.LaboratorioRequest;
import dev.matheus.cadastroBolsistas.dto.response.LaboratorioResponse;
import dev.matheus.cadastroBolsistas.dto.response.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.response.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Laboratório", description = "Gerenciamento de laboratórios de pesquisa, equipe alocada, capacidade e cálculo de ocupação.")
@RestController
@RequestMapping("/api/v1/laboratorio")
@SecurityRequirement(name = "bearerAuth")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;
    private final BolsistaService bolsistaService;
    private final ProjetoService projetoService;

    public LaboratorioController(LaboratorioService laboratorioService, BolsistaService bolsistaService,
                                 ProjetoService projetoService) {
        this.laboratorioService = laboratorioService;
        this.bolsistaService = bolsistaService;
        this.projetoService = projetoService;
    }

    @Operation(summary = "Listar laboratórios paginados", description = "Retorna os laboratórios cadastrados com capacidade e ocupação calculadas. Professores visualizam somente os laboratórios que coordenam.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de laboratórios"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<LaboratorioResponse>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @ParameterObject Pageable pageable,
            @Parameter(description = "Filtro de busca por nome, área de pesquisa ou coordenador", example = "Inteligência") @RequestParam(required = false) String buscaNome) {
        return ResponseEntity.ok(laboratorioService.listar(usuario, buscaNome, pageable).map(this::comOcupacao));
    }

    @Operation(summary = "Buscar laboratório por ID", description = "Retorna os detalhes de um laboratório específico incluindo capacidade e percentual de ocupação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes do laboratório", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<LaboratorioResponse> buscar(
            @Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        return ResponseEntity.ok(comOcupacao(laboratorioService.buscarOuFalhar(id)));
    }

    @Operation(summary = "Listar bolsistas de um laboratório", description = "Retorna a lista completa de bolsistas e pesquisadores vinculados ao laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de bolsistas do laboratório"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/bolsistas")
    public ResponseEntity<List<UsuarioResponse>> bolsistas(
            @Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        laboratorioService.buscarOuFalhar(id);
        return ResponseEntity.ok(bolsistaService.buscarPorLaboratorio(id).stream().map(UsuarioResponse::from).toList());
    }

    @Operation(summary = "Listar projetos de um laboratório", description = "Retorna os projetos de pesquisa desenvolvidos no âmbito do laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos do laboratório"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/projetos")
    public ResponseEntity<List<ProjetoResponse>> projetos(
            @Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        laboratorioService.buscarOuFalhar(id);
        return ResponseEntity.ok(projetoService.listarPorLaboratorio(id).stream()
                .map(p -> ProjetoResponse.from(p, projetoService.contarMembros(p.getId())))
                .toList());
    }

    @Operation(summary = "Criar laboratório", description = "Cadastra um novo laboratório no sistema (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Laboratório criado com sucesso", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de administrador", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratorioResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do laboratório", required = true)
                                                     @Valid @RequestBody LaboratorioRequest body,
                                                     UriComponentsBuilder uriBuilder) {
        Laboratorio lab = laboratorioService.criar(body);
        URI uri = uriBuilder.replacePath("/api/v1/laboratorio/{id}").buildAndExpand(lab.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(comOcupacao(lab));
    }

    @Operation(summary = "Atualizar laboratório", description = "Atualiza os dados de capacidade, nome, área de pesquisa ou coordenador do laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Laboratório atualizado com sucesso", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar este laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<LaboratorioResponse> atualizar(
            @Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do laboratório", required = true)
            @Valid @RequestBody LaboratorioRequest body) {
        Laboratorio lab = laboratorioService.atualizar(id, body, usuario);
        return ResponseEntity.ok(comOcupacao(lab));
    }

    @Operation(summary = "Desativar laboratório (Soft Delete)", description = "Desativa o laboratório no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Laboratório desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para excluir este laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        laboratorioService.buscarExigindoGerencia(id, usuario);
        laboratorioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private LaboratorioResponse comOcupacao(Laboratorio lab) {
        return LaboratorioResponse.from(lab, laboratorioService.contarBolsistasNoLaboratorio(lab.getId()));
    }
}
