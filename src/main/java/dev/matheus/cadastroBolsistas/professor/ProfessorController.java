package dev.matheus.cadastroBolsistas.professor;

import dev.matheus.cadastroBolsistas.common.ErroResponse;
import dev.matheus.cadastroBolsistas.usuario.UsuarioResponse;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Professor", description = "Gestão de professores coordenadores (restrito a Administradores).")
@RestController
@RequestMapping("/api/v1/professor")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    @Operation(summary = "Listar professores paginados", description = "Retorna os professores coordenadores cadastrados (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de professores"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @ParameterObject Pageable pageable,
            @Parameter(description = "Filtro de busca textual por nome", example = "Carlos") @RequestParam(required = false) String buscaNome) {
        return ResponseEntity.ok(professorService.listarComLaboratorio(buscaNome, pageable).map(UsuarioResponse::from));
    }

    @Operation(summary = "Buscar professor por ID", description = "Recupera as informações detalhadas de um professor coordenador pelo seu identificador público (ex: prf_...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do professor", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(@Parameter(description = "ID público do professor (ex: prf_...)", required = true, example = "prf_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id) {
        Professor p = professorService.buscarComLaboratorio(id);
        return ResponseEntity.ok(UsuarioResponse.from(p));
    }

    @Operation(summary = "Cadastrar novo professor", description = "Cria um novo professor coordenador no sistema (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Professor cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados cadastrais inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do professor a ser cadastrado", required = true)
                                                 @Valid @RequestBody ProfessorRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        Professor p = professorService.criar(body);
        URI uri = uriBuilder.replacePath("/api/v1/professor/{id}").buildAndExpand(p.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(UsuarioResponse.from(p));
    }

    @Operation(summary = "Atualizar professor", description = "Atualiza os dados do professor (restrito a Administradores). Se a senha vier em branco, a atual é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@Parameter(description = "ID público do professor a atualizar", required = true, example = "prf_a1b2c3d4e5f6g7h8i9j0") @PathVariable String id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novos dados do professor", required = true)
                                     @Valid @RequestBody ProfessorRequest body) {
        Professor p = professorService.atualizar(id, body);
        return ResponseEntity.ok(UsuarioResponse.from(p));
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
}
