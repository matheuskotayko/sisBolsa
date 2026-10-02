package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

import static org.junit.jupiter.api.Assertions.*;

class ProfessorModelAssemblerTest {

    private final ProfessorModelAssembler assembler = new ProfessorModelAssembler();

    @Test
    void toModel_geraLinkSelfCorretamente() {
        UsuarioResponse resp = new UsuarioResponse(
                "prf_123456", "Dr. Carlos", "carlos@teste.com", "PROFESSOR",
                null, null, true, null, null, null, null, null, null,
                "Lab IA", null, null, null, null, null, null, false, false);

        EntityModel<UsuarioResponse> model = assembler.toModel(resp);

        assertNotNull(model);
        assertEquals(resp, model.getContent());
        assertTrue(model.getLink("self").isPresent());
        assertEquals("/api/v1/professor/prf_123456", model.getLink("self").get().getHref());
    }
}
