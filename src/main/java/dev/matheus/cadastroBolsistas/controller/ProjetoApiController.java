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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Projetos", description = "Gerenciamento de projetos de pesquisa, alocação de pesquisadores e entregáveis (repositórios, documentação).")
@RestController
@RequestMapping("/api/v1/projetos")
public class ProjetoApiController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final ProjetoService projetoService;
    private final BolsistaService bolsistaService;
    private final UsuarioLogado usuarioLogado;

    public ProjetoApiController(ProjetoService projetoService, BolsistaService bolsistaService,
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
    public PaginaResponse<ProjetoResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Quantidade de itens por página", example = "10") @RequestParam(required = false) Integer tamanho,
            @Parameter(description = "Filtro por nome do projeto", example = "Sistema") @RequestParam(required = false) String buscaNome,
            @Parameter(description = "Filtro por ID do laboratório (UUID)", example = "c1111111-1111-1111-1111-111111111111") @RequestParam(required = false) UUID labId) {
        usuarioLogado.obrigatorio();
        List<Projeto> lista = projetoService.buscarProjetos(buscaNome, labId);
        return PaginacaoUtil.paginar(lista, pagina, tamanho, TAMANHO_PADRAO, TAMANHO_MAXIMO, this::comMembros);
    }

    @Operation(summary = "Buscar projeto por ID", description = "Retorna os detalhes de um projeto de pesquisa pelo seu identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do projeto", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<ProjetoResponse> buscar(@Parameter(description = "ID do projeto (UUID)", required = true, example = "e1111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        Projeto p = projetoService.buscarOuFalhar(id);
        return comLinks(comMembros(p), p.getLaboratorioId());
    }

    @Operation(summary = "Listar membros de um projeto", description = "Retorna a lista de bolsistas e pesquisadores vinculados à equipe do projeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de membros do projeto"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/membros")
    public List<UsuarioResponse> membros(@Parameter(description = "ID do projeto (UUID)", required = true, example = "e1111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        projetoService.buscarOuFalhar(id);
        return bolsistaService.buscarPorProjeto(id).stream().map(UsuarioResponse::de).toList();
    }

    @Operation(summary = "Criar novo projeto", description = "Cadastra um novo projeto vinculado a um laboratório sob gestão do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do projeto inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para criar projeto no laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<ProjetoResponse>> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do projeto", required = true)
                                                 @Valid @RequestBody ProjetoRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        Usuario logado = usuarioLogado.obrigatorio();

        Projeto p = new Projeto();
        projetoService.aplicar(p, body);
        projetoService.cadastrar(p, logado);
        URI uri = uriBuilder.replacePath("/api/v1/projetos/{id}").buildAndExpand(p.getId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(ProjetoResponse.de(p), p.getLaboratorioId()));
    }

    @Operation(summary = "Atualizar projeto", description = "Atualiza o título, descrição, links externos de entregáveis ou laboratório de lotação do projeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar o projeto", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public EntityModel<ProjetoResponse> atualizar(@Parameter(description = "ID do projeto (UUID)", required = true, example = "e1111111-1111-1111-1111-111111111111") @PathVariable UUID id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do projeto", required = true)
                                     @Valid @RequestBody ProjetoRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        Projeto p = projetoService.buscarExigindoGerencia(id, logado);
        projetoService.exigirPodeMoverPara(logado, body.laboratorioId());

        projetoService.aplicar(p, body);
        p.setAtivo(true);
        projetoService.atualizar(p);
        return comLinks(ProjetoResponse.de(p), p.getLaboratorioId());
    }

    @Operation(summary = "Desativar projeto (Soft Delete)", description = "Desativa o projeto mantendo o histórico de vínculos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do projeto (UUID)", required = true, example = "e1111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        projetoService.buscarExigindoGerencia(id, logado);
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
    public ResponseEntity<Void> vincular(
            /* exemplo: Diego (lab de IA) no projeto do lab de software - vinculo que o seed ainda nao tem */
            @Parameter(description = "ID do projeto (UUID)", required = true, example = "e1111111-1111-1111-1111-111111111111") @PathVariable UUID id,
            @Parameter(description = "ID do bolsista a vincular (UUID)", required = true, example = "d3333333-3333-3333-3333-333333333333") @PathVariable UUID bolsistaId) {
        Usuario logado = usuarioLogado.obrigatorio();
        projetoService.buscarExigindoGerencia(id, logado);
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
    public ResponseEntity<Void> desvincular(
            @Parameter(description = "ID do projeto (UUID)", required = true, example = "e1111111-1111-1111-1111-111111111111") @PathVariable UUID id,
            @Parameter(description = "ID do bolsista a desvincular (UUID)", required = true, example = "d1111111-1111-1111-1111-111111111111") @PathVariable UUID bolsistaId) {
        Usuario logado = usuarioLogado.obrigatorio();
        projetoService.buscarExigindoGerencia(id, logado);
        projetoService.desvincularBolsista(bolsistaId, id);
        return ResponseEntity.noContent().build();
    }

    private ProjetoResponse comMembros(Projeto p) {
        return ProjetoResponse.de(p, projetoService.contarMembros(p.getId()));
    }

    private EntityModel<ProjetoResponse> comLinks(ProjetoResponse resp, UUID laboratorioId) {
        EntityModel<ProjetoResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/projetos/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/projetos/" + resp.id() + "/membros").withRel("membros"));
        if (laboratorioId != null) {
            modelo.add(Link.of("/api/v1/laboratorios/" + laboratorioId).withRel("laboratorio"));
        }
        return modelo;
    }
}
