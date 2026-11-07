package dev.matheus.cadastroBolsistas.model;

import lombok.Getter;
import lombok.Setter;

import dev.matheus.cadastroBolsistas.util.PublicIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

/*
 * curso de graduacao do bolsista. lista comeca fixa mas admin pode
 * adicionar novos em tempo de execucao (por isso e entidade, nao enum).
 * Identificador publico prefixado cur_.
 */
@Getter
@Setter
@Entity
@Table(name = "curso")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    private String nome;

    private boolean ativo;

    public Curso() {
        this.publicId = PublicIdGenerator.generateCursoId();
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateCursoId();
        }
    }

}
