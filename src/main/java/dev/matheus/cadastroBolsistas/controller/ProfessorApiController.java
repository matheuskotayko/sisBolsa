package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProfessorRequest;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
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
import java.util.UUID;

/*
 * so admin mexe em professor - manter/criar/editar/excluir e ate listar. e a
 * mesma restricao que ja existia quando professor vivia dentro de /usuarios.
 */
@Tag(name = "Professores", description = "Gestão de professores coordenadores (restrito a Administradores).")
@RestController
@RequestMapping("/api/v1/professores")
public class ProfessorApiController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final ProfessorService professorService;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;
    private final AuditoriaService auditoriaService;

    public ProfessorApiController(ProfessorService professorService, BolsistaService bolsistaService,
                                  LaboratorioService laboratorioService, PasswordEncoder passwordEncoder,
                                  UsuarioLogado usuarioLogado, AuditoriaService auditoriaService) {
        this.professorService = professorService;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Listar professores paginados", description = "Retorna os professores coordenadores cadastrados (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de professores"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @Parameter(description = "Número da página", example = "1") @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Quantidade de itens por página", example = "10") @RequestParam(required = false) Integer tamanho,
            @Parameter(description = "Filtro de busca textual por nome", example = "Roberto") @RequestParam(required = false) String buscaNome) {
        Usuario logado = usuarioLogado.obrigatorio();
        professorService.exigirAdmin(logado);

        ArrayList<Professor> lista = StringUtil.estaVazio(buscaNome)
                ? professorService.listarTodos()
                : professorService.buscarPorNome(buscaNome);
        laboratorioService.preencherLabsDosProfessores(new ArrayList<>(lista));

        return PaginacaoUtil.paginar(lista, pagina, tamanho, TAMANHO_PADRAO, TAMANHO_MAXIMO, UsuarioResponse::de);
    }

    @Operation(summary = "Buscar professor por ID", description = "Recupera as informações detalhadas de um professor coordenador (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do professor", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<UsuarioResponse> buscar(@Parameter(description = "ID do professor (UUID)", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Professor p = professorService.buscarExigindoAdmin(id, logado);
        return comLinks(UsuarioResponse.de(p));
    }

    @Operation(summary = "Cadastrar novo professor", description = "Cria um novo professor coordenador (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Professor cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<UsuarioResponse>> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do professor a ser cadastrado", required = true)
                                                 @Valid @RequestBody ProfessorRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        Usuario logado = usuarioLogado.obrigatorio();
        professorService.exigirAdmin(logado);
        bolsistaService.validarSenha(body.senha(), true);

        Professor p = new Professor();
        professorService.aplicarComuns(p, body);
        p.setSenha(passwordEncoder.encode(body.senha()));
        professorService.inserir(p);
        auditoriaService.registrar(logado, "CRIAR_PROFESSOR", "USUARIO", "Professor '" + p.getNome() + "' (" + p.getEmail() + ") cadastrado.", null);
        URI uri = uriBuilder.replacePath("/api/v1/professores/{id}").buildAndExpand(p.getId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(UsuarioResponse.de(p)));
    }

    @Operation(summary = "Atualizar professor", description = "Atualiza os dados de um professor existente (restrito a Administradores). Se a senha for enviada em branco, a atual é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}")
    public EntityModel<UsuarioResponse> atualizar(@Parameter(description = "ID do professor a atualizar", required = true) @PathVariable UUID id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do professor", required = true)
                                     @Valid @RequestBody ProfessorRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();
        bolsistaService.validarSenha(body.senha(), false);
        Professor p = professorService.buscarExigindoAdmin(id, logado);

        professorService.aplicarComuns(p, body);
        if (!StringUtil.estaVazio(body.senha())) {
            p.setSenha(passwordEncoder.encode(body.senha()));
        }
        professorService.atualizar(p);
        auditoriaService.registrar(logado, "ATUALIZAR_PROFESSOR", "USUARIO", "Professor '" + p.getNome() + "' atualizado.", null);
        return comLinks(UsuarioResponse.de(p));
    }

    @Operation(summary = "Desativar professor (Soft Delete)", description = "Desativa um professor coordenador (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Professor desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID do professor a desativar", required = true) @PathVariable UUID id) {
        Usuario logado = usuarioLogado.obrigatorio();
        Professor p = professorService.buscarExigindoAdmin(id, logado);
        professorService.excluir(id);
        auditoriaService.registrar(logado, "EXCLUIR_PROFESSOR", "USUARIO", "Professor '" + p.getNome() + "' desativado.", null);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UsuarioResponse> comLinks(UsuarioResponse resp) {
        return EntityModel.of(resp, Link.of("/api/v1/professores/" + resp.id()).withSelfRel());
    }
}
