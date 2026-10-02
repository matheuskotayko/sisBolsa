package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.FrequenciaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FrequenciaModelAssemblerTest {

    private final FrequenciaModelAssembler assembler = new FrequenciaModelAssembler();

    private FrequenciaResponse response(String id, String bolsistaId) {
        return new FrequenciaResponse(id, bolsistaId, "Lucas Oliveira",
                LocalDate.of(2026, 9, 1), 4.0, "Implementação de testes", null, true);
    }

    @Test
    void toModel_geraLinkSelfEBolsista() {
        FrequenciaResponse resp = response("frq_abc123", "bol_xyz789");

        EntityModel<FrequenciaResponse> model = assembler.toModel(resp);

        assertTrue(model.getLink("self").isPresent());
        assertTrue(model.getLink("bolsista").isPresent());
        assertTrue(model.getLink("self").get().getHref().contains("frq_abc123"));
        assertTrue(model.getLink("bolsista").get().getHref().contains("bol_xyz789"));
    }

    @Test
    void toModel_conteudoDoModeloEstaCorreto() {
        FrequenciaResponse resp = response("frq_abc123", "bol_xyz789");

        EntityModel<FrequenciaResponse> model = assembler.toModel(resp);

        assertEquals(resp, model.getContent());
        assertEquals(2, model.getLinks().toList().size());
    }
}
