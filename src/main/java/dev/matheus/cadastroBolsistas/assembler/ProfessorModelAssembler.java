package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.UsuarioResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Monta o {@link EntityModel} de {@link UsuarioResponse} para professores com os links HATEOAS:
 * <ul>
 *   <li>{@code self} → {@code /api/v1/professor/{id}}</li>
 * </ul>
 */
@Component
public class ProfessorModelAssembler implements RepresentationModelAssembler<UsuarioResponse, EntityModel<UsuarioResponse>> {

    @Override
    public EntityModel<UsuarioResponse> toModel(UsuarioResponse resp) {
        return EntityModel.of(resp, Link.of("/api/v1/professor/" + resp.id()).withSelfRel());
    }
}
