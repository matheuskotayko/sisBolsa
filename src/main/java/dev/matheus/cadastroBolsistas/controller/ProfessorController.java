package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.PaginaResponse;
import dev.matheus.cadastroBolsistas.dto.ProfessorRequest;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Professor;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Tag(name = "Professor", description = "Gestão de professores coordenadores (restrito a Administradores).")
@RestController
@RequestMapping("/api/v1/professor")
@SecurityRequirement(name = "bearerAuth")
public class ProfessorController {

    private static final int TAMANHO_PADRAO = 10;
    private static final int TAMANHO_MAXIMO = 200;

    private final ProfessorService professorService;
    private final BolsistaService bolsistaService;
    private final LaboratorioService laboratorioService;
    private final PasswordEncoder passwordEncoder;

    public ProfessorController(ProfessorService professorService, BolsistaService bolsistaService,
                               LaboratorioService laboratorioService, PasswordEncoder passwordEncoder) {
        this.professorService = professorService;
        this.bolsistaService = bolsistaService;
        this.laboratorioService = laboratorioService;
        this.passwordEncoder = passwordEncoder;
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
            @Parameter(description = "Filtro de busca textual por nome", example = "Carlos") @RequestParam(required = false) String buscaNome) {

        ArrayList<Professor> lista;
        if (!StringUtil.estaVazio(buscaNome)) {
            lista = new ArrayList<>(professorService.buscarPorNome(buscaNome));
        } else {
            lista = new ArrayList<>(professorService.listarTodos());
        }

        for (Professor p : lista) {
            laboratorioService.listarPorCoordenador(p.getId()).stream().findFirst()
                    .ifPresent(lab -> p.setNomeLaboratorio(lab.getNome()));
        }

        return PaginacaoUtil.paginar(lista, pagina, tamanho, TAMANHO_PADRAO, TAMANHO_MAXIMO, UsuarioResponse::de);
    }

    @Operation(summary = "Buscar professor por ID", description = "Recupera as informações detalhadas de um professor coordenador pelo seu identificador público (ex: prf_...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do professor", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public EntityModel<UsuarioResponse> buscar(@Parameter(description = "ID público do professor (ex: prf_...)", required = true, example = "prf_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        Professor p = professorService.buscarOuFalhar(id);
        laboratorioService.listarPorCoordenador(p.getId()).stream().findFirst()
                .ifPresent(lab -> p.setNomeLaboratorio(lab.getNome()));
        return comLinks(UsuarioResponse.de(p));
    }

    @Operation(summary = "Cadastrar novo professor", description = "Cria um novo professor coordenador no sistema (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Professor cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<UsuarioResponse>> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do professor a ser cadastrado", required = true)
                                                 @Valid @RequestBody ProfessorRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        bolsistaService.validarSenha(body.senha(), true);

        Professor p = new Professor();
        professorService.aplicarComuns(p, body);
        p.setSenha(passwordEncoder.encode(body.senha()));
        professorService.inserir(p);
        URI uri = uriBuilder.replacePath("/api/v1/professor/{id}").buildAndExpand(p.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(comLinks(UsuarioResponse.de(p)));
    }

    @Operation(summary = "Atualizar professor", description = "Substitui os dados do professor pelos enviados: campo omitido fica vazio (restrito a Administradores). Se a senha vier em branco, a atual é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/{id}")
    public EntityModel<UsuarioResponse> atualizar(@Parameter(description = "ID público do professor a atualizar", required = true, example = "prf_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do professor", required = true)
                                     @Valid @RequestBody ProfessorRequest body) {
        bolsistaService.validarSenha(body.senha(), false);
        Professor p = professorService.buscarOuFalhar(id);

        professorService.aplicarComuns(p, body);
        if (!StringUtil.estaVazio(body.senha())) {
            p.setSenha(passwordEncoder.encode(body.senha()));
        }
        professorService.atualizar(p);
        return comLinks(UsuarioResponse.de(p));
    }

    @Operation(summary = "Desativar professor (Soft Delete)", description = "Desativa um professor coordenador (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Professor desativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "ID público do professor a desativar", required = true, example = "prf_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        professorService.buscarOuFalhar(id);
        professorService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UsuarioResponse> comLinks(UsuarioResponse resp) {
        return EntityModel.of(resp, Link.of("/api/v1/professor/" + resp.id()).withSelfRel());
    }
}
