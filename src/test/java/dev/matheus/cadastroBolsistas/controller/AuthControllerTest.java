package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.exceptions.LimiteAdminsAtingidoException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.AdministradorService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.JwtService;
import dev.matheus.cadastroBolsistas.service.LoginService;
import dev.matheus.cadastroBolsistas.service.PasswordResetService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Cobre autenticacao, logout, perfil e cadastro inicial de admin na rota /api/v1/auth.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @TestConfiguration
    static class Config {
        /**
         * PasswordEncoder permanece aqui pois é usado nos @BeforeEach para montar
         * usuarios com senha hasheada realista nos testes de /perfil.
         * O AuthController em si NÃO injeta mais PasswordEncoder — encoding é no AdministradorService.
         */
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    private static final String SENHA_ATUAL = "senha123";
    private static final UUID USUARIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private BolsistaService bolsistaService;

    @MockitoBean
    private ProfessorService professorService;

    @MockitoBean
    private AdministradorService administradorService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    private Usuario usuarioLogado;
    private Bolsista bolsistaLogado;

    @BeforeEach
    void setUp() {
        usuarioLogado = new Usuario(USUARIO_ID, "Thiago Rocha", "thiago@teste.com",
                passwordEncoder.encode(SENHA_ATUAL), true, "BOLSISTA", null, null);
        bolsistaLogado = new Bolsista(USUARIO_ID, usuarioLogado);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void logarComo(Usuario usuario) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getTipoUsuario()));
        var auth = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String json(String... pares) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < pares.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(pares[i]).append("\":\"").append(pares[i + 1]).append("\"");
        }
        return sb.append("}").toString();
    }

    @Test
    void login_comCredenciaisValidas_retornaTokenNoHeader() throws Exception {
        when(loginService.autenticar("thiago@teste.com", "12345678")).thenReturn(usuarioLogado);
        when(jwtService.gerarToken("thiago@teste.com", "BOLSISTA")).thenReturn("token-fake");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "senha", "12345678")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Auth-Token", "token-fake"))
                .andExpect(jsonPath("$.email").value("thiago@teste.com"));
    }

    @Test
    void login_quandoContaBloqueadaPorRateLimiting_retorna429() throws Exception {
        when(loginService.isBloqueado("bloqueado@teste.com")).thenReturn(true);
        when(loginService.getSegundosRestantesBloqueio("bloqueado@teste.com")).thenReturn(300L);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "bloqueado@teste.com", "senha", "qualquer")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_naoDevolveASenhaNoCorpo() throws Exception {
        Usuario u = new Usuario();
        u.setEmail("thiago@teste.com");
        u.setSenha("hash-secreto");
        when(loginService.autenticar(any(), any())).thenReturn(u);
        when(jwtService.gerarToken(any(), any())).thenReturn("t");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "senha", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void login_comCredenciaisInvalidas_retorna401() throws Exception {
        when(loginService.autenticar(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "x@teste.com", "senha", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_comEmailEmBranco_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "   ", "senha", "12345678")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_comEmailFormatoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "email-invalido", "senha", "12345678")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_comSenhaEmBranco_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "senha", "   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_comEspacosNoEmail_normalizaComTrim() throws Exception {
        Usuario u = new Usuario();
        u.setEmail("thiago@teste.com");
        u.setTipoUsuario("BOLSISTA");
        when(loginService.autenticar("thiago@teste.com", "12345678")).thenReturn(u);
        when(jwtService.gerarToken("thiago@teste.com", "BOLSISTA")).thenReturn("token-fake");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "   thiago@teste.com   ", "senha", "12345678")))
                .andExpect(status().isOk());

        verify(loginService).autenticar("thiago@teste.com", "12345678");
    }

    @Test
    void logout_retorna204() throws Exception {
        logarComo(usuarioLogado);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void me_semSessao_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_comSessao_devolveOUsuario() throws Exception {
        logarComo(usuarioLogado);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Thiago Rocha"));
    }

    @Test
    void perfil_semTrocarSenha_atualizaOsDados() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        when(bolsistaService.calcularNovaSenha(eq(usuarioLogado), isNull(), isNull(), isNull())).thenReturn(null);
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Editado", "email", "novo@teste.com")))
                .andExpect(status().isOk());

        verify(bolsistaService).aplicarDadosPerfil(eq(bolsistaLogado.getUsuario()), any(), isNull());
        verify(bolsistaService).atualizar(bolsistaLogado);
    }

    @Test
    void perfil_comSenhaAtualCorreta_gravaNovoHash() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        String hashEsperado = passwordEncoder.encode("novaSenha123");
        when(bolsistaService.calcularNovaSenha(usuarioLogado, SENHA_ATUAL, "novaSenha123", "novaSenha123")).thenReturn(hashEsperado);
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "novaSenha123", "confirmaSenha", "novaSenha123")))
                .andExpect(status().isOk());

        verify(bolsistaService).aplicarDadosPerfil(eq(bolsistaLogado.getUsuario()), any(), eq(hashEsperado));
        verify(bolsistaService).atualizar(bolsistaLogado);
    }

    @Test
    void perfil_comSenhaAtualErrada_recusa() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        when(bolsistaService.calcularNovaSenha(usuarioLogado, "chuteErrado", "hackeado123", "hackeado123"))
                .thenThrow(new IllegalArgumentException("A senha atual informada esta incorreta."));
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", "chuteErrado", "senha", "hackeado123", "confirmaSenha", "hackeado123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha atual informada esta incorreta."));

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comConfirmacaoDiferente_recusa() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        when(bolsistaService.calcularNovaSenha(usuarioLogado, SENHA_ATUAL, "novaSenha123", "outraCoisa"))
                .thenThrow(new IllegalArgumentException("A nova senha e a confirmacao nao coincidem."));
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "novaSenha123", "confirmaSenha", "outraCoisa")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comSenhaNovaCurta_recusa() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        when(bolsistaService.calcularNovaSenha(usuarioLogado, SENHA_ATUAL, "123", "123"))
                .thenThrow(new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres."));
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "123", "confirmaSenha", "123")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comNomeCurto_recusa() throws Exception {
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Jo", "email", "thiago@teste.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("nome"))
                .andExpect(jsonPath("$[0].mensagem").value("O nome deve ter pelo menos 3 caracteres."));
    }

    @Test
    void perfil_deProfessor_usaOServicoDeProfessor() throws Exception {
        UUID profId = UUID.randomUUID();
        Usuario profUsuario = new Usuario(profId, "Dr. Roberto", "roberto@teste.com",
                passwordEncoder.encode(SENHA_ATUAL), true, "PROFESSOR", null, null);
        Professor professor = new Professor(profId, profUsuario);

        when(professorService.buscarOuFalhar(profId)).thenReturn(professor);
        when(bolsistaService.calcularNovaSenha(eq(profUsuario), isNull(), isNull(), isNull())).thenReturn(null);
        logarComo(profUsuario);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Dr. Roberto Mendes", "email", "roberto@teste.com")))
                .andExpect(status().isOk());

        verify(professorService).atualizar(any(Professor.class));
        verify(bolsistaService, never()).buscarOuFalhar(any(UUID.class));
        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_quandoOUsuarioSumiuDoBanco_retorna404() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenThrow(new RecursoNaoEncontradoException("Usuario nao encontrado."));
        logarComo(usuarioLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastroAdmin_dentroDoLimite_cria() throws Exception {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setNome("Novo Admin");
        u.setEmail("novo@teste.com");
        u.setTipoUsuario("ADMIN");

        Administrador adminCriado = new Administrador();
        adminCriado.setUsuario(u);

        // Senha crua agora é passada ao service (4 args); encoding é responsabilidade do AdministradorService
        when(administradorService.criarAdminAutocadastro("Novo Admin", "novo@teste.com", "123456", "123456"))
                .thenReturn(adminCriado);

        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Novo Admin", "email", "novo@teste.com",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoUsuario").value("ADMIN"));

        verify(administradorService).criarAdminAutocadastro("Novo Admin", "novo@teste.com", "123456", "123456");
    }

    @Test
    void cadastroAdmin_noLimite_retorna409() throws Exception {
        // criarAdminAutocadastro já chama exigirVagaParaNovoAdmin internamente
        doThrow(new LimiteAdminsAtingidoException("O sistema ja possui o numero maximo de administradores permitido."))
                .when(administradorService).criarAdminAutocadastro(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Quarto Admin", "email", "quarto@teste.com",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroAdmin_comEmailInvalido_recusa() throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Alguem", "email", "nao-e-email",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isBadRequest());

        verify(administradorService, never()).criarAdminAutocadastro(any(), any(), any(), any());
    }

    @Test
    void cadastroAdmin_comSenhasDivergentes_retorna400() throws Exception {
        // Validação cross-field agora ocorre no service; mockamos a exceção
        doThrow(new IllegalArgumentException("As senhas nao coincidem."))
                .when(administradorService).criarAdminAutocadastro(any(), any(), eq("123456"), eq("outra"));

        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Alguem", "email", "alguem@teste.com",
                                "senha", "123456", "confirmaSenha", "outra")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void perfil_deAdmin_usaOServicoDeAdministrador() throws Exception {
        UUID adminId = UUID.randomUUID();
        Usuario adminUsuario = new Usuario(adminId, "Admin Master", "admin@teste.com",
                passwordEncoder.encode(SENHA_ATUAL), true, "ADMIN", null, null);
        Administrador admin = new Administrador();
        admin.setId(adminId);
        admin.setUsuario(adminUsuario);

        when(administradorService.buscarPorId(adminId)).thenReturn(admin);
        when(bolsistaService.calcularNovaSenha(eq(adminUsuario), isNull(), isNull(), isNull())).thenReturn(null);
        logarComo(adminUsuario);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Admin Editado", "email", "admin@teste.com")))
                .andExpect(status().isOk());

        verify(administradorService).atualizar(admin);
        verify(bolsistaService, never()).atualizar(any());
        verify(professorService, never()).atualizar(any());
    }

    @Test
    void esqueciSenha_comEmailExistente_retorna200ECodigo() throws Exception {
        // buscarPorEmailOuFalhar retorna usuário normalmente (sem lançar exceção)
        when(loginService.buscarPorEmailOuFalhar("thiago@teste.com")).thenReturn(usuarioLogado);
        when(passwordResetService.gerarCodigo("thiago@teste.com")).thenReturn("123456");

        mockMvc.perform(post("/api/v1/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoDev").value("123456"))
                .andExpect(jsonPath("$.mensagem").exists());

        verify(passwordResetService).gerarCodigo("thiago@teste.com");
    }

    @Test
    void esqueciSenha_comEmailInexistente_retorna404() throws Exception {
        // buscarPorEmailOuFalhar lança RecursoNaoEncontradoException → handler mapeia para 404
        when(loginService.buscarPorEmailOuFalhar("inexistente@teste.com"))
                .thenThrow(new RecursoNaoEncontradoException("Nenhum usuário cadastrado encontrado com este e-mail."));

        mockMvc.perform(post("/api/v1/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "inexistente@teste.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void redefinirSenha_comDadosValidos_chamaServiceERetorna200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "codigo", "123456",
                                "novaSenha", "novaSenha123", "confirmaSenha", "novaSenha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists());

        verify(passwordResetService).redefinirSenha("thiago@teste.com", "123456", "novaSenha123", "novaSenha123");
    }
}
