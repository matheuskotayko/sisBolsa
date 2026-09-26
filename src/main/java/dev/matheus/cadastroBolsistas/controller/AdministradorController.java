package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.AdministradorRequest;
import dev.matheus.cadastroBolsistas.dto.AdministradorResponse;
import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.service.AdministradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/administrador")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administrador", description = "Gestão de perfis e contas administrativas do sistema.")
@SecurityRequirement(name = "bearerAuth")
public class AdministradorController {

    private final AdministradorService service;
    private final UsuarioLogado usuarioLogado;

    public AdministradorController(AdministradorService service, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.usuarioLogado = usuarioLogado;
    }

    @Operation(summary = "Listar administradores", description = "Retorna todos os administradores ativos no sistema.")
    @ApiResponse(responseCode = "200", description = "Listagem de administradores.")
    @GetMapping
    public ResponseEntity<List<AdministradorResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @Operation(summary = "Buscar administrador por ID público", description = "Retorna os detalhes de um administrador específico.")
    @ApiResponse(responseCode = "200", description = "Administrador encontrado.")
    @ApiResponse(responseCode = "404", description = "Administrador não encontrado.", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<AdministradorResponse> buscarPorId(@PathVariable String id) {
        Administrador admin = service.buscarPorId(id);
        return ResponseEntity.ok(AdministradorResponse.de(admin));
    }

    @Operation(summary = "Cadastrar administrador", description = "Cria um novo administrador institucional. Requer perfil ADMIN e respeita o limite de 3 contas ativas.")
    @ApiResponse(responseCode = "201", description = "Administrador cadastrado com sucesso.")
    @ApiResponse(responseCode = "400", description = "Erro de validação ou limite de administradores excedido.", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PostMapping
    public ResponseEntity<AdministradorResponse> criar(@Valid @RequestBody AdministradorRequest body) {
        Usuario usuario = usuarioLogado.obrigatorio();
        AdministradorResponse criado = service.criar(body, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @Operation(summary = "Atualizar administrador", description = "Atualiza os dados de um administrador existente.")
    @ApiResponse(responseCode = "200", description = "Administrador atualizado com sucesso.")
    @ApiResponse(responseCode = "404", description = "Administrador não encontrado.", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<AdministradorResponse> atualizar(@PathVariable String id,
                                                           @Valid @RequestBody AdministradorRequest body) {
        Usuario usuario = usuarioLogado.obrigatorio();
        AdministradorResponse atualizado = service.atualizar(id, body, usuario);
        return ResponseEntity.ok(atualizado);
    }

    @Operation(summary = "Desativar administrador", description = "Desativa um administrador do sistema (não permite desativar o último ativo).")
    @ApiResponse(responseCode = "204", description = "Administrador desativado com sucesso.")
    @ApiResponse(responseCode = "400", description = "Não é permitido desativar o único administrador ativo.", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable String id) {
        Usuario usuario = usuarioLogado.obrigatorio();
        service.desativar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
