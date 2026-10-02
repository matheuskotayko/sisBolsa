package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.assembler.BolsistaModelAssembler;
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
@Import(BolsistaModelAssembler.class)
class BolsistaControllerTest {

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
    void buscar_comPublicId_retornaBolsistaComLinks() throws Exception {
        String publicId = "bol_12345678901234567890";
        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setPublicId(publicId);
        bolsista.setNome("Maria Silva");
        bolsista.setEmail("maria@teste.com");
        bolsista.setAtivo(true);

        when(bolsistaService.buscarComPermissaoDeVisualizacao(eq(publicId), any(Usuario.class)))
                .thenReturn(bolsista);

        mockMvc.perform(get("/api/v1/bolsista/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                // BolsistaModelAssembler gera links self e projetos
                .andExpect(jsonPath("$.links[0].href").value("/api/v1/bolsista/" + publicId))
                .andExpect(jsonPath("$.links[1].rel").value("projetos"));

        verify(bolsistaService).buscarComPermissaoDeVisualizacao(eq(publicId), any(Usuario.class));
    }

    @Test
    void buscar_comLaboratorioVinculado_incluiLinkDeLaboratorio() throws Exception {
        String publicId = "bol_12345678901234567890";
        String labPublicId = "lab_abcdef1234567890abcd";

        // Laboratorio com publicId para que getLaboratorioPublicId() retorne valor não-nulo
        dev.matheus.cadastroBolsistas.model.Laboratorio lab =
                new dev.matheus.cadastroBolsistas.model.Laboratorio();
        lab.setId(UUID.randomUUID());
        lab.setPublicId(labPublicId);
        lab.setNome("Lab Teste");

        Bolsista bolsista = new Bolsista();
        bolsista.setId(UUID.randomUUID());
        bolsista.setPublicId(publicId);
        bolsista.setNome("Carlos Lima");
        bolsista.setEmail("carlos@teste.com");
        bolsista.setAtivo(true);
        bolsista.setLaboratorio(lab);

        when(bolsistaService.buscarComPermissaoDeVisualizacao(eq(publicId), any(Usuario.class)))
                .thenReturn(bolsista);

        mockMvc.perform(get("/api/v1/bolsista/{id}", publicId))
                .andExpect(status().isOk())
                // assembler adiciona link "laboratorio" quando laboratorioId != null
                .andExpect(jsonPath("$.links[?(@.rel=='laboratorio')]").exists());
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

    @Test
    void criar_comDadosValidos_retorna201ComLinks() throws Exception {
        String publicId = "bol_novo1234567890123456";
        Bolsista b = new Bolsista();
        b.setId(UUID.randomUUID());
        b.setPublicId(publicId);
        b.setNome("Lucas Teste");
        b.setEmail("lucas@teste.com");
        b.setTipoUsuario("BOLSISTA");
        b.setAtivo(true);

        when(bolsistaService.criar(any(BolsistaRequest.class), any(Usuario.class))).thenReturn(b);

        String json = """
            {
                "nome": "Lucas Teste",
                "email": "lucas@teste.com",
                "senha": "senha123",
                "curso": "Engenharia",
                "matricula": "2024001",
                "tipoUsuario": "BOLSISTA"
            }
            """;

        mockMvc.perform(post("/api/v1/bolsista")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/bolsista/" + publicId))
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Lucas Teste"))
                .andExpect(jsonPath("$.links[0].href").value("/api/v1/bolsista/" + publicId));

        verify(bolsistaService).criar(any(BolsistaRequest.class), any(Usuario.class));
    }

    @Test
    void atualizar_comDadosValidos_retorna200() throws Exception {
        String publicId = "bol_existente1234567890";
        Bolsista b = new Bolsista();
        b.setId(UUID.randomUUID());
        b.setPublicId(publicId);
        b.setNome("Lucas Atualizado");
        b.setEmail("lucas@teste.com");
        b.setTipoUsuario("BOLSISTA");
        b.setAtivo(true);

        when(bolsistaService.atualizar(eq(publicId), any(BolsistaRequest.class), any(Usuario.class))).thenReturn(b);

        String json = """
            {
                "nome": "Lucas Atualizado",
                "email": "lucas@teste.com",
                "curso": "Engenharia",
                "matricula": "2024001",
                "tipoUsuario": "BOLSISTA"
            }
            """;

        // endpoint é PATCH (atualização parcial — senha em branco preserva a atual)
        mockMvc.perform(patch("/api/v1/bolsista/{id}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.nome").value("Lucas Atualizado"));

        verify(bolsistaService).atualizar(eq(publicId), any(BolsistaRequest.class), any(Usuario.class));
    }

    @Test
    void exportar_retornaCsvEHeaderCorreto() throws Exception {
        when(bolsistaService.gerarCsv(any(Usuario.class))).thenReturn("ID,Nome\n1,Lucas\n");

        mockMvc.perform(get("/api/v1/bolsista/exportar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=bolsistas.csv"))
                .andExpect(content().string("ID,Nome\n1,Lucas\n"));

        verify(bolsistaService).gerarCsv(any(Usuario.class));
    }
}
