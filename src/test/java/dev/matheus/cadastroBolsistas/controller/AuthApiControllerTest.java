package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.exceptions.LimiteAdminsAtingidoException;
import dev.matheus.cadastroBolsistas.exceptions.RecursoNaoEncontradoException;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.JwtService;
import dev.matheus.cadastroBolsistas.service.LoginService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * cobre login e edicao de perfil pela api com UUIDs.
 */
@WebMvcTest(controllers = AuthApiController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthApiControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UsuarioLogado usuarioLogado() {
            return new UsuarioLogado();
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
    private dev.matheus.cadastroBolsistas.service.PasswordResetService passwordResetService;

    private Bolsista bolsistaLogado;

    @BeforeEach
    void setUp() {
        bolsistaLogado = new Bolsista();
        bolsistaLogado.setId(USUARIO_ID);
        bolsistaLogado.setNome("Thiago Rocha");
        bolsistaLogado.setEmail("thiago@teste.com");
        bolsistaLogado.setSenha(passwordEncoder.encode(SENHA_ATUAL));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /* simula o que o JwtCookieFilter faz de verdade: poe o Usuario como principal no SecurityContext */
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
    void login_comCredenciaisValidas_gravaCookieComOToken() throws Exception {
        Bolsista u = new Bolsista();
        u.setId(USUARIO_ID);
        u.setNome("Thiago");
        u.setEmail("thiago@teste.com");
        when(loginService.autenticar("thiago@teste.com", "12345678")).thenReturn(u);
        when(jwtService.gerarToken("thiago@teste.com", "BOLSISTA")).thenReturn("token-fake");
        when(jwtService.getExpiracaoMinutos()).thenReturn(120L);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "senha", "12345678")))
                .andExpect(status().isOk())
                .andExpect(cookie().value("token", "token-fake"))
                .andExpect(cookie().httpOnly("token", true))
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
        Bolsista u = new Bolsista();
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
    void login_comCredenciaisInvalidas_retorna401SemCookie() throws Exception {
        when(loginService.autenticar(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "x@teste.com", "senha", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("token"));
    }

    @Test
    void logout_limpaOCookie() throws Exception {
        logarComo(bolsistaLogado);

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("token", 0));
    }

    @Test
    void me_semSessao_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_comSessao_devolveOUsuario() throws Exception {
        logarComo(bolsistaLogado);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Thiago Rocha"));
    }

    @Test
    void perfil_semTrocarSenha_atualizaOsDados() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        when(bolsistaService.calcularNovaSenha(eq(bolsistaLogado), isNull(), isNull(), isNull())).thenReturn(null);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Editado", "email", "novo@teste.com")))
                .andExpect(status().isOk());

        /* mapeamento de campos e regra de senha agora sao testados isoladamente em BolsistaServiceTest */
        verify(bolsistaService).aplicarDadosPerfil(eq(bolsistaLogado), any(), isNull());
        verify(bolsistaService).atualizar(bolsistaLogado);
    }

    @Test
    void perfil_comSenhaAtualCorreta_gravaNovoHash() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        String hashEsperado = passwordEncoder.encode("novaSenha123");
        when(bolsistaService.calcularNovaSenha(bolsistaLogado, SENHA_ATUAL, "novaSenha123", "novaSenha123")).thenReturn(hashEsperado);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "novaSenha123", "confirmaSenha", "novaSenha123")))
                .andExpect(status().isOk());

        verify(bolsistaService).aplicarDadosPerfil(eq(bolsistaLogado), any(), eq(hashEsperado));
        verify(bolsistaService).atualizar(bolsistaLogado);
    }

    @Test
    void perfil_comSenhaAtualErrada_recusa() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenReturn(bolsistaLogado);
        when(bolsistaService.calcularNovaSenha(bolsistaLogado, "chuteErrado", "hackeado123", "hackeado123"))
                .thenThrow(new IllegalArgumentException("A senha atual informada esta incorreta."));
        logarComo(bolsistaLogado);

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
        when(bolsistaService.calcularNovaSenha(bolsistaLogado, SENHA_ATUAL, "novaSenha123", "outraCoisa"))
                .thenThrow(new IllegalArgumentException("A nova senha e a confirmacao nao coincidem."));
        logarComo(bolsistaLogado);

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
        when(bolsistaService.calcularNovaSenha(bolsistaLogado, SENHA_ATUAL, "123", "123"))
                .thenThrow(new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres."));
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "123", "confirmaSenha", "123")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comNomeCurto_recusa() throws Exception {
        logarComo(bolsistaLogado);

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
        Professor professor = new Professor();
        professor.setId(profId);
        professor.setNome("Dr. Roberto");
        professor.setEmail("roberto@teste.com");
        professor.setSenha(passwordEncoder.encode(SENHA_ATUAL));
        when(professorService.buscarOuFalhar(profId)).thenReturn(professor);
        when(bolsistaService.calcularNovaSenha(eq(professor), isNull(), isNull(), isNull())).thenReturn(null);
        logarComo(professor);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Dr. Roberto Mendes", "email", "roberto@teste.com")))
                .andExpect(status().isOk());

        verify(professorService).atualizar(any(Professor.class));
        /* fluxo de professor nao deve tocar em persistencia de bolsista */
        verify(bolsistaService, never()).buscarOuFalhar(any());
        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_quandoOUsuarioSumiuDoBanco_retorna404() throws Exception {
        when(bolsistaService.buscarOuFalhar(USUARIO_ID)).thenThrow(new RecursoNaoEncontradoException("Usuario nao encontrado."));
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/v1/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastroAdmin_dentroDoLimite_cria() throws Exception {
        Bolsista adminCriado = new Bolsista();
        adminCriado.setId(UUID.randomUUID());
        adminCriado.setNome("Novo Admin");
        adminCriado.setEmail("novo@teste.com");
        adminCriado.setTipoUsuario("ADMIN");
        when(bolsistaService.criarAdmin(eq("Novo Admin"), eq("novo@teste.com"), any())).thenReturn(adminCriado);

        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Novo Admin", "email", "novo@teste.com",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoUsuario").value("ADMIN"));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(bolsistaService).criarAdmin(eq("Novo Admin"), eq("novo@teste.com"), hashCaptor.capture());
        assertTrue(passwordEncoder.matches("123456", hashCaptor.getValue()));
    }

    @Test
    void cadastroAdmin_noLimite_retorna409() throws Exception {
        doThrow(new LimiteAdminsAtingidoException("O sistema ja possui o numero maximo de administradores permitido."))
                .when(bolsistaService).exigirVagaParaNovoAdmin();

        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Quarto Admin", "email", "quarto@teste.com",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isConflict());

        verify(bolsistaService, never()).criarAdmin(any(), any(), any());
    }

    @Test
    void cadastroAdmin_comEmailInvalido_recusa() throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Alguem", "email", "nao-e-email",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).inserir(any());
    }
}
