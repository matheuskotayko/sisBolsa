package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.LaboratorioRequest;
import dev.matheus.cadastroBolsistas.dto.LaboratorioResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Laboratórios", description = "Gerenciamento de laboratórios de pesquisa, equipe alocada, capacidade e cálculo de ocupação.")
@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioApiController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final LaboratorioService laboratorioService;
    private final BolsistaService bolsistaService;
    private final ProjetoService projetoService;
    private final UsuarioLogado usuarioLogado;
    private final AuditoriaService auditoriaService;

    public LaboratorioApiController(LaboratorioService laboratorioService, BolsistaService bolsistaService,
                                    ProjetoService projetoService, UsuarioLogado usuarioLogado,
                                    AuditoriaService auditoriaService) {
        this.laboratorioService = laboratorioService;
        this.bolsistaService = bolsistaService;
        this.projetoService = projetoService;
        this.usuarioLogado = usuarioLogado;
        this.auditoriaService = auditoriaService;
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
                ? filtrarPorTermo(laboratorioService.listarPorCoordenador(logado.getId()), buscaNome)
                : laboratorioService.buscarLaboratorios(buscaNome);
        return PaginacaoUtil.paginar(labs, pagina, tamanho, TAMANHO_PADRAO, TAMANHO_MAXIMO, this::comOcupacao);
    }

    /* professor coordena poucos laboratorios - filtra em memoria em vez de virar mais uma query no banco */
    private List<Laboratorio> filtrarPorTermo(List<Laboratorio> labs, String buscaNome) {
        if (StringUtil.estaVazio(buscaNome)) {
            return labs;
        }
        String termo = buscaNome.trim().toLowerCase();
        return labs.stream()
                .filter(l -> contem(l.getNome(), termo) || contem(l.getAreaPesquisa(), termo) || contem(l.getCoordenador(), termo))
                .toList();
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    @Operation(summary = "Buscar laboratório por ID", description = "Retorna os detalhes de um laboratório específico incluindo capacidade e percentual de ocupação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhes do laboratório", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public LaboratorioResponse buscar(@Parameter(description = "ID do laboratório (UUID)", required = true) @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        return comOcupacao(exigirLab(id));
    }

    @Operation(summary = "Listar bolsistas de um laboratório", description = "Retorna a lista completa de bolsistas e pesquisadores vinculados ao laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de bolsistas do laboratório"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/bolsistas")
    public List<UsuarioResponse> bolsistas(@Parameter(description = "ID do laboratório (UUID)", required = true) @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        exigirLab(id);
        return bolsistaService.buscarPorLaboratorio(id).stream().map(UsuarioResponse::de).toList();
    }

    @Operation(summary = "Listar projetos de um laboratório", description = "Retorna os projetos de pesquisa desenvolvidos no âmbito do laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos do laboratório"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/projetos")
    public List<ProjetoResponse> projetos(@Parameter(description = "ID do laboratório (UUID)", required = true) @PathVariable UUID id) {
        usuarioLogado.obrigatorio();
        exigirLab(id);
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
    public ResponseEntity<LaboratorioResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do laboratório", required = true)
                                                     @Valid @RequestBody LaboratorioRequest body,
                                                     UriComponentsBuilder uriBuilder) {
        Usuario logado = usuarioLogado.obrigatorio();
        usuarioLogado.exigirAdmin(logado);

        Laboratorio lab = new Laboratorio();
        aplicar(lab, body);
        laboratorioService.cadastrar(lab);
        auditoriaService.registrar(logado, "CRIAR_LABORATORIO", "LABORATORIO", "Laboratório '" + lab.getNome() + "' criado com sucesso.", null);
        URI uri = uriBuilder.replacePath("/api/laboratorios/{id}").buildAndExpand(lab.getId()).toUri();
        return ResponseEntity.created(uri).body(comOcupacao(lab));
    }

    @Operation(summary = "Atualizar laboratório", description = "Atualiza os dados de capacidade, nome, área de pesquisa ou coordenador do laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Laboratório atualizado com sucesso", content = @Content(schema = @Schema(implementation = LaboratorioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para gerenciar este laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public LaboratorioResponse atualizar(@Parameter(description = "ID do laboratório (UUID)", required = true) @PathVariable UUID id,
                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do laboratório", required = true)
                                         @Valid @RequestBody LaboratorioRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        Laboratorio lab = exigirLab(id);
        usuarioLogado.exigir(laboratorioService.podeGerenciar(logado, id), "Sem permissao para editar este laboratorio.");

        aplicar(lab, body);
        lab.setAtivo(true);
        laboratorioService.atualizar(lab);
        auditoriaService.registrar(logado, "ATUALIZAR_LABORATORIO", "LABORATORIO", "Laboratório '" + lab.getNome() + "' atualizado.", null);
        return comOcupacao(lab);
    }

    @Operation(summary = "Desativar laboratório (Soft Delete)", description = "Desativa o laboratório no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Laboratório desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para excluir este laboratório", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do laboratório (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Laboratorio lab = exigirLab(id);
        usuarioLogado.exigir(laboratorioService.podeGerenciar(logado, id), "Sem permissao para excluir este laboratorio.");
        laboratorioService.excluir(id);
        auditoriaService.registrar(logado, "EXCLUIR_LABORATORIO", "LABORATORIO", "Laboratório '" + lab.getNome() + "' desativado.", null);
        return ResponseEntity.noContent().build();
    }

    private Laboratorio exigirLab(UUID id) {
        Laboratorio lab = laboratorioService.buscarPorId(id);
        if (lab == null || !lab.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Laboratorio nao encontrado.");
        }
        return lab;
    }

    private void aplicar(Laboratorio lab, LaboratorioRequest body) {
        lab.setNome(StringUtil.limpar(body.nome()));
        lab.setAreaPesquisa(body.areaPesquisa());
        lab.setStatus(StringUtil.estaVazio(body.status()) ? "Ativo" : body.status());
        lab.setCapacidade(body.capacidade());
        lab.setCoordenadorId(body.coordenadorId());
    }

    private LaboratorioResponse comOcupacao(Laboratorio lab) {
        return LaboratorioResponse.de(lab, laboratorioService.contarBolsistasNoLaboratorio(lab.getId()));
    }
}
