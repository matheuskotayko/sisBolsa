package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.LaboratorioResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class LaboratorioModelAssembler implements RepresentationModelAssembler<LaboratorioResponse, EntityModel<LaboratorioResponse>> {

    @Override
    public EntityModel<LaboratorioResponse> toModel(LaboratorioResponse resp) {
        EntityModel<LaboratorioResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/laboratorio/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/laboratorio/" + resp.id() + "/bolsistas").withRel("bolsistas"),
                Link.of("/api/v1/laboratorio/" + resp.id() + "/projetos").withRel("projetos"));
        if (resp.coordenadorId() != null && !resp.coordenadorId().isBlank()) {
            modelo.add(Link.of("/api/v1/professor/" + resp.coordenadorId()).withRel("coordenador"));
        }
        return modelo;
    }
}
