package dev.matheus.cadastroBolsistas.controller;

import dev.matheus.cadastroBolsistas.model.Curso;
import dev.matheus.cadastroBolsistas.security.JwtCookieFilter;
import dev.matheus.cadastroBolsistas.security.SecurityConfig;
import dev.matheus.cadastroBolsistas.service.CursoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CursoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtCookieFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class CursoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoService cursoService;

    private Curso cursoFake(String nome) {
        Curso c = new Curso();
        c.setId(UUID.randomUUID());
        c.setNome(nome);
        c.setAtivo(true);
        return c;
    }

    @Test
    void listar_retornaCursosAtivos() throws Exception {
        when(cursoService.listarTodos()).thenReturn(new ArrayList<>(List.of(
                cursoFake("Engenharia de Computação"),
                cursoFake("Sistemas para Internet"))));

        mockMvc.perform(get("/api/v1/curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Engenharia de Computação"));
    }

    @Test
    void criar_comNomeValido_retorna201() throws Exception {
        Curso curso = cursoFake("Análise e Desenvolvimento de Sistemas");
        when(cursoService.cadastrar(anyString())).thenReturn(curso);

        mockMvc.perform(post("/api/v1/curso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Análise e Desenvolvimento de Sistemas\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Análise e Desenvolvimento de Sistemas"));

        verify(cursoService).cadastrar("Análise e Desenvolvimento de Sistemas");
    }

    @Test
    void criar_comNomeEmBranco_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/curso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"   \"}"))
                .andExpect(status().isBadRequest());

        verify(cursoService, never()).cadastrar(anyString());
    }

    @Test
    void criar_comNomeComEspacos_normalizaComTrim() throws Exception {
        Curso curso = cursoFake("Ciência da Computação");
        when(cursoService.cadastrar("Ciência da Computação")).thenReturn(curso);

        mockMvc.perform(post("/api/v1/curso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"  Ciência da Computação  \"}"))
                .andExpect(status().isCreated());

        // compact constructor deve ter feito o trim antes de chamar o service
        verify(cursoService).cadastrar("Ciência da Computação");
    }
}
