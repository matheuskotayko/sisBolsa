package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.controller.BolsistaController;
import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Monta o {@link EntityModel} de {@link UsuarioResponse} para bolsistas com os links HATEOAS:
 * <ul>
 *   <li>{@code self} → {@code /api/v1/bolsista/{id}}</li>
 *   <li>{@code projetos} → {@code /api/v1/bolsista/{id}/projetos}</li>
 *   <li>{@code laboratorio} → {@code /api/v1/laboratorio/{laboratorioId}} (se vinculado)</li>
 * </ul>
 */
@Component
public class BolsistaModelAssembler implements RepresentationModelAssembler<UsuarioResponse, EntityModel<UsuarioResponse>> {

    @Override
    public EntityModel<UsuarioResponse> toModel(UsuarioResponse resp) {
        EntityModel<UsuarioResponse> modelo = EntityModel.of(resp,
                Link.of("/api/v1/bolsista/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/bolsista/" + resp.id() + "/projetos").withRel("projetos"));

        if (resp.laboratorioId() != null) {
            modelo.add(Link.of("/api/v1/laboratorio/" + resp.laboratorioId()).withRel("laboratorio"));
        }

        return modelo;
    }
}
