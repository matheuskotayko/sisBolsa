package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

import static org.assertj.core.api.Assertions.assertThat;

class ProjetoModelAssemblerTest {

    private final ProjetoModelAssembler assembler = new ProjetoModelAssembler();

    @Test
    @DisplayName("toModel com laboratorioId deve conter self, membros e laboratorio")
    void toModel_ComLaboratorio_DeveConterTodosOsLinks() {
        ProjetoResponse resp = new ProjetoResponse(
                "prj_123", "Projeto NLP", "Descricao NLP",
                "lab_456", "Lab IA", true, 3,
                "https://github.com/org/repo", "https://docs.org");

        EntityModel<ProjetoResponse> model = assembler.toModel(resp);

        assertThat(model).isNotNull();
        assertThat(model.getContent()).isEqualTo(resp);
        assertThat(model.getLink("self")).isPresent();
        assertThat(model.getLink("self").get().getHref()).isEqualTo("/api/v1/projeto/prj_123");
        assertThat(model.getLink("membros")).isPresent();
        assertThat(model.getLink("membros").get().getHref()).isEqualTo("/api/v1/projeto/prj_123/membros");
        assertThat(model.getLink("laboratorio")).isPresent();
        assertThat(model.getLink("laboratorio").get().getHref()).isEqualTo("/api/v1/laboratorio/lab_456");
    }

    @Test
    @DisplayName("toModel sem laboratorioId nao deve conter link de laboratorio")
    void toModel_SemLaboratorio_NaoDeveConterLinkLaboratorio() {
        ProjetoResponse resp = new ProjetoResponse(
                "prj_123", "Projeto NLP", "Descricao NLP",
                null, null, true, 0,
                null, null);

        EntityModel<ProjetoResponse> model = assembler.toModel(resp);

        assertThat(model).isNotNull();
        assertThat(model.getLink("self")).isPresent();
        assertThat(model.getLink("membros")).isPresent();
        assertThat(model.getLink("laboratorio")).isNotPresent();
    }
}
