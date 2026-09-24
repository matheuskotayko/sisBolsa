package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.BolsistaRequest;
import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Cargo;
import dev.matheus.cadastroBolsistas.model.ModalidadeBolsa;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * cobre bolsista e admin - admin e uma linha desta mesma tabela com
 * tipoUsuario=ADMIN, nao uma entidade separada. professor tem controller
 * proprio (ProfessorApiController / /api/v1/professores).
 */
@Tag(name = "Bolsistas", description = "Gestão de bolsistas e administradores, incluindo vigência, modalidades e cargos.")
@RestController
@RequestMapping("/api/v1/bolsistas")
public class BolsistaApiController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final BolsistaService bolsistaService;
    private final ProjetoService projetoService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;

    public BolsistaApiController(BolsistaService bolsistaService, ProjetoService projetoService,
                                 PasswordEncoder passwordEncoder, UsuarioLogado usuarioLogado) {
        this.bolsistaService = bolsistaService;
        this.projetoService = projetoService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
    }

    @Operation(summary = "Listar bolsistas paginados", description = "Retorna a listagem de bolsistas de acordo com o escopo do usuário autenticado: ADMIN visualiza todos, PROFESSOR visualiza os bolsistas dos laboratórios que coordena, e BOLSISTA visualiza seus colegas de laboratório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de bolsistas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Quantidade de itens por página", example = "10") @RequestParam(required = false) Integer tamanho,
            @Parameter(description = "Filtro por perfil (ADMIN, BOLSISTA)", example = "BOLSISTA") @RequestParam(required = false) String tipo,
            @Parameter(description = "Filtro de busca textual por nome", example = "Lucas") @RequestParam(required = false) String buscaNome,
            @Parameter(description = "Filtro de busca textual por curso", example = "Engenharia") @RequestParam(required = false) String buscaCurso) {
        Usuario logado = usuarioLogado.obrigatorio();

        ArrayList<Usuario> lista;
        if (!StringUtil.estaVazio(buscaNome)) {
            lista = new ArrayList<>(bolsistaService.buscarPorNome(buscaNome));
        } else if (!StringUtil.estaVazio(buscaCurso)) {
            lista = new ArrayList<>(bolsistaService.buscarPorCurso(buscaCurso));
        } else {
            lista = new ArrayList<>(bolsistaService.listarTodos());
        }

        lista = bolsistaService.filtrarPorTipo(lista, tipo);
        lista = bolsistaService.filtrarPorEscopo(lista, logado);

        return paginar(lista, pagina, tamanho);
    }

    @Operation(summary = "Buscar bolsista por ID", description = "Recupera as informações detalhadas de um bolsista ou administrador pelo seu identificador UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do bolsista", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para ver este bolsista", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<UsuarioResponse> buscar(@Parameter(description = "ID do bolsista (UUID)", required = true, example = "d1111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Bolsista b = bolsistaService.buscarComPermissaoDeVisualizacao(id, logado);
        return comLinks(UsuarioResponse.de(b));
    }

    @Operation(summary = "Listar cargos disponíveis", description = "Retorna todos os cargos cadastrados para bolsistas nos laboratórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cargos")
    })
    @GetMapping("/cargos")
    public List<Map<String, String>> cargos() {
        return java.util.Arrays.stream(Cargo.values())
                .map(c -> Map.of("valor", c.name(), "descricao", c.getDescricao()))
                .toList();
    }

    @Operation(summary = "Listar modalidades de bolsa", description = "Retorna todas as modalidades de bolsa suportadas pelo sistema (PIBIC, PIBITI, Extensão, Monitoria, etc.).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de modalidades")
    })
    @GetMapping("/modalidades")
    public List<Map<String, String>> modalidades() {
        return java.util.Arrays.stream(ModalidadeBolsa.values())
                .map(m -> Map.of("valor", m.name(), "descricao", m.getDescricao()))
                .toList();
    }

    @Operation(summary = "Exportar bolsistas em CSV", description = "Exporta em arquivo CSV a lista de bolsistas visíveis para o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo CSV para download", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "403", description = "Acesso negado para bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() {
        Usuario logado = usuarioLogado.obrigatorio();
        bolsistaService.exigirPodeExportarUsuarios(logado);

        ArrayList<Usuario> lista = new ArrayList<>(bolsistaService.listarTodos());
        lista = bolsistaService.filtrarPorEscopo(lista, logado);

        StringBuilder sb = new StringBuilder("ID,Nome,Email,Tipo,Curso,Matricula,Cargo,Modalidade,Valor,DataInicio,DataFim,Laboratorio\n");
        for (Usuario u : lista) {
            UsuarioResponse r = UsuarioResponse.de(u);
            sb.append(String.join(",",
                    String.valueOf(r.id()), CsvUtil.escapar(r.nome()), CsvUtil.escapar(r.email()), CsvUtil.escapar(r.tipoUsuario()),
                    CsvUtil.escapar(r.curso()), CsvUtil.escapar(r.matricula()), CsvUtil.escapar(r.cargo()),
                    CsvUtil.escapar(r.modalidadeBolsaDescricao()), CsvUtil.escapar(r.valorBolsa() != null ? String.format("%.2f", r.valorBolsa()) : ""),
                    CsvUtil.escapar(r.dataInicioBolsa() != null ? r.dataInicioBolsa().toString() : ""),
                    CsvUtil.escapar(r.dataFimBolsa() != null ? r.dataFimBolsa().toString() : ""),
                    CsvUtil.escapar(r.nomeLaboratorio()))).append("\n");
        }

        return ArquivoDownloadUtil.csv("bolsistas.csv", sb.toString());
    }

    @Operation(summary = "Listar projetos vinculados a um bolsista", description = "Retorna todos os projetos de pesquisa dos quais o bolsista é membro ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de projetos vinculados"),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}/projetos")
    public List<ProjetoResponse> projetos(@Parameter(description = "ID do bolsista", required = true, example = "d1111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        bolsistaService.buscarComPermissaoDeVisualizacao(id, logado);
        return projetoService.listarPorBolsista(id).stream().map(ProjetoResponse::de).toList();
    }

    @Operation(summary = "Cadastrar novo bolsista ou administrador", description = "Cria um novo bolsista ou administrador no sistema. Administradores podem cadastrar qualquer um; Professores podem cadastrar bolsistas para os laboratórios que coordenam.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bolsista cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para cadastrar bolsistas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<UsuarioResponse>> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do bolsista a ser cadastrado", required = true)
                                                 @Valid @RequestBody BolsistaRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        Usuario logado = usuarioLogado.obrigatorio();
        bolsistaService.exigirPodeCadastrarUsuario(logado);
        bolsistaService.validarSenha(body.senha(), true);

        if ("ADMIN".equalsIgnoreCase(body.tipoUsuario())) {
            bolsistaService.exigirPodeCriarAdmin(logado);
        }

        Bolsista b = new Bolsista();
        bolsistaService.aplicarComuns(b, body);
        bolsistaService.aplicarCamposDeBolsista(b, body, logado);
        b.setSenha(passwordEncoder.encode(body.senha()));
        bolsistaService.inserir(b);
        URI uri = uriBuilder.replacePath("/api/v1/bolsistas/{id}").buildAndExpand(b.getId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(UsuarioResponse.de(b)));
    }

    @Operation(summary = "Atualizar bolsista ou administrador", description = "Substitui os dados do bolsista pelos enviados: campo omitido fica vazio. Se a senha vier em branco, a atual é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bolsista atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão para editar este bolsista", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public EntityModel<UsuarioResponse> atualizar(@Parameter(description = "ID do bolsista a atualizar", required = true, example = "d1111111-1111-1111-1111-111111111111") @PathVariable UUID id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do bolsista", required = true)
                                     @Valid @RequestBody BolsistaRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        bolsistaService.validarSenha(body.senha(), false);
        Bolsista b = bolsistaService.buscarComPermissaoDeEdicao(id, logado);

        String senhaAtual = b.getSenha();
        bolsistaService.aplicarComuns(b, body);
        bolsistaService.aplicarCamposDeBolsista(b, body, logado);
        b.setSenha(StringUtil.estaVazio(body.senha()) ? senhaAtual : passwordEncoder.encode(body.senha()));
        bolsistaService.atualizar(b);
        return comLinks(UsuarioResponse.de(b));
    }

    @Operation(summary = "Desativar bolsista (Soft Delete)", description = "Desativa um bolsista ou administrador, mantendo o histórico de frequência e projetos íntegros no banco de dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bolsista desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para desativar este bolsista", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bolsista não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do bolsista a desativar", required = true, example = "d1111111-1111-1111-1111-111111111111") @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        bolsistaService.buscarComPermissaoDeExclusao(id, logado);
        bolsistaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private PaginaResponse<UsuarioResponse> paginar(List<Usuario> lista, int pagina, Integer tamanhoPedido) {
        return PaginacaoUtil.paginar(lista, pagina, tamanhoPedido, TAMANHO_PADRAO, TAMANHO_MAXIMO, UsuarioResponse::de);
    }

    private EntityModel<UsuarioResponse> comLinks(UsuarioResponse resp) {
        EntityModel<UsuarioResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/bolsistas/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/bolsistas/" + resp.id() + "/projetos").withRel("projetos"));
        if (resp.laboratorioId() != null) {
            modelo.add(Link.of("/api/v1/laboratorios/" + resp.laboratorioId()).withRel("laboratorio"));
        }
        return modelo;
    }
}
