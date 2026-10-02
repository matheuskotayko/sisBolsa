package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.ProjetoResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ProjetoModelAssembler implements RepresentationModelAssembler<ProjetoResponse, EntityModel<ProjetoResponse>> {

    @Override
    public EntityModel<ProjetoResponse> toModel(ProjetoResponse resp) {
        EntityModel<ProjetoResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/projeto/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/projeto/" + resp.id() + "/membros").withRel("membros"));
        if (resp.laboratorioId() != null && !resp.laboratorioId().isBlank()) {
            modelo.add(Link.of("/api/v1/laboratorio/" + resp.laboratorioId()).withRel("laboratorio"));
        }
        return modelo;
    }
}
