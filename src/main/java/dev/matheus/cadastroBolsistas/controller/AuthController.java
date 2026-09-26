package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.*;
import dev.matheus.cadastroBolsistas.exceptions.ContaBloqueadaException;
import dev.matheus.cadastroBolsistas.exceptions.CredenciaisInvalidasException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.repository.AdministradorRepository;
import dev.matheus.cadastroBolsistas.repository.UsuarioRepository;
import dev.matheus.cadastroBolsistas.service.*;

import dev.matheus.cadastroBolsistas.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Tag(name = "Autenticação & Sessão", description = "Endpoints de login, emissão de JWT, consulta de sessão (/me), auto-cadastro de admin e fluxo de recuperação de senha.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginService loginService;
    private final JwtService jwtService;
    private final UsuarioLogado usuarioLogado;
    private final BolsistaService bolsistaService;
    private final ProfessorService professorService;
    private final AdministradorService administradorService;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(LoginService loginService, JwtService jwtService, UsuarioLogado usuarioLogado,
                          BolsistaService bolsistaService, ProfessorService professorService,
                          AdministradorService administradorService, AdministradorRepository administradorRepository,
                          PasswordEncoder passwordEncoder, PasswordResetService passwordResetService,
                          UsuarioRepository usuarioRepository) {
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.usuarioLogado = usuarioLogado;
        this.bolsistaService = bolsistaService;
        this.professorService = professorService;
        this.administradorService = administradorService;
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.usuarioRepository = usuarioRepository;
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
        String email = body.email() != null ? body.email().trim() : "";

        if (loginService.isBloqueado(email)) {
            long segundos = loginService.getSegundosRestantesBloqueio(email);
            long minutos = Math.max(1, (segundos + 59) / 60);
            throw new ContaBloqueadaException(
                    "Muitas tentativas incorretas. Conta bloqueada temporariamente por " + minutos + " minuto(s).");
        }

        Usuario usuario = loginService.autenticar(
                body.email() != null ? body.email().trim() : null,
                body.senha() != null ? body.senha().trim() : null);

        if (usuario == null) {
            loginService.registrarFalha(email);
            int restantes = loginService.getTentativasRestantes(email);

            if (loginService.isBloqueado(email)) {
                throw new ContaBloqueadaException(
                        "Limite de 5 tentativas consecutivas excedido. Conta bloqueada temporariamente por 5 minutos.");
            }

            String aviso = (restantes <= 2 && restantes > 0) ? " Restam " + restantes + " tentativa(s) antes do bloqueio temporário." : "";
            throw new CredenciaisInvalidasException("E-mail ou senha incorretos." + aviso);
        }

        loginService.registrarSucesso(email);
        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getTipoUsuario());
        return ResponseEntity.ok()
                .header("X-Auth-Token", token)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(UsuarioResponse.de(usuario));
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
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> eu() {
        Usuario usuario = usuarioLogado.obrigatorio();
        return ResponseEntity.ok(UsuarioResponse.de(usuario));
    }

    @Operation(summary = "Atualizar perfil próprio", description = "Permite a alteração do nome, e-mail, foto e senha do usuário logado. A alteração de senha exige a validação prévia da senha atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PatchMapping("/perfil")
    public ResponseEntity<UsuarioResponse> atualizarPerfil(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para atualização de perfil e senha", required = true)
            @Valid @RequestBody PerfilRequest body) {
        Usuario usuario = usuarioLogado.obrigatorio();
        String senhaNova = bolsistaService.calcularNovaSenha(usuario, body.senhaAtual(), body.senha(), body.confirmaSenha());

        UsuarioResponse response;
        if (usuario.isAdmin()) {
            Administrador a = administradorService.buscarPorId(usuario.getId());
            bolsistaService.aplicarDadosPerfil(a.getUsuario(), body, senhaNova);
            administradorRepository.save(a);
            response = UsuarioResponse.de(a.getUsuario());
        } else if (usuario.isProfessor()) {
            Professor p = professorService.buscarOuFalhar(usuario.getId());
            bolsistaService.aplicarDadosPerfil(p.getUsuario(), body, senhaNova);
            professorService.atualizar(p);
            response = UsuarioResponse.de(p);
        } else {
            Bolsista b = bolsistaService.buscarOuFalhar(usuario.getId());
            bolsistaService.aplicarDadosPerfil(b.getUsuario(), body, senhaNova);
            bolsistaService.atualizar(b);
            response = UsuarioResponse.de(b);
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cadastro inicial de Administrador", description = "Permite a criação pública de uma conta de Administrador caso o limite de 3 vagas não tenha sido atingido.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Administrador cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Limite de administradores atingido", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/cadastro-admin")
    public ResponseEntity<UsuarioResponse> cadastrarAdmin(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para cadastro de administrador", required = true)
                                                         @Valid @RequestBody CadastroAdminRequest body,
                                                         UriComponentsBuilder uriBuilder) {
        String nome = StringUtil.limpar(body.nome());
        String email = StringUtil.limpar(body.email());
        String senha = StringUtil.limpar(body.senha());
        String confirma = StringUtil.limpar(body.confirmaSenha());

        if (!senha.equals(confirma)) {
            throw new IllegalArgumentException("As senhas nao coincidem.");
        }
        administradorService.exigirVagaParaNovoAdmin();

        Administrador admin = administradorService.criarAdminAutocadastro(nome, email, passwordEncoder.encode(senha));

        URI uri = uriBuilder.replacePath("/api/v1/administrador/{id}").buildAndExpand(admin.getPublicId()).toUri();
        return ResponseEntity.created(uri).body(UsuarioResponse.de(admin.getUsuario()));
    }

    @Operation(summary = "Solicitar código de recuperação de senha", description = "Gera um código temporário de 6 dígitos válido por 15 minutos para o e-mail cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de recuperação gerado"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "E-mail não encontrado no sistema", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/password-reset-requests")
    public ResponseEntity<Map<String, String>> esqueciSenha(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "E-mail do usuário", required = true)
                                           @Valid @RequestBody EsqueciSenhaRequest body) {
        String email = StringUtil.limpar(body.email());

        Usuario u = loginService.buscarPorEmail(email);
        if (u == null) {
            throw new RecursoNaoEncontradoException("Nenhum usuário cadastrado encontrado com este e-mail.");
        }

        String codigo = passwordResetService.gerarCodigo(email);

        return ResponseEntity.ok(Map.of(
                "mensagem", "Código de verificação enviado para o e-mail informado (Válido por 15 minutos).",
                "codigoDev", codigo
        ));
    }

    @Operation(summary = "Redefinir senha com código verificado", description = "Aplica uma nova senha à conta após a validação do código de 6 dígitos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou senhas divergentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/password-resets")
    public ResponseEntity<MensagemResponse> redefinirSenha(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para redefinição de senha", required = true)
                                              @Valid @RequestBody RedefinirSenhaRequest body) {
        String email = StringUtil.limpar(body.email());
        String codigo = StringUtil.limpar(body.codigo());
        String novaSenha = StringUtil.limpar(body.novaSenha());
        String confirma = StringUtil.limpar(body.confirmaSenha());

        if (!passwordResetService.validarCodigo(email, codigo)) {
            throw new IllegalArgumentException("Código de verificação inválido ou expirado.");
        }

        if (!novaSenha.equals(confirma)) {
            throw new IllegalArgumentException("A nova senha e a confirmação não conferem.");
        }

        Usuario u = loginService.buscarPorEmail(email);
        if (u == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado.");
        }

        String hash = passwordEncoder.encode(novaSenha);
        u.setSenha(hash);
        usuarioRepository.save(u);

        passwordResetService.invalidarCodigo(email);

        return ResponseEntity.ok(new MensagemResponse("Senha redefinida com sucesso! Você já pode acessar sua conta com a nova senha."));
    }
}
