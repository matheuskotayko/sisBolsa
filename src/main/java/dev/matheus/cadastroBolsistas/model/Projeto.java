package dev.matheus.cadastroBolsistas.model;

import lombok.Getter;
import lombok.Setter;

import dev.matheus.cadastroBolsistas.util.PublicIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 * projeto vinculado a um laboratorio com id UUID e identificador publico prefixado prj_.
 */
@Getter
@Setter
@Entity
@Table(name = "projeto")
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    private String nome;
    private String descricao;

    @Column(name = "link_repositorio")
    private String linkRepositorio;

    @Column(name = "link_documentacao")
    private String linkDocumentacao;

    @Column(name = "laboratorio_id")
    private UUID laboratorioId;

    @ManyToOne
    @JoinColumn(name = "laboratorio_id", insertable = false, updatable = false)
    private Laboratorio laboratorio;

    private boolean ativo;

    /*
     * lado dono do relacionamento N:N com bolsista. a tabela bolsista_projeto so
     * tem as duas FKs, entao o @JoinTable da conta dela sem entidade intermediaria
     * e sem SQL nativo pra vincular/desvincular.
     */
    @ManyToMany
    @JoinTable(name = "bolsista_projeto",
            joinColumns = @JoinColumn(name = "projeto_id"),
            inverseJoinColumns = @JoinColumn(name = "bolsista_id"))
    private Set<Bolsista> bolsistas = new HashSet<>();

    public Projeto() {
        this.publicId = PublicIdGenerator.generateProjetoId();
    }

    public Projeto(UUID id, String nome, String descricao, UUID laboratorioId, String nomeLaboratorio, boolean ativo) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateProjetoId();
        this.nome = nome;
        this.descricao = descricao;
        this.laboratorioId = laboratorioId;
        this.ativo = ativo;
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateProjetoId();
        }
    }

    public void setLaboratorio(Laboratorio laboratorio) {
        this.laboratorio = laboratorio;
        if (laboratorio != null) {
            this.laboratorioId = laboratorio.getId();
        }
    }

    public String getLaboratorioPublicId() {
        return laboratorio != null ? laboratorio.getPublicId() : null;
    }

    public String getNomeLaboratorio() {
        return laboratorio != null ? laboratorio.getNome() : null;
    }

}
