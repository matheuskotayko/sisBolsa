package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

import static org.junit.jupiter.api.Assertions.*;

class BolsistaModelAssemblerTest {

    private final BolsistaModelAssembler assembler = new BolsistaModelAssembler();

    private UsuarioResponse responseBase(String id, String laboratorioId) {
        return new UsuarioResponse(
                id, "Ana Pereira", "ana@teste.com", "BOLSISTA",
                null, null,  // fotoUrl, bio
                true,        // ativo
                null, null, null, null, null, // curso, matricula, cpf, telefone, dataNascimento
                laboratorioId, null, null,    // laboratorioId, nomeLaboratorio, cargo
                null, null, null, null, null, // modalidadeBolsa, modalidadeBolsaDescricao, valorBolsa, dataInicioBolsa, dataFimBolsa
                false, false);               // bolsaVencida, bolsaPrestesAVencer
    }

    @Test
    void toModel_semLaboratorio_geraLinksBasicos() {
        UsuarioResponse resp = responseBase("bol_abc123", null);

        EntityModel<UsuarioResponse> model = assembler.toModel(resp);

        assertTrue(model.getLink("self").isPresent());
        assertTrue(model.getLink("projetos").isPresent());
        assertTrue(model.getLink("self").get().getHref().contains("bol_abc123"));
        assertTrue(model.getLink("projetos").get().getHref().contains("/projetos"));
        assertFalse(model.getLink("laboratorio").isPresent());
    }

    @Test
    void toModel_comLaboratorio_incluiLinkDeLaboratorio() {
        String labPublicId = "lab_xyz789";
        UsuarioResponse resp = responseBase("bol_abc123", labPublicId);

        EntityModel<UsuarioResponse> model = assembler.toModel(resp);

        assertTrue(model.getLink("laboratorio").isPresent());
        assertTrue(model.getLink("laboratorio").get().getHref().contains(labPublicId));
    }
}
