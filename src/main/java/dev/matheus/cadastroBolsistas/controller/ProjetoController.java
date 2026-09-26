package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProjetoRequest;
import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
import dev.matheus.cadastroBolsistas.util.PaginacaoUtil;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Projeto", description = "Gerenciamento de projetos de pesquisa, alocação de pesquisadores e entregáveis (repositórios, documentação).")
@RestController
@RequestMapping("/api/v1/projeto")
public class ProjetoController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final ProjetoService projetoService;
    private final BolsistaService bolsistaService;
    private final UsuarioLogado usuarioLogado;

    public ProjetoController(ProjetoService projetoService, BolsistaService bolsistaService,
                             UsuarioLogado usuarioLogado) {
        this.projetoService = projetoService;
        this.bolsistaService = bolsistaService;
        this.usuarioLogado = usuarioLogado;
    }

    @Operation(summary = "Listar projetos paginados", description = "Retorna projetos ativos, com filtros opcionais por nome ou por laboratório de pesquisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de projetos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PaginaResponse<ProjetoResponse>> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Quantidade de itens por página", example = "10") @RequestParam(required = false) Integer tamanho,
            @Parameter(description = "Filtro por nome do projeto", example = "Sistema") @RequestParam(required = false) String buscaNome,
            @Parameter(description = "Filtro por ID do laboratório", example = "lab_a1b2c3d4e5f6g7h8i9j0") @RequestParam(required = false) String labId) {
        usuarioLogado.obrigatorio();
        List<Projeto> lista = projetoService.buscarProjetos(buscaNome, labId);
        return ResponseEntity.ok(PaginacaoUtil.paginar(lista, pagina, tamanho, TAMANHO_PADRAO, TAMANHO_MAXIMO, this::comMembros));
    }

    @Operation(summary = "Buscar projeto por ID", description = "Recupera os detalhes completos do projeto pelo seu identificador público (ex: prj_...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes do projeto", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ProjetoResponse>> buscar(
            @Parameter(description = "ID público do projeto (ex: prj_...)", required = true, example = "prj_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        usuarioLogado.obrigatorio();
        Projeto p = projetoService.buscarOuFalhar(id);
        return ResponseEntity.ok(comLinks(comMembros(p), p.getLaboratorioPublicId()));
    }

    @Operation(summary = "Listar membros de um projeto", description = "Retorna a lista de bolsistas e pesquisadores vinculados à equipe do projeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de membros do projeto"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/membros")
    public ResponseEntity<List<UsuarioResponse>> membros(
            @Parameter(description = "ID público do projeto (ex: prj_...)", required = true, example = "prj_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        usuarioLogado.obrigatorio();
        projetoService.buscarOuFalhar(id);
        return ResponseEntity.ok(bolsistaService.buscarPorProjeto(id).stream().map(UsuarioResponse::de).toList());
    }

    @Operation(summary = "Criar novo projeto", description = "Cadastra um novo projeto vinculado a um laboratório sob gestão do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do projeto inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para criar projeto no laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<EntityModel<ProjetoResponse>> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do projeto", required = true)
            @Valid @RequestBody ProjetoRequest body,
            UriComponentsBuilder uriBuilder) {
        Usuario usuario = usuarioLogado.obrigatorio();

        Projeto p = new Projeto();
        projetoService.aplicar(p, body);
        projetoService.cadastrar(p, usuario);
        URI uri = uriBuilder.replacePath("/api/v1/projeto/{id}").buildAndExpand(p.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(ProjetoResponse.de(p), p.getLaboratorioPublicId()));
    }

    @Operation(summary = "Atualizar projeto", description = "Atualiza o título, descrição, links externos de entregáveis ou laboratório de lotação do projeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar o projeto", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<EntityModel<ProjetoResponse>> atualizar(
            @Parameter(description = "ID público do projeto (ex: prj_...)", required = true, example = "prj_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do projeto", required = true)
            @Valid @RequestBody ProjetoRequest body) {
        Usuario usuario = usuarioLogado.obrigatorio();
        Projeto p = projetoService.buscarExigindoGerencia(id, usuario);
        projetoService.exigirPodeMoverPara(usuario, body.laboratorioId());

        projetoService.aplicar(p, body);
        p.setAtivo(true);
        projetoService.atualizar(p);
        return ResponseEntity.ok(comLinks(ProjetoResponse.de(p), p.getLaboratorioPublicId()));
    }

    @Operation(summary = "Desativar projeto (Soft Delete)", description = "Desativa o projeto mantendo o histórico de vínculos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID público do projeto (ex: prj_...)", required = true, example = "prj_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        Usuario usuario = usuarioLogado.obrigatorio();
        projetoService.buscarExigindoGerencia(id, usuario);
        projetoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vincular bolsista a um projeto", description = "Adiciona um bolsista à equipe do projeto de pesquisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bolsista vinculado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto ou Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/{id}/membros/{bolsistaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Void> vincular(
            @Parameter(description = "ID público do projeto (ex: prj_...)", required = true, example = "prj_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @Parameter(description = "ID público do bolsista a vincular (ex: bol_...)", required = true, example = "bol_a1b2c3d4e5f6g7h8i9j0") @PathVariable String bolsistaId) {
        Usuario usuario = usuarioLogado.obrigatorio();
        projetoService.buscarExigindoGerencia(id, usuario);
        bolsistaService.buscarOuFalhar(bolsistaId);
        projetoService.vincularBolsista(bolsistaId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desvincular bolsista de um projeto", description = "Remove um bolsista da equipe do projeto de pesquisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bolsista desvinculado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}/membros/{bolsistaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Void> desvincular(
            @Parameter(description = "ID público do projeto (ex: prj_...)", required = true, example = "prj_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
            @Parameter(description = "ID público do bolsista a desvincular (ex: bol_...)", required = true, example = "bol_a1b2c3d4e5f6g7h8i9j0") @PathVariable String bolsistaId) {
        Usuario usuario = usuarioLogado.obrigatorio();
        projetoService.buscarExigindoGerencia(id, usuario);
        projetoService.desvincularBolsista(bolsistaId, id);
        return ResponseEntity.noContent().build();
    }

    private ProjetoResponse comMembros(Projeto p) {
        return ProjetoResponse.de(p, projetoService.contarMembros(p.getId()));
    }

    private EntityModel<ProjetoResponse> comLinks(ProjetoResponse resp, String laboratorioId) {
        EntityModel<ProjetoResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/projeto/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/projeto/" + resp.id() + "/membros").withRel("membros"));
        if (laboratorioId != null) {
            modelo.add(Link.of("/api/v1/laboratorio/" + laboratorioId).withRel("laboratorio"));
        }
        return modelo;
    }
}
