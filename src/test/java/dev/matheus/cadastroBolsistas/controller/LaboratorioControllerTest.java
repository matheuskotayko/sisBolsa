package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.request.LaboratorioRequest;
import dev.matheus.cadastroBolsistas.dto.response.LaboratorioResponse;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.infra.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.infra.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
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

@WebMvcTest(controllers = LaboratorioController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class LaboratorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LaboratorioService laboratorioService;

    @MockitoBean
    private BolsistaService bolsistaService;

    @MockitoBean
    private ProjetoService projetoService;

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
    @DisplayName("GET /api/v1/laboratorio/{id} deve retornar 200 ")
    void buscar_DeveRetornar200ComLinks() throws Exception {
        Laboratorio lab = new Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId("lab_001");
        lab.setNome("Lab Sistemas");
        lab.setAreaPesquisa("Engenharia");
        lab.setCapacidade(10);
        lab.setAtivo(true);

        when(laboratorioService.detalhar("lab_001")).thenReturn(LaboratorioResponse.from(lab, 2));

        mockMvc.perform(get("/api/v1/laboratorio/lab_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lab_001"))
                .andExpect(jsonPath("$.nome").value("Lab Sistemas"));
    }

    @Test
    @DisplayName("POST /api/v1/laboratorio deve retornar 201 Location e ")
    void criar_DeveRetornar201() throws Exception {
        Laboratorio lab = new Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId("lab_new");
        lab.setNome("Novo Lab");
        lab.setCapacidade(15);
        lab.setAtivo(true);

        when(laboratorioService.criar(any(LaboratorioRequest.class))).thenReturn(LaboratorioResponse.from(lab));

        String json = """
                {
                    "nome": "Novo Lab",
                    "areaPesquisa": "IA",
                    "status": "Ativo",
                    "capacidade": 15
                }
                """;

        mockMvc.perform(post("/api/v1/laboratorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/laboratorio/lab_new")))
                .andExpect(jsonPath("$.id").value("lab_new"));

        verify(laboratorioService).criar(any(LaboratorioRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/laboratorio/{id} deve retornar 200 com laboratório atualizado")
    void atualizar_DeveRetornar200() throws Exception {
        Laboratorio lab = new Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId("lab_001");
        lab.setNome("Lab Atualizado");
        lab.setCapacidade(20);
        lab.setAtivo(true);

        when(laboratorioService.atualizar(eq("lab_001"), any(LaboratorioRequest.class), any(Usuario.class))).thenReturn(LaboratorioResponse.from(lab));

        String json = """
                {
                    "nome": "Lab Atualizado",
                    "areaPesquisa": "IA",
                    "status": "Ativo",
                    "capacidade": 20
                }
                """;

        mockMvc.perform(patch("/api/v1/laboratorio/lab_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lab_001"))
                .andExpect(jsonPath("$.nome").value("Lab Atualizado"));

        verify(laboratorioService).atualizar(eq("lab_001"), any(LaboratorioRequest.class), any(Usuario.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/laboratorio/{id} deve retornar 204")
    void excluir_DeveRetornar204() throws Exception {
        mockMvc.perform(delete("/api/v1/laboratorio/lab_001"))
                .andExpect(status().isNoContent());

        verify(laboratorioService).excluir(eq("lab_001"), any(Usuario.class));
    }
}
