package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.assembler.FrequenciaModelAssembler;
import dev.matheus.cadastroBolsistas.dto.FrequenciaRequest;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Usuario;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.BolsistaService;
import dev.matheus.cadastroBolsistas.service.ComprovanteFrequenciaPdfService;
import dev.matheus.cadastroBolsistas.service.FrequenciaService;
import dev.matheus.cadastroBolsistas.service.LaboratorioService;
import dev.matheus.cadastroBolsistas.service.ProfessorService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FrequenciaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(FrequenciaModelAssembler.class)
class FrequenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FrequenciaService frequenciaService;
    @MockitoBean private BolsistaService bolsistaService;
    @MockitoBean private LaboratorioService laboratorioService;
    @MockitoBean private ComprovanteFrequenciaPdfService comprovantePdfService;
    @MockitoBean private ProfessorService professorService;

    private Usuario adminLogado;

    @BeforeEach
    void setUp() {
        adminLogado = new Usuario();
        adminLogado.setId(UUID.randomUUID());
        adminLogado.setNome("Admin Teste");
        adminLogado.setEmail("admin@teste.com");
        adminLogado.setTipoUsuario("ADMIN");

        var auth = new UsernamePasswordAuthenticationToken(
                adminLogado, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Frequencia frequenciaFake(String publicId, String bolsistaPublicId) {
        Bolsista b = new Bolsista();
        b.setId(UUID.randomUUID());
        b.setPublicId(bolsistaPublicId);
        b.setNome("Lucas");

        Frequencia f = new Frequencia();
        f.setId(UUID.randomUUID());
        f.setPublicId(publicId);
        f.setBolsista(b);
        f.setData(LocalDate.of(2026, 9, 1));
        f.setHorasTrabalhadas(4.0);
        f.setDescricao("Implementação");
        f.setAtivo(true);
        return f;
    }

    @Test
    void buscar_comId_retornaFrequenciaComLinks() throws Exception {
        String publicId = "frq_abc123456789012345678";
        Frequencia f = frequenciaFake(publicId, "bol_xyz789");

        when(frequenciaService.buscarComPermissao(eq(publicId), any(Usuario.class))).thenReturn(f);

        mockMvc.perform(get("/api/v1/frequencia/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.links[0].rel").value("self"))
                .andExpect(jsonPath("$.links[1].rel").value("bolsista"));
    }

    @Test
    void registrar_comDadosValidos_retorna201ComLinks() throws Exception {
        String bolsistaPublicId = "bol_xyz789";
        String freqPublicId = "frq_novo123456789012345";
        Bolsista alvo = new Bolsista();
        alvo.setId(UUID.randomUUID());
        alvo.setPublicId(bolsistaPublicId);
        Frequencia f = frequenciaFake(freqPublicId, bolsistaPublicId);

        when(frequenciaService.resolverBolsistaAlvo(any(Usuario.class), eq(bolsistaPublicId))).thenReturn(alvo);
        when(frequenciaService.registrar(any(FrequenciaRequest.class), eq(alvo))).thenReturn(f);

        String json = """
            {
                "bolsistaId": "%s",
                "data": "2026-09-01",
                "horasTrabalhadas": 4.0,
                "descricao": "Implementação de testes unitários"
            }
            """.formatted(bolsistaPublicId);

        mockMvc.perform(post("/api/v1/frequencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(freqPublicId)))
                .andExpect(jsonPath("$.id").value(freqPublicId))
                .andExpect(jsonPath("$.links[0].rel").value("self"));

        verify(frequenciaService).registrar(any(FrequenciaRequest.class), eq(alvo));
    }

    @Test
    void atualizar_comDadosValidos_retorna200() throws Exception {
        String publicId = "frq_abc123456789012345678";
        Frequencia f = frequenciaFake(publicId, "bol_xyz789");

        when(frequenciaService.buscarComPermissao(eq(publicId), any(Usuario.class))).thenReturn(f);

        String json = """
            {
                "data": "2026-09-02",
                "horasTrabalhadas": 6.0,
                "descricao": "Revisão de código e documentação"
            }
            """;

        // endpoint é PATCH (atualização parcial)
        mockMvc.perform(patch("/api/v1/frequencia/{id}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].rel").value("self"));

        verify(frequenciaService).atualizar(f);
    }

    @Test
    void excluir_comId_retorna204() throws Exception {
        String publicId = "frq_abc123456789012345678";
        Frequencia f = frequenciaFake(publicId, "bol_xyz789");

        when(frequenciaService.buscarComPermissao(eq(publicId), any(Usuario.class))).thenReturn(f);

        mockMvc.perform(delete("/api/v1/frequencia/{id}", publicId))
                .andExpect(status().isNoContent());

        verify(frequenciaService).excluir(publicId);
    }

    @Test
    void registrar_semDescricao_retorna400() throws Exception {
        String json = """
            {
                "data": "2026-09-01",
                "horasTrabalhadas": 4.0
            }
            """;

        mockMvc.perform(post("/api/v1/frequencia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(frequenciaService, never()).registrar(any(FrequenciaRequest.class), any(Bolsista.class));
    }
}
