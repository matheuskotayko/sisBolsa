package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.common.ArquivoDownloadUtil;
import dev.matheus.cadastroBolsistas.dto.request.BolsistaRequest;
import dev.matheus.cadastroBolsistas.dto.response.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.response.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.response.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Cargo;
import dev.matheus.cadastroBolsistas.model.ModalidadeBolsa;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "Bolsista", description = "Gestão de bolsistas, dados acadêmicos, modalidades de bolsa e projetos vinculados.")
@RestController
@RequestMapping("/api/v1/bolsista")
@SecurityRequirement(name = "bearerAuth")
public class BolsistaController {

    private final BolsistaService bolsistaService;
    private final ProjetoService projetoService;

    public BolsistaController(BolsistaService bolsistaService, ProjetoService projetoService) {
        this.bolsistaService = bolsistaService;
        this.projetoService = projetoService;
    }

    @Operation(summary = "Listar bolsistas paginados", description = "Retorna a listagem de bolsistas de acordo com o escopo do usuário autenticado: ADMIN visualiza todos, PROFESSOR visualiza os bolsistas dos laboratórios que coordena, e BOLSISTA visualiza seus colegas de laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de bolsistas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @ParameterObject Pageable pageable,
            @Parameter(description = "Filtro por perfil (BOLSISTA)", example = "BOLSISTA") @RequestParam(required = false) String tipo,
            @Parameter(description = "Filtro de busca textual por nome", example = "Lucas") @RequestParam(required = false) String buscaNome,
            @Parameter(description = "Filtro de busca textual por curso", example = "Engenharia") @RequestParam(required = false) String buscaCurso) {
        return ResponseEntity.ok(bolsistaService.listar(usuario, buscaNome, buscaCurso, tipo, pageable).map(UsuarioResponse::from));
    }

    @Operation(summary = "Buscar bolsista por ID", description = "Recupera as informações detalhadas de um bolsista pelo seu identificador público ou UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do bolsista", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para ver este bolsista", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(
            @Parameter(description = "ID público do bolsista", required = true, example = "bol_k8s2M4n9P1q3W5z7A0b2") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        Bolsista b = bolsistaService.buscarComPermissaoDeVisualizacao(id, usuario);
        return ResponseEntity.ok(UsuarioResponse.from(b));
    }

    @Operation(summary = "Listar cargos disponíveis", description = "Retorna todos os cargos cadastrados para bolsistas nos laboratórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cargos")
    })
    @GetMapping("/cargos")
    public ResponseEntity<List<Map<String, String>>> cargos() {
        return ResponseEntity.ok(java.util.Arrays.stream(Cargo.values())
                .map(c -> Map.of("valor", c.name(), "descricao", c.getDescricao()))
                .toList());
    }

    @Operation(summary = "Listar modalidades de bolsa", description = "Retorna todas as modalidades de bolsa suportadas pelo sistema (PIBIC, PIBITI, Extensão, Monitoria, etc.).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de modalidades de bolsa")
    })
    @GetMapping("/modalidades")
    public ResponseEntity<List<Map<String, String>>> modalidades() {
        return ResponseEntity.ok(java.util.Arrays.stream(ModalidadeBolsa.values())
                .map(m -> Map.of("valor", m.name(), "descricao", m.getDescricao()))
                .toList());
    }

    @Operation(summary = "Exportar bolsistas em formato CSV", description = "Gera planilha CSV com todos os dados cadastrais dos bolsistas de acordo com o escopo do usuário autenticado (restrito a ADMIN e PROFESSOR).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo CSV para download", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "403", description = "Acesso negado para bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/exportar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Usuario usuario) {
        String csv = bolsistaService.gerarCsv(usuario);
        return ArquivoDownloadUtil.csv("bolsistas.csv", csv);
    }

    @Operation(summary = "Listar projetos vinculados a um bolsista", description = "Retorna todos os projetos de pesquisa dos quais o bolsista é membro ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos vinculados"),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/projetos")
    public ResponseEntity<List<ProjetoResponse>> projetos(
            @Parameter(description = "ID público do bolsista", required = true, example = "bol_k8s2M4n9P1q3W5z7A0b2") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        bolsistaService.buscarComPermissaoDeVisualizacao(id, usuario);
        return ResponseEntity.ok(projetoService.listarPorBolsista(id).stream().map(ProjetoResponse::from).toList());
    }

    @Operation(summary = "Cadastrar novo bolsista", description = "Cria um novo bolsista no sistema. Administradores podem cadastrar qualquer um; Professores podem cadastrar bolsistas para os laboratórios que coordenam.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bolsista cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para cadastrar bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<UsuarioResponse> criar(
            @AuthenticationPrincipal Usuario usuario,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do bolsista a ser cadastrado", required = true)
            @Valid @RequestBody BolsistaRequest body,
            UriComponentsBuilder uriBuilder) {
        Bolsista b = bolsistaService.criar(body, usuario);
        URI uri = uriBuilder.replacePath("/api/v1/bolsista/{id}").buildAndExpand(b.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(UsuarioResponse.from(b));
    }

    @Operation(summary = "Atualizar bolsista", description = "Atualiza os dados do bolsista. Se a senha vier em branco, a atual é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bolsista atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar este bolsista", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "ID público do bolsista a atualizar", required = true, example = "bol_k8s2M4n9P1q3W5z7A0b2") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do bolsista", required = true)
            @Valid @RequestBody BolsistaRequest body) {
        Bolsista b = bolsistaService.atualizar(id, body, usuario);
        return ResponseEntity.ok(UsuarioResponse.from(b));
    }

    @Operation(summary = "Desativar bolsista (Soft Delete)", description = "Desativa um bolsista, mantendo o histórico de frequência e projetos íntegros no banco de dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bolsista desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para desativar este bolsista", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID público do bolsista a desativar", required = true, example = "bol_k8s2M4n9P1q3W5z7A0b2") @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        bolsistaService.excluir(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
