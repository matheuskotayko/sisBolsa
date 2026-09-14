package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.ProjetoRequest;
import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projetos", description = "Gerenciamento de projetos de pesquisa, alocação de pesquisadores e entregáveis (repositórios, documentação).")
@RestController
@RequestMapping("/api/projetos")
public class ProjetoApiController {

    private final ProjetoService projetoService;
    private final LaboratorioService laboratorioService;
    private final BolsistaService bolsistaService;
    private final UsuarioLogado usuarioLogado;
    private final AuditoriaService auditoriaService;

    public ProjetoApiController(ProjetoService projetoService, LaboratorioService laboratorioService,
                                BolsistaService bolsistaService, UsuarioLogado usuarioLogado,
                                AuditoriaService auditoriaService) {
        this.projetoService = projetoService;
        this.laboratorioService = laboratorioService;
        this.bolsistaService = bolsistaService;
        this.usuarioLogado = usuarioLogado;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Listar projetos", description = "Retorna todos os projetos ativos, com filtros opcionais por nome ou por laboratório de pesquisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public List<ProjetoResponse> listar(
            @Parameter(description = "Filtro por nome do projeto", example = "Processamento") @RequestParam(required = false) String buscaNome,
            @Parameter(description = "Filtro por ID do laboratório (UUID)") @RequestParam(required = false) UUID labId) {
        usuarioLogado.obrigatorio();
        return projetoService.buscarProjetos(buscaNome, labId).stream().map(this::comMembros).toList();
    }

    @Operation(summary = "Buscar projeto por ID", description = "Retorna os detalhes de um projeto de pesquisa pelo seu identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do projeto", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ProjetoResponse buscar(@Parameter(description = "ID do projeto (UUID)", required = true) @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        return comMembros(exigirProjeto(id));
    }

    @Operation(summary = "Listar membros de um projeto", description = "Retorna a lista de bolsistas e pesquisadores vinculados à equipe do projeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de membros do projeto"),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/membros")
    public List<UsuarioResponse> membros(@Parameter(description = "ID do projeto (UUID)", required = true) @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        exigirProjeto(id);
        return bolsistaService.buscarPorProjeto(id).stream().map(UsuarioResponse::de).toList();
    }

    @Operation(summary = "Criar novo projeto", description = "Cadastra um novo projeto vinculado a um laboratório sob gestão do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados do projeto inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para criar projeto no laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProjetoResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do projeto", required = true)
                                                 @Valid @RequestBody ProjetoRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        usuarioLogado.exigir(laboratorioService.podeGerenciar(logado, body.laboratorioId()),
                "Sem permissao para criar projeto neste laboratorio.");

        Projeto p = new Projeto();
        aplicar(p, body);
        projetoService.cadastrar(p);
        auditoriaService.registrar(logado, "CRIAR_PROJETO", "PROJETO", "Projeto '" + p.getNome() + "' criado com sucesso.", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjetoResponse.de(p));
    }

    @Operation(summary = "Atualizar projeto", description = "Atualiza o título, descrição, links externos de entregáveis ou laboratório de lotação do projeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso", content = @Content(schema = @Schema(implementation = ProjetoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar o projeto", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public ProjetoResponse atualizar(@Parameter(description = "ID do projeto (UUID)", required = true) @PathVariable UUID id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do projeto", required = true)
                                     @Valid @RequestBody ProjetoRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        Projeto p = exigirProjeto(id);
        exigirPermissaoNoLab(logado, p.getLaboratorioId());
        usuarioLogado.exigir(laboratorioService.podeGerenciar(logado, body.laboratorioId()),
                "Sem permissao para mover o projeto para este laboratorio.");

        aplicar(p, body);
        p.setAtivo(true);
        projetoService.atualizar(p);
        auditoriaService.registrar(logado, "ATUALIZAR_PROJETO", "PROJETO", "Projeto '" + p.getNome() + "' atualizado.", null);
        return ProjetoResponse.de(p);
    }

    @Operation(summary = "Desativar projeto (Soft Delete)", description = "Desativa o projeto mantendo o histórico de vínculos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do projeto (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Projeto p = exigirProjeto(id);
        exigirPermissaoNoLab(logado, p.getLaboratorioId());
        projetoService.excluir(id);
        auditoriaService.registrar(logado, "EXCLUIR_PROJETO", "PROJETO", "Projeto '" + p.getNome() + "' desativado.", null);
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
            @Parameter(description = "ID do projeto (UUID)", required = true) @PathVariable UUID id,
            @Parameter(description = "ID do bolsista a vincular (UUID)", required = true) @PathVariable UUID bolsistaId) {
        Usuario logado = usuarioLogado.obrigatorio();
        Projeto p = exigirProjeto(id);
        exigirPermissaoNoLab(logado, p.getLaboratorioId());
        if (bolsistaService.buscarPorId(bolsistaId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsista nao encontrado.");
        }
        projetoService.vincularBolsista(bolsistaId, id);
        auditoriaService.registrar(logado, "VINCULAR_BOLSISTA", "PROJETO", "Bolsista " + bolsistaId + " vinculado ao projeto '" + p.getNome() + "'.", null);
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
            @Parameter(description = "ID do projeto (UUID)", required = true) @PathVariable UUID id,
            @Parameter(description = "ID do bolsista a desvincular (UUID)", required = true) @PathVariable UUID bolsistaId) {
        Usuario logado = usuarioLogado.obrigatorio();
        Projeto p = exigirProjeto(id);
        exigirPermissaoNoLab(logado, p.getLaboratorioId());
        projetoService.desvincularBolsista(bolsistaId, id);
        auditoriaService.registrar(logado, "DESVINCULAR_BOLSISTA", "PROJETO", "Bolsista " + bolsistaId + " desvinculado do projeto '" + p.getNome() + "'.", null);
        return ResponseEntity.noContent().build();
    }

    private Projeto exigirProjeto(UUID id) {
        Projeto p = projetoService.buscarPorId(id);
        if (p == null || !p.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto nao encontrado.");
        }
        return p;
    }

    private void exigirPermissaoNoLab(Usuario logado, UUID labId) {
        usuarioLogado.exigir(laboratorioService.podeGerenciar(logado, labId),
                "Sem permissao para gerenciar projetos deste laboratorio.");
    }

    private void aplicar(Projeto p, ProjetoRequest body) {
        p.setNome(StringUtil.limpar(body.nome()));
        p.setDescricao(body.descricao());
        p.setLaboratorioId(body.laboratorioId());
        p.setLinkRepositorio(StringUtil.limpar(body.linkRepositorio()));
        p.setLinkDocumentacao(StringUtil.limpar(body.linkDocumentacao()));
    }

    private ProjetoResponse comMembros(Projeto p) {
        return ProjetoResponse.de(p, projetoService.contarMembros(p.getId()));
    }
}
