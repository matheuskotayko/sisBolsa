package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.dto.ProfessorRequest;
import dev.matheus.cadastroBolsistas.model.Professor;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;
import dev.matheus.cadastroBolsistas.assembler.ProfessorModelAssembler;

@WebMvcTest(controllers = ProfessorController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(ProfessorModelAssembler.class)
class ProfessorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorService professorService;

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
    void listar_retorna200EProfessores() throws Exception {
        Professor p = new Professor();
        p.setId(UUID.randomUUID());
        p.setPublicId("prf_12345678901234567890");
        p.setNome("Dr. Roberto");
        p.setEmail("roberto@teste.com");
        p.setNomeLaboratorio("Lab IA");
        p.setAtivo(true);

        when(professorService.listarComLaboratorio(null)).thenReturn(new ArrayList<>(List.of(p)));

        mockMvc.perform(get("/api/v1/professor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value("prf_12345678901234567890"))
                .andExpect(jsonPath("$.itens[0].nome").value("Dr. Roberto"))
                .andExpect(jsonPath("$.itens[0].nomeLaboratorio").value("Lab IA"));

        verify(professorService).listarComLaboratorio(null);
    }

    @Test
    void buscar_comPublicId_retornaProfessor() throws Exception {
        String publicId = "prf_12345678901234567890";
        Professor p = new Professor();
        p.setId(UUID.randomUUID());
        p.setPublicId(publicId);
        p.setNome("Dr. Roberto");
        p.setEmail("roberto@teste.com");
        p.setNomeLaboratorio("Lab IA");
        p.setAtivo(true);

        when(professorService.buscarComLaboratorio(publicId)).thenReturn(p);

        mockMvc.perform(get("/api/v1/professor/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Dr. Roberto"))
                .andExpect(jsonPath("$.links[0].href").value("/api/v1/professor/" + publicId));

        verify(professorService).buscarComLaboratorio(publicId);
    }

    @Test
    void criar_comDadosValidos_retorna201ECriaProfessor() throws Exception {
        String publicId = "prf_novo1234567890123456";
        Professor p = new Professor();
        p.setId(UUID.randomUUID());
        p.setPublicId(publicId);
        p.setNome("Novo Prof");
        p.setEmail("novo@teste.com");
        p.setAtivo(true);

        when(professorService.criar(any(ProfessorRequest.class))).thenReturn(p);

        String json = """
            {
                "nome": "Novo Prof",
                "email": "novo@teste.com",
                "senha": "senha123"
            }
            """;

        mockMvc.perform(post("/api/v1/professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/professor/" + publicId))
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Novo Prof"));

        verify(professorService).criar(any(ProfessorRequest.class));
    }

    @Test
    void atualizar_comDadosValidos_retorna200EAtualizaProfessor() throws Exception {
        String publicId = "prf_existente1234567890";
        Professor p = new Professor();
        p.setId(UUID.randomUUID());
        p.setPublicId(publicId);
        p.setNome("Prof Editado");
        p.setEmail("roberto@teste.com");
        p.setAtivo(true);

        when(professorService.atualizar(eq(publicId), any(ProfessorRequest.class))).thenReturn(p);

        String json = """
            {
                "nome": "Prof Editado",
                "email": "roberto@teste.com"
            }
            """;

        mockMvc.perform(patch("/api/v1/professor/{id}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Prof Editado"));

        verify(professorService).atualizar(eq(publicId), any(ProfessorRequest.class));
    }

    @Test
    void excluir_comPublicId_retorna204() throws Exception {
        String publicId = "prf_12345678901234567890";
        when(professorService.excluir(publicId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/professor/{id}", publicId))
                .andExpect(status().isNoContent());

        verify(professorService).excluir(publicId);
    }
}
