package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.CursoRequest;
import dev.matheus.cadastroBolsistas.dto.CursoResponse;
import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.model.Curso;
import dev.matheus.cadastroBolsistas.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Curso", description = "Lista de cursos disponíveis para vínculo de bolsistas.")
@RestController
@RequestMapping("/api/v1/curso")
@SecurityRequirement(name = "bearerAuth")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @Operation(summary = "Listar cursos disponíveis", description = "Retorna todos os cursos ativos cadastrados no sistema, usados no cadastro de bolsistas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cursos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<CursoResponse>> listar() {
        return ResponseEntity.ok(cursoService.listarTodos().stream().map(CursoResponse::de).toList());
    }

    @Operation(summary = "Cadastrar novo curso", description = "Adiciona um novo curso à lista disponível (restrito a Administradores).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Curso cadastrado com sucesso", content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Curso já cadastrado ou nome inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão de administrador", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CursoResponse> criar(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados do curso", required = true)
                                                @Valid @RequestBody CursoRequest body,
                                                UriComponentsBuilder uriBuilder) {
        // nome já normalizado pelo compact constructor de CursoRequest
        Curso curso = cursoService.cadastrar(body.nome());
        URI uri = uriBuilder.replacePath("/api/v1/curso").build().toUri();
        return ResponseEntity.created(uri).body(CursoResponse.de(curso));
    }
}
