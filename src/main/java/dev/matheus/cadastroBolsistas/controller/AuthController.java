package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.request.CadastroAdminRequest;
import dev.matheus.cadastroBolsistas.dto.response.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.request.EsqueciSenhaRequest;
import dev.matheus.cadastroBolsistas.dto.response.EsqueciSenhaResponse;
import dev.matheus.cadastroBolsistas.dto.request.LoginRequest;
import dev.matheus.cadastroBolsistas.dto.response.MensagemResponse;
import dev.matheus.cadastroBolsistas.dto.request.PerfilRequest;
import dev.matheus.cadastroBolsistas.dto.request.RedefinirSenhaRequest;
import dev.matheus.cadastroBolsistas.dto.response.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtService;
import dev.matheus.cadastroBolsistas.service.AdministradorService;
import dev.matheus.cadastroBolsistas.service.LoginService;
import dev.matheus.cadastroBolsistas.service.PasswordResetService;
import dev.matheus.cadastroBolsistas.service.PerfilService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Tag(name = "Autenticação & Sessão", description = "Endpoints de login, emissão de JWT, consulta de sessão (/me), auto-cadastro de admin e fluxo de recuperação de senha.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginService loginService;
    private final JwtService jwtService;
    private final AdministradorService administradorService;
    private final PasswordResetService passwordResetService;
    private final PerfilService perfilService;

    public AuthController(LoginService loginService, JwtService jwtService,
                          AdministradorService administradorService,
                          PasswordResetService passwordResetService,
                          PerfilService perfilService) {
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.administradorService = administradorService;
        this.passwordResetService = passwordResetService;
        this.perfilService = perfilService;
    }

    @Operation(summary = "Autenticar usuário e emitir token", description = "Realiza login por e-mail e senha. Aplica proteção por rate-limiting com bloqueio temporário após tentativas inválidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso. Token Bearer retornado no header Authorization e no corpo JSON.", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Credenciais inválidas ou e-mail com formato incorreto", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "429", description = "Conta temporariamente bloqueada por excesso de tentativas incorretas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Credenciais de e-mail e senha", required = true)
                                                 @Valid @RequestBody LoginRequest body) {
        Usuario usuario = loginService.entrar(body.email(), body.senha());
        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getTipoUsuario());
        return ResponseEntity.ok()
                .header("X-Auth-Token", token)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(UsuarioResponse.from(usuario));
    }

    @Operation(summary = "Encerrar sessão autenticada", description = "Informa ao cliente para descartar o token JWT armazenado localmente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout efetuado com sucesso")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obter dados do usuário autenticado (/me)", description = "Retorna as informações do usuário atualmente logado na sessão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário logado", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> eu(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    @Operation(summary = "Atualizar perfil próprio", description = "Permite a alteração do nome, e-mail, foto e senha do usuário logado. A alteração de senha exige a validação prévia da senha atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/perfil")
    public ResponseEntity<UsuarioResponse> atualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para atualização de perfil e senha", required = true)
            @Valid @RequestBody PerfilRequest body) {
        return ResponseEntity.ok(perfilService.atualizar(usuario, body));
    }

    @Operation(summary = "Cadastro inicial de Administrador", description = "Permite a criação pública de uma conta de Administrador caso o limite de 3 vagas não tenha sido atingido.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrador cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senhas divergentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Limite de administradores atingido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/cadastro-admin")
    public ResponseEntity<UsuarioResponse> cadastrarAdmin(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para cadastro de administrador", required = true)
                                                         @Valid @RequestBody CadastroAdminRequest body,
                                                         UriComponentsBuilder uriBuilder) {
        // Sanitização e validação cross-field delegadas a CadastroAdminRequest e AdministradorService
        Administrador admin = administradorService.criarAdminAutocadastro(
                body.nome(), body.email(), body.senha());

        URI uri = uriBuilder.replacePath("/api/v1/administrador/{id}").buildAndExpand(admin.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(UsuarioResponse.from(admin.getUsuario()));
    }

    @Operation(summary = "Solicitar código de recuperação de senha", description = "Gera um código temporário de 6 dígitos válido por 15 minutos para o e-mail cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de recuperação gerado", content = @Content(schema = @Schema(implementation = EsqueciSenhaResponse.class))),
            @ApiResponse(responseCode = "400", description = "E-mail inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "E-mail não encontrado no sistema", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/password-reset-requests")
    public ResponseEntity<EsqueciSenhaResponse> esqueciSenha(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "E-mail do usuário", required = true)
                                           @Valid @RequestBody EsqueciSenhaRequest body) {
        // EsqueciSenhaRequest normaliza o e-mail via compact constructor; buscarPorEmailOuFalhar lança 404 se não existir
        loginService.buscarPorEmailOuFalhar(body.email());

        String codigo = passwordResetService.gerarCodigo(body.email());

        return ResponseEntity.ok(new EsqueciSenhaResponse(
                "Código de verificação enviado para o e-mail informado (Válido por 15 minutos).",
                codigo
        ));
    }

    @Operation(summary = "Redefinir senha com código verificado", description = "Aplica uma nova senha à conta após a validação do código de 6 dígitos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso", content = @Content(schema = @Schema(implementation = MensagemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou senhas divergentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/password-resets")
    public ResponseEntity<MensagemResponse> redefinirSenha(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para redefinição de senha", required = true)
                                              @Valid @RequestBody RedefinirSenhaRequest body) {
        passwordResetService.redefinirSenha(body.email(), body.codigo(), body.novaSenha());

        return ResponseEntity.ok(new MensagemResponse("Senha redefinida com sucesso! Você já pode acessar sua conta com a nova senha."));
    }
}
