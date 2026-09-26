package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.AdministradorResponse;
import dev.matheus.cadastroBolsistas.model.Administrador;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.AdministradorService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdministradorController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AdministradorControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        UsuarioLogado usuarioLogado() {
            return new UsuarioLogado();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministradorService service;

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
    void buscar_comPublicId_retornaAdminComPublicId() throws Exception {
        String publicId = "adm_12345678901234567890";
        Administrador admin = new Administrador();
        admin.setPublicId(publicId);
        admin.setNome("Carlos Souza");
        admin.setEmail("carlos@teste.com");
        admin.setAtivo(true);

        when(service.buscarPorId(publicId)).thenReturn(admin);

        mockMvc.perform(get("/api/v1/administrador/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Carlos Souza"));

        verify(service).buscarPorId(publicId);
    }

    @Test
    void desativar_comPublicId_retorna204() throws Exception {
        String publicId = "adm_12345678901234567890";

        mockMvc.perform(delete("/api/v1/administrador/{id}", publicId))
                .andExpect(status().isNoContent());

        verify(service).desativar(eq(publicId), any(Usuario.class));
    }
}
