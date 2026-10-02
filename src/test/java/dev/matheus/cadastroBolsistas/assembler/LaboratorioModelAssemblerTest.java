package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.LaboratorioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

import static org.assertj.core.api.Assertions.assertThat;

class LaboratorioModelAssemblerTest {

    private final LaboratorioModelAssembler assembler = new LaboratorioModelAssembler();

    @Test
    @DisplayName("toModel deve incluir self, bolsistas, projetos e coordenador quando presente")
    void toModel_ComCoordenador_DeveConterTodosOsLinks() {
        LaboratorioResponse resp = new LaboratorioResponse(
                "lab_123", "Lab IA", "IA", "Ativo", 20,
                "prf_456", "Prof. Alan", true, 5, 25.0);

        EntityModel<LaboratorioResponse> model = assembler.toModel(resp);

        assertThat(model).isNotNull();
        assertThat(model.getContent()).isEqualTo(resp);
        assertThat(model.getLink("self")).isPresent();
        assertThat(model.getLink("self").get().getHref()).isEqualTo("/api/v1/laboratorio/lab_123");
        assertThat(model.getLink("bolsistas")).isPresent();
        assertThat(model.getLink("bolsistas").get().getHref()).isEqualTo("/api/v1/laboratorio/lab_123/bolsistas");
        assertThat(model.getLink("projetos")).isPresent();
        assertThat(model.getLink("projetos").get().getHref()).isEqualTo("/api/v1/laboratorio/lab_123/projetos");
        assertThat(model.getLink("coordenador")).isPresent();
        assertThat(model.getLink("coordenador").get().getHref()).isEqualTo("/api/v1/professor/prf_456");
    }

    @Test
    @DisplayName("toModel sem coordenador nao deve incluir link de coordenador")
    void toModel_SemCoordenador_NaoDeveConterLinkCoordenador() {
        LaboratorioResponse resp = new LaboratorioResponse(
                "lab_123", "Lab IA", "IA", "Ativo", 20,
                null, null, true, 0, 0.0);

        EntityModel<LaboratorioResponse> model = assembler.toModel(resp);

        assertThat(model).isNotNull();
        assertThat(model.getLink("self")).isPresent();
        assertThat(model.getLink("bolsistas")).isPresent();
        assertThat(model.getLink("projetos")).isPresent();
        assertThat(model.getLink("coordenador")).isNotPresent();
    }
}
