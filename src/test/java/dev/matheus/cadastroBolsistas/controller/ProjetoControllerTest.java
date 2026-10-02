package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.assembler.ProjetoModelAssembler;
import dev.matheus.cadastroBolsistas.dto.ProjetoRequest;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Projeto;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ProjetoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(controllers = ProjetoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(ProjetoModelAssembler.class)
class ProjetoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjetoService projetoService;

    @MockitoBean
    private BolsistaService bolsistaService;

    private Usuario adminLogado;

    @BeforeEach
    void setUp() {
        adminLogado = new Usuario();
        adminLogado.setId(UUID.randomUUID());
        adminLogado.setPublicId("adm_123");
        adminLogado.setNome("Admin Teste");
        adminLogado.setEmail("admin@teste.com");
        adminLogado.setTipoUsuario("ADMIN");
        adminLogado.setAtivo(true);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminLogado, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/projeto/{id} deve retornar 200 com links HATEOAS")
    void buscar_DeveRetornar200ComLinks() throws Exception {
        Laboratorio lab = new Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId("lab_001");
        lab.setNome("Lab Sistemas");

        Projeto proj = new Projeto();
        proj.setId(UUID.randomUUID());
        proj.setPublicId("prj_001");
        proj.setNome("Projeto IA");
        proj.setDescricao("Descricao");
        proj.setLaboratorio(lab);
        proj.setAtivo(true);

        when(projetoService.buscarOuFalhar("prj_001")).thenReturn(proj);
        when(projetoService.contarMembros(proj.getId())).thenReturn(3);

        mockMvc.perform(get("/api/v1/projeto/prj_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prj_001"))
                .andExpect(jsonPath("$.nome").value("Projeto IA"))
                .andExpect(jsonPath("$.links[?(@.rel == 'self')].href").value("/api/v1/projeto/prj_001"))
                .andExpect(jsonPath("$.links[?(@.rel == 'membros')].href").value("/api/v1/projeto/prj_001/membros"))
                .andExpect(jsonPath("$.links[?(@.rel == 'laboratorio')].href").value("/api/v1/laboratorio/lab_001"));
    }

    @Test
    @DisplayName("POST /api/v1/projeto deve retornar 201 com Location e links HATEOAS")
    void criar_DeveRetornar201() throws Exception {
        Laboratorio lab = new Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId("lab_001");
        lab.setNome("Lab Sistemas");

        Projeto p = new Projeto();
        p.setId(UUID.randomUUID());
        p.setPublicId("prj_new");
        p.setNome("Novo Projeto");
        p.setLaboratorio(lab);
        p.setAtivo(true);

        when(projetoService.criar(any(ProjetoRequest.class), any(Usuario.class))).thenReturn(p);

        String json = """
                {
                    "nome": "Novo Projeto",
                    "descricao": "Descricao do projeto",
                    "laboratorioId": "lab_001"
                }
                """;

        mockMvc.perform(post("/api/v1/projeto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/projeto/prj_new")))
                .andExpect(jsonPath("$.id").value("prj_new"))
                .andExpect(jsonPath("$.links[?(@.rel == 'self')].href").value("/api/v1/projeto/prj_new"));

        verify(projetoService).criar(any(ProjetoRequest.class), any(Usuario.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/projeto/{id} deve retornar 200 com projeto atualizado")
    void atualizar_DeveRetornar200() throws Exception {
        Laboratorio lab = new Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId("lab_001");
        lab.setNome("Lab Sistemas");

        Projeto proj = new Projeto();
        proj.setId(UUID.randomUUID());
        proj.setPublicId("prj_001");
        proj.setNome("Projeto Atualizado");
        proj.setLaboratorio(lab);
        proj.setAtivo(true);

        when(projetoService.atualizar(eq("prj_001"), any(ProjetoRequest.class), any(Usuario.class))).thenReturn(proj);

        String json = """
                {
                    "nome": "Projeto Atualizado",
                    "descricao": "Nova descricao",
                    "laboratorioId": "lab_001"
                }
                """;

        mockMvc.perform(patch("/api/v1/projeto/prj_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prj_001"))
                .andExpect(jsonPath("$.nome").value("Projeto Atualizado"));

        verify(projetoService).atualizar(eq("prj_001"), any(ProjetoRequest.class), any(Usuario.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/projeto/{id} deve retornar 204")
    void excluir_DeveRetornar204() throws Exception {
        Projeto proj = new Projeto();
        proj.setId(UUID.randomUUID());
        proj.setPublicId("prj_001");

        when(projetoService.buscarExigindoGerencia(eq("prj_001"), any(Usuario.class))).thenReturn(proj);

        mockMvc.perform(delete("/api/v1/projeto/prj_001"))
                .andExpect(status().isNoContent());

        verify(projetoService).excluir("prj_001");
    }
}
