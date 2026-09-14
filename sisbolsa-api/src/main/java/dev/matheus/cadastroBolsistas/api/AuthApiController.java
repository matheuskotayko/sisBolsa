package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.dto.CadastroAdminRequest;
import dev.matheus.cadastroBolsistas.dto.ErroResponse;
import dev.matheus.cadastroBolsistas.dto.EsqueciSenhaRequest;
import dev.matheus.cadastroBolsistas.dto.LoginRequest;
import dev.matheus.cadastroBolsistas.dto.PerfilRequest;
import dev.matheus.cadastroBolsistas.dto.RedefinirSenhaRequest;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.CookieJwt;
import dev.matheus.cadastroBolsistas.security.JwtService;
import dev.matheus.cadastroBolsistas.security.LoginAttemptService;
import dev.matheus.cadastroBolsistas.security.PasswordResetService;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LoginService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import dev.matheus.cadastroBolsistas.util.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Tag(name = "Autenticação", description = "Endpoints para autenticação, controle de sessão via JWT HttpOnly, perfil e recuperação de senha.")
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final int LIMITE_ADMINS = 3;

    private final LoginService loginService;
    private final JwtService jwtService;
    private final UsuarioLogado usuarioLogado;
    private final BolsistaService bolsistaService;
    private final ProfessorService professorService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;

    public AuthApiController(LoginService loginService, JwtService jwtService, UsuarioLogado usuarioLogado,
                             BolsistaService bolsistaService, ProfessorService professorService,
                             PasswordEncoder passwordEncoder,
                             AuditoriaService auditoriaService,
                             LoginAttemptService loginAttemptService,
                             PasswordResetService passwordResetService) {
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.usuarioLogado = usuarioLogado;
        this.bolsistaService = bolsistaService;
        this.professorService = professorService;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.loginAttemptService = loginAttemptService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Autenticar usuário", description = "Valida as credenciais e grava o token JWT em um cookie HttpOnly com proteção SameSite. Possui proteção por Rate Limiting contra força bruta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "429", description = "Conta bloqueada temporariamente por excesso de tentativas incorretas", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/login")
    public UsuarioResponse login(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Credenciais de e-mail e senha", required = true)
                                 @RequestBody LoginRequest body,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String email = body.email() != null ? body.email().trim() : "";
        String ip = extrairIp(request);

        if (loginAttemptService.isBloqueado(email)) {
            long segundos = loginAttemptService.getSegundosRestantesBloqueio(email);
            long minutos = Math.max(1, (segundos + 59) / 60);
            auditoriaService.registrar(null, "Anônimo", "LOGIN_BLOQUEADO", "AUTH", "Tentativa de login com conta temporariamente bloqueada: " + email, ip);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas incorretas. Conta bloqueada temporariamente por " + minutos + " minuto(s).");
        }

        Usuario usuario = loginService.autenticar(
                body.email() != null ? body.email().trim() : null,
                body.senha() != null ? body.senha().trim() : null);

        if (usuario == null) {
            loginAttemptService.registrarFalha(email);
            int restantes = loginAttemptService.getTentativasRestantes(email);
            auditoriaService.registrar(null, "Anônimo", "LOGIN_FALHA", "AUTH", "Tentativa de login inválida com e-mail: " + email + " (" + restantes + " restantes)", ip);

            if (loginAttemptService.isBloqueado(email)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Limite de 5 tentativas consecutivas excedido. Conta bloqueada temporariamente por 5 minutos.");
            }

            String aviso = (restantes <= 2 && restantes > 0) ? " Restam " + restantes + " tentativa(s) antes do bloqueio temporário." : "";
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos." + aviso);
        }

        loginAttemptService.registrarSucesso(email);
        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getTipoUsuario());
        CookieJwt.gravar(response, token, jwtService.getExpiracaoMinutos());
        auditoriaService.registrar(usuario, "LOGIN", "AUTH", "Login efetuado com sucesso (" + usuario.getTipoUsuario() + ")", ip);
        return UsuarioResponse.de(usuario);
    }

    private static String extrairIp(HttpServletRequest request) {
        if (request == null) return null;
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Operation(summary = "Encerrar sessão (Logout)", description = "Limpa o cookie HttpOnly contendo o token JWT e invalida a sessão no servidor.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão encerrada com sucesso")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Usuario usuario = usuarioLogado.obrigatorio();
        auditoriaService.registrar(usuario, "LOGOUT", "AUTH", "Sessão encerrada pelo usuário", null);
        CookieJwt.limpar(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obter dados do usuário autenticado (/me)", description = "Retorna as informações do usuário atualmente logado na sessão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário logado", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @GetMapping("/me")
    public UsuarioResponse eu() {
        return UsuarioResponse.de(usuarioLogado.obrigatorio());
    }

    @Operation(summary = "Atualizar perfil próprio", description = "Permite a alteração do nome, e-mail, foto e senha do usuário logado. A alteração de senha exige a validação prévia da senha atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PutMapping("/perfil")
    public UsuarioResponse atualizarPerfil(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para atualização de perfil e senha", required = true)
                                           @Valid @RequestBody PerfilRequest body) {
        Usuario logado = usuarioLogado.obrigatorio();

        String nome = StringUtil.limpar(body.nome());
        String email = StringUtil.limpar(body.email());

        String senhaNova = null;
        boolean trocandoSenha = !StringUtil.estaVazio(body.senhaAtual())
                || !StringUtil.estaVazio(body.senha())
                || !StringUtil.estaVazio(body.confirmaSenha());
        if (trocandoSenha) {
            if (StringUtil.estaVazio(body.senhaAtual()) || StringUtil.estaVazio(body.senha())
                    || StringUtil.estaVazio(body.confirmaSenha())) {
                throw new IllegalArgumentException("Para alterar a senha, preencha a senha atual, a nova e a confirmacao.");
            }
            if (!passwordEncoder.matches(body.senhaAtual(), logado.getSenha())) {
                throw new IllegalArgumentException("A senha atual informada esta incorreta.");
            }
            if (body.senha().length() < 6) {
                throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres.");
            }
            if (!body.senha().equals(body.confirmaSenha())) {
                throw new IllegalArgumentException("A nova senha e a confirmacao nao coincidem.");
            }
            senhaNova = passwordEncoder.encode(body.senha());
        }

        Usuario atualizado;
        if (logado.isProfessor()) {
            Professor p = professorService.buscarPorId(logado.getId());
            if (p == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil nao encontrado.");
            }
            aplicar(p, nome, email, body, senhaNova);
            professorService.atualizar(p);
            atualizado = p;
        } else {
            Bolsista b = bolsistaService.buscarPorId(logado.getId());
            if (b == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil nao encontrado.");
            }
            aplicar(b, nome, email, body, senhaNova);
            bolsistaService.atualizar(b);
            atualizado = b;
        }

        if (senhaNova != null) {
            auditoriaService.registrar(atualizado, "ALTERAR_SENHA", "USUARIO", "Senha de acesso alterada pelo próprio usuário com sucesso.", null);
        } else {
            auditoriaService.registrar(atualizado, "ATUALIZAR_PERFIL", "USUARIO", "Dados cadastrais do perfil atualizados.", null);
        }

        return UsuarioResponse.de(atualizado);
    }

    private void aplicar(Usuario u, String nome, String email, PerfilRequest body, String senhaNova) {
        u.setNome(nome);
        u.setEmail(email);
        u.setFotoUrl(body.fotoUrl());
        u.setBio(body.bio());
        if (senhaNova != null) {
            u.setSenha(senhaNova);
        }
    }

    @Operation(summary = "Vagas de administrador restantes", description = "Informa quantas contas com perfil ADMIN ainda podem ser cadastradas (limite máximo de 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade de vagas restantes")
    })
    @GetMapping("/admins-restantes")
    public Map<String, Integer> adminsRestantes() {
        return Map.of("restantes", Math.max(0, LIMITE_ADMINS - bolsistaService.contarAdmins()));
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
        if (bolsistaService.contarAdmins() >= LIMITE_ADMINS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O sistema ja possui o numero maximo de administradores (" + LIMITE_ADMINS + ").");
        }

        Bolsista admin = new Bolsista();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setTipoUsuario("ADMIN");
        admin.setAtivo(true);
        admin.setDataNascimento(java.time.LocalDate.of(1990, 1, 1));
        admin.setCurso("Gestao");
        admin.setMatricula("ADM001");
        bolsistaService.inserir(admin);

        /* admin cadastrado aqui vira um Bolsista com tipoUsuario=ADMIN, o recurso mora em /api/usuarios */
        URI uri = uriBuilder.replacePath("/api/usuarios/{id}").buildAndExpand(admin.getId()).toUri();
        return ResponseEntity.created(uri).body(UsuarioResponse.de(admin));
    }

    @Operation(summary = "Solicitar código de recuperação de senha", description = "Gera um código temporário de 6 dígitos válido por 15 minutos para o e-mail cadastrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de recuperação gerado"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "E-mail não encontrado no sistema", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/password-reset-requests")
    public Map<String, String> esqueciSenha(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "E-mail do usuário", required = true)
                                           @Valid @RequestBody EsqueciSenhaRequest body,
                                           HttpServletRequest request) {
        String email = StringUtil.limpar(body.email());

        Usuario u = loginService.buscarPorEmail(email);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum usuário cadastrado encontrado com este e-mail.");
        }

        String codigo = passwordResetService.gerarCodigo(email);
        String ip = extrairIp(request);
        auditoriaService.registrar(u, "SOLICITAR_RECUPERACAO_SENHA", "AUTH", "Código de recuperação gerado para " + email, ip);

        return Map.of(
                "mensagem", "Código de verificação enviado para o e-mail informado (Válido por 15 minutos).",
                "codigoDev", codigo
        );
    }

    @Operation(summary = "Redefinir senha com código verificado", description = "Aplica uma nova senha à conta após a validação do código de 6 dígitos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou senhas divergentes", content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    @PostMapping("/password-resets")
    public Map<String, String> redefinirSenha(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dados para redefinição de senha", required = true)
                                              @Valid @RequestBody RedefinirSenhaRequest body,
                                              HttpServletRequest request) {
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }

        String hash = passwordEncoder.encode(novaSenha);
        if (u.isProfessor()) {
            Professor p = professorService.buscarPorId(u.getId());
            if (p != null) {
                p.setSenha(hash);
                professorService.atualizar(p);
            }
        } else {
            Bolsista b = bolsistaService.buscarPorId(u.getId());
            if (b != null) {
                b.setSenha(hash);
                bolsistaService.atualizar(b);
            }
        }

        passwordResetService.invalidarCodigo(email);
        String ip = extrairIp(request);
        auditoriaService.registrar(u, "REDEFINICAO_SENHA", "AUTH", "Senha redefinida com sucesso via código de verificação.", ip);

        return Map.of("mensagem", "Senha redefinida com sucesso! Você já pode acessar sua conta com a nova senha.");
    }
}
