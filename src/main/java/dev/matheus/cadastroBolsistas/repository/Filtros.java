package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Projeto;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * filtros dinamicos montados com a Criteria API do proprio JPA.
 *
 * as consultas com parametro opcional (periodo, termo de busca)
 * ficavam em @Query usando o truque ":x IS NULL OR campo = :x", que obriga o
 * banco a avaliar todo filtro mesmo quando nao foi informado. com Specification
 * o predicado so entra no WHERE se o valor existir, e a mesma especificacao
 * serve pro findAll e pro count - sem duplicar a consulta.
 */
public final class Filtros {

    private Filtros() {
    }

    public static Specification<Frequencia> frequencia(UUID bolsistaId, LocalDate inicio, LocalDate fim) {
        return (raiz, consulta, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(raiz.get("ativo")));
            if (bolsistaId != null) {
                ps.add(cb.equal(raiz.get("bolsistaId"), bolsistaId));
            }
            ps.addAll(periodo(raiz.get("data"), inicio, fim, cb));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    public static Specification<Frequencia> frequenciaDeVarios(List<UUID> bolsistaIds,
                                                               LocalDate inicio, LocalDate fim) {
        return (raiz, consulta, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(raiz.get("ativo")));
            ps.add(raiz.get("bolsistaId").in(bolsistaIds));
            ps.addAll(periodo(raiz.get("data"), inicio, fim, cb));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    public static Specification<Projeto> projeto(String buscaNome, UUID labId) {
        return (raiz, consulta, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(raiz.get("ativo")));
            if (temTermo(buscaNome)) {
                ps.add(cb.or(contem(raiz.get("nome"), buscaNome, cb),
                        contem(raiz.get("descricao"), buscaNome, cb)));
            }
            if (labId != null) {
                ps.add(cb.equal(raiz.get("laboratorioId"), labId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    public static Specification<Projeto> projeto(String buscaNome, String labPublicId) {
        return (raiz, consulta, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(raiz.get("ativo")));
            if (temTermo(buscaNome)) {
                ps.add(cb.or(contem(raiz.get("nome"), buscaNome, cb),
                        contem(raiz.get("descricao"), buscaNome, cb)));
            }
            if (labPublicId != null && !labPublicId.isBlank()) {
                ps.add(cb.equal(raiz.join("laboratorio", JoinType.INNER).get("publicId"), labPublicId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    public static Specification<Laboratorio> laboratorio(String buscaNome) {
        return (raiz, consulta, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(raiz.get("ativo")));
            if (temTermo(buscaNome)) {
                /* o coordenador e opcional, entao o join precisa ser LEFT pra nao sumir com labs sem coordenador. */
                Path<?> coordenador = raiz.join("coordenadorProfessor", JoinType.LEFT).get("nome");
                ps.add(cb.or(contem(raiz.get("nome"), buscaNome, cb),
                        contem(raiz.get("areaPesquisa"), buscaNome, cb),
                        contem(coordenador, buscaNome, cb)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static boolean temTermo(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Predicate contem(Path<?> campo, String termo, CriteriaBuilder cb) {
        return cb.like(cb.lower(campo.as(String.class)), "%" + termo.toLowerCase() + "%");
    }

    private static <T extends Comparable<? super T>> List<Predicate> periodo(
            Path<T> campo, T inicio, T fim, CriteriaBuilder cb) {
        List<Predicate> ps = new ArrayList<>();
        if (inicio != null) {
            ps.add(cb.greaterThanOrEqualTo(campo, inicio));
        }
        if (fim != null) {
            ps.add(cb.lessThanOrEqualTo(campo, fim));
        }
        return ps;
    }
}
