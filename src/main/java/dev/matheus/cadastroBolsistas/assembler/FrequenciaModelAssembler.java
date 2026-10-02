package dev.matheus.cadastroBolsistas.assembler;

import dev.matheus.cadastroBolsistas.dto.FrequenciaResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Monta o {@link EntityModel} de {@link FrequenciaResponse} com links HATEOAS:
 * <ul>
 *   <li>{@code self} → {@code /api/v1/frequencia/{id}}</li>
 *   <li>{@code bolsista} → {@code /api/v1/bolsista/{bolsistaId}}</li>
 * </ul>
 */
@Component
public class FrequenciaModelAssembler implements RepresentationModelAssembler<FrequenciaResponse, EntityModel<FrequenciaResponse>> {

    @Override
    public EntityModel<FrequenciaResponse> toModel(FrequenciaResponse resp) {
        return EntityModel.of(resp,
                Link.of("/api/v1/frequencia/" + resp.id()).withSelfRel(),
                Link.of("/api/v1/bolsista/" + resp.bolsistaId()).withRel("bolsista"));
    }
}
