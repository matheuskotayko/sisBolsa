package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.BolsistaRequest;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BolsistaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class BolsistaControllerTest {

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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BolsistaService bolsistaService;

    @MockitoBean
    private ProjetoService projetoService;

    private Usuario adminLogado;

    @BeforeEach
    void setUp() {
        adminLogado = new Usuario();
        adminLogado.setId(UUID.randomUUID());
        adminLogado.setPublicId("usr_admin12345678901234");
        adminLogado.setNome("Admin Teste");
        adminLogado.setEmail("admin@teste.com");
        adminLogado.setTipoUsuario("ADMIN");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminLogado, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buscar_comPublicId_retornaBolsistaComPublicId() throws Exception {
        String publicId = "bol_12345678901234567890";
        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setPublicId(publicId);
        bolsista.setNome("Maria Silva");
        bolsista.setEmail("maria@teste.com");
        bolsista.setAtivo(true);

        when(bolsistaService.buscarComPermissaoDeVisualizacao(eq(publicId), any(Usuario.class)))
                .thenReturn(bolsista);
        when(projetoService.listarPorBolsista(publicId)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/bolsista/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.links[0].href").value("/api/v1/bolsista/" + publicId));

        verify(bolsistaService).buscarComPermissaoDeVisualizacao(eq(publicId), any(Usuario.class));
    }

    @Test
    void excluir_comPublicId_executaSoftDeleteERetorna204() throws Exception {
        String publicId = "bol_12345678901234567890";
        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setPublicId(publicId);

        when(bolsistaService.buscarComPermissaoDeExclusao(eq(publicId), any(Usuario.class)))
                .thenReturn(bolsista);

        mockMvc.perform(delete("/api/v1/bolsista/{id}", publicId))
                .andExpect(status().isNoContent());

        verify(bolsistaService).excluir(publicId);
    }
}
