package dev.matheus.cadastroBolsistas.api;

import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.JwtService;
import dev.matheus.cadastroBolsistas.security.LoginAttemptService;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.AuditoriaService;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
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
    private AuditoriaService auditoriaService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private dev.matheus.cadastroBolsistas.security.PasswordResetService passwordResetService;

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

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "senha", "12345678")))
                .andExpect(status().isOk())
                .andExpect(cookie().value("token", "token-fake"))
                .andExpect(cookie().httpOnly("token", true))
                .andExpect(jsonPath("$.email").value("thiago@teste.com"));
    }

    @Test
    void login_quandoContaBloqueadaPorRateLimiting_retorna429() throws Exception {
        when(loginAttemptService.isBloqueado("bloqueado@teste.com")).thenReturn(true);
        when(loginAttemptService.getSegundosRestantesBloqueio("bloqueado@teste.com")).thenReturn(300L);

        mockMvc.perform(post("/api/auth/login")
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

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "thiago@teste.com", "senha", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void login_comCredenciaisInvalidas_retorna401SemCookie() throws Exception {
        when(loginService.autenticar(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "x@teste.com", "senha", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("token"));
    }

    @Test
    void logout_limpaOCookie() throws Exception {
        logarComo(bolsistaLogado);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("token", 0));
    }

    @Test
    void me_semSessao_retorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_comSessao_devolveOUsuario() throws Exception {
        logarComo(bolsistaLogado);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Thiago Rocha"));
    }

    @Test
    void perfil_semTrocarSenha_atualizaOsDados() throws Exception {
        when(bolsistaService.buscarPorId(USUARIO_ID)).thenReturn(bolsistaLogado);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Editado", "email", "novo@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Thiago Editado"));

        ArgumentCaptor<Bolsista> captor = ArgumentCaptor.forClass(Bolsista.class);
        verify(bolsistaService).atualizar(captor.capture());
        /* senha em branco nao pode mexer no hash gravado */
        assertTrue(passwordEncoder.matches(SENHA_ATUAL, captor.getValue().getSenha()));
    }

    @Test
    void perfil_comSenhaAtualCorreta_gravaNovoHash() throws Exception {
        when(bolsistaService.buscarPorId(USUARIO_ID)).thenReturn(bolsistaLogado);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "novaSenha123", "confirmaSenha", "novaSenha123")))
                .andExpect(status().isOk());

        ArgumentCaptor<Bolsista> captor = ArgumentCaptor.forClass(Bolsista.class);
        verify(bolsistaService).atualizar(captor.capture());
        assertTrue(passwordEncoder.matches("novaSenha123", captor.getValue().getSenha()));
        assertNotEquals("novaSenha123", captor.getValue().getSenha());
    }

    @Test
    void perfil_comSenhaAtualErrada_recusa() throws Exception {
        when(bolsistaService.buscarPorId(USUARIO_ID)).thenReturn(bolsistaLogado);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", "chuteErrado", "senha", "hackeado123", "confirmaSenha", "hackeado123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha atual informada esta incorreta."));

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comConfirmacaoDiferente_recusa() throws Exception {
        when(bolsistaService.buscarPorId(USUARIO_ID)).thenReturn(bolsistaLogado);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "novaSenha123", "confirmaSenha", "outraCoisa")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comSenhaNovaCurta_recusa() throws Exception {
        when(bolsistaService.buscarPorId(USUARIO_ID)).thenReturn(bolsistaLogado);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com",
                                "senhaAtual", SENHA_ATUAL, "senha", "123", "confirmaSenha", "123")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).atualizar(any());
    }

    @Test
    void perfil_comNomeCurto_recusa() throws Exception {
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
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
        when(professorService.buscarPorId(profId)).thenReturn(professor);
        logarComo(professor);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Dr. Roberto Mendes", "email", "roberto@teste.com")))
                .andExpect(status().isOk());

        verify(professorService).atualizar(any(Professor.class));
        verifyNoInteractions(bolsistaService);
    }

    @Test
    void perfil_quandoOUsuarioSumiuDoBanco_retorna404() throws Exception {
        when(bolsistaService.buscarPorId(USUARIO_ID)).thenReturn(null);
        logarComo(bolsistaLogado);

        mockMvc.perform(patch("/api/auth/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Thiago Rocha", "email", "thiago@teste.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastroAdmin_dentroDoLimite_cria() throws Exception {
        when(bolsistaService.contarAdmins()).thenReturn(1);

        mockMvc.perform(post("/api/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Novo Admin", "email", "novo@teste.com",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoUsuario").value("ADMIN"));

        ArgumentCaptor<Bolsista> captor = ArgumentCaptor.forClass(Bolsista.class);
        verify(bolsistaService).inserir(captor.capture());
        assertTrue(passwordEncoder.matches("123456", captor.getValue().getSenha()));
    }

    @Test
    void cadastroAdmin_noLimite_retorna409() throws Exception {
        when(bolsistaService.contarAdmins()).thenReturn(3);

        mockMvc.perform(post("/api/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Quarto Admin", "email", "quarto@teste.com",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isConflict());

        verify(bolsistaService, never()).inserir(any());
    }

    @Test
    void cadastroAdmin_comEmailInvalido_recusa() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Alguem", "email", "nao-e-email",
                                "senha", "123456", "confirmaSenha", "123456")))
                .andExpect(status().isBadRequest());

        verify(bolsistaService, never()).inserir(any());
    }
}
