package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.LaboratorioRequest;
import dev.matheus.cadastroBolsistas.dto.LaboratorioResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
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

@Tag(name = "Laboratório", description = "Gerenciamento de laboratórios de pesquisa, equipe alocada, capacidade e cálculo de ocupação.")
@RestController
@RequestMapping("/api/v1/laboratorio")
public class LaboratorioController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final LaboratorioService laboratorioService;
    private final BolsistaService bolsistaService;
    private final ProjetoService projetoService;
    private final UsuarioLogado usuarioLogado;

    public LaboratorioController(LaboratorioService laboratorioService, BolsistaService bolsistaService,
                                 ProjetoService projetoService, UsuarioLogado usuarioLogado) {
        this.laboratorioService = laboratorioService;
        this.bolsistaService = bolsistaService;
        this.projetoService = projetoService;
        this.usuarioLogado = usuarioLogado;
    }

    @Operation(summary = "Listar laboratórios paginados", description = "Retorna laboratórios ativos do sistema ou apenas os que o professor autenticado coordena.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de laboratórios com ocupação"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public PaginaResponse<LaboratorioResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Quantidade de itens por página", example = "10") @RequestParam(required = false) Integer tamanho,
            @Parameter(description = "Filtro de busca por nome, área de pesquisa ou coordenador", example = "Inteligência") @RequestParam(required = false) String buscaNome) {
        Usuario logado = usuarioLogado.obrigatorio();
        List<Laboratorio> labs = logado.isProfessor()
                ? laboratorioService.filtrarPorTermo(laboratorioService.listarPorCoordenador(logado.getId()), buscaNome)
                : laboratorioService.buscarLaboratorios(buscaNome);
        return PaginacaoUtil.paginar(labs, pagina, tamanho, TAMANHO_PADRAO, TAMANHO_MAXIMO, this::comOcupacao);
    }

    @Operation(summary = "Buscar laboratório por ID", description = "Retorna os detalhes de um laboratório específico incluindo capacidade e percentual de ocupação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes do laboratório", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<LaboratorioResponse> buscar(@Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        usuarioLogado.obrigatorio();
        return comLinks(comOcupacao(laboratorioService.buscarOuFalhar(id)));
    }

    @Operation(summary = "Listar bolsistas de um laboratório", description = "Retorna a lista completa de bolsistas e pesquisadores vinculados ao laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de bolsistas do laboratório"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/bolsistas")
    public List<UsuarioResponse> bolsistas(@Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        usuarioLogado.obrigatorio();
        laboratorioService.buscarOuFalhar(id);
        return bolsistaService.buscarPorLaboratorio(id).stream().map(UsuarioResponse::de).toList();
    }

    @Operation(summary = "Listar projetos de um laboratório", description = "Retorna os projetos de pesquisa desenvolvidos no âmbito do laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos do laboratório"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/projetos")
    public List<ProjetoResponse> projetos(@Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        usuarioLogado.obrigatorio();
        laboratorioService.buscarOuFalhar(id);
        return projetoService.listarPorLaboratorio(id).stream()
                .map(p -> ProjetoResponse.de(p, projetoService.contarMembros(p.getId())))
                .toList();
    }

    @Operation(summary = "Criar laboratório", description = "Cadastra um novo laboratório no sistema (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Laboratório criado com sucesso", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de administrador", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<LaboratorioResponse>> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do laboratório", required = true)
                                                     @Valid @RequestBody LaboratorioRequest body,
                                                     UriComponentsBuilder uriBuilder) {
        Laboratorio lab = new Laboratorio();
        laboratorioService.aplicar(lab, body);
        laboratorioService.cadastrar(lab);
        URI uri = uriBuilder.replacePath("/api/v1/laboratorio/{id}").buildAndExpand(lab.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(comOcupacao(lab)));
    }

    @Operation(summary = "Atualizar laboratório", description = "Atualiza os dados de capacidade, nome, área de pesquisa ou coordenador do laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Laboratório atualizado com sucesso", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar este laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public EntityModel<LaboratorioResponse> atualizar(@Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do laboratório", required = true)
                                         @Valid @RequestBody LaboratorioRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        Laboratorio lab = laboratorioService.buscarExigindoGerencia(id, logado);

        laboratorioService.aplicar(lab, body);
        lab.setAtivo(true);
        laboratorioService.atualizar(lab);
        return comLinks(comOcupacao(lab));
    }

    @Operation(summary = "Desativar laboratório (Soft Delete)", description = "Desativa o laboratório no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Laboratório desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para excluir este laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID público do laboratório (ex: lab_...)", required = true, example = "lab_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        Usuario logado = usuarioLogado.obrigatorio();
        laboratorioService.buscarExigindoGerencia(id, logado);
        laboratorioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private LaboratorioResponse comOcupacao(Laboratorio lab) {
        return LaboratorioResponse.de(lab, laboratorioService.contarBolsistasNoLaboratorio(lab.getId()));
    }

    private EntityModel<LaboratorioResponse> comLinks(LaboratorioResponse resp) {
        EntityModel<LaboratorioResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/laboratorio/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/laboratorio/" + resp.id() + "/bolsistas").withRel("bolsistas"),
                Link.of("/api/v1/laboratorio/" + resp.id() + "/projetos").withRel("projetos"));
        if (resp.coordenadorId() != null) {
            modelo.add(Link.of("/api/v1/professor/" + resp.coordenadorId()).withRel("coordenador"));
        }
        return modelo;
    }
}
