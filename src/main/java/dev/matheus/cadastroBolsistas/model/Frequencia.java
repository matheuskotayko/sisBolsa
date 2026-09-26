package dev.matheus.cadastroBolsistas.model;

import dev.matheus.cadastroBolsistas.util.PublicIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/*
 * registro de horas trabalhadas por um bolsista com id UUID, identificador publico prefixado frq_ e link de comprovante/entregavel.
 */
@Entity
@Table(name = "frequencia")
public class Frequencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "bolsista_id")
    private UUID bolsistaId;

    @ManyToOne
    @JoinColumn(name = "bolsista_id", insertable = false, updatable = false)
    private Bolsista bolsista;

    private LocalDate data;

    @Column(name = "horas_trabalhadas")
    private double horasTrabalhadas;

    private String descricao;

    @Column(name = "link_comprovante")
    private String linkComprovante;

    private boolean ativo;

    public Frequencia() {
        this.publicId = PublicIdGenerator.generateFrequenciaId();
    }

    public Frequencia(UUID id, UUID bolsistaId, LocalDate data, double horasTrabalhadas, String descricao, boolean ativo) {
        this.id = id;
        this.publicId = PublicIdGenerator.generateFrequenciaId();
        this.bolsistaId = bolsistaId;
        this.data = data;
        this.horasTrabalhadas = horasTrabalhadas;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = PublicIdGenerator.generateFrequenciaId();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public UUID getBolsistaId() { return bolsistaId; }
    public void setBolsistaId(UUID bolsistaId) { this.bolsistaId = bolsistaId; }

    public String getNomeBolsista() {
        return bolsista != null ? bolsista.getNome() : null;
    }

    public String getBolsistaPublicId() {
        return bolsista != null ? bolsista.getPublicId() : null;
    }

    /* preenche o objeto em memoria e ajusta a FK */
    public void setBolsista(Bolsista bolsista) {
        this.bolsista = bolsista;
        if (bolsista != null) {
            this.bolsistaId = bolsista.getId();
        }
    }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public double getHorasTrabalhadas() { return horasTrabalhadas; }
    public void setHorasTrabalhadas(double horasTrabalhadas) { this.horasTrabalhadas = horasTrabalhadas; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getLinkComprovante() { return linkComprovante; }
    public void setLinkComprovante(String linkComprovante) { this.linkComprovante = linkComprovante; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
