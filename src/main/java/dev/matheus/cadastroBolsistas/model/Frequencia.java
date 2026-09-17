package dev.matheus.cadastroBolsistas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/*
 * registro de horas trabalhadas por um bolsista com id UUID e link de comprovante/entregavel.
 */
@Entity
@Table(name = "frequencia")
public class Frequencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    public Frequencia() {}

    public Frequencia(UUID id, UUID bolsistaId, LocalDate data, double horasTrabalhadas, String descricao, boolean ativo) {
        this.id = id;
        this.bolsistaId = bolsistaId;
        this.data = data;
        this.horasTrabalhadas = horasTrabalhadas;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBolsistaId() { return bolsistaId; }
    public void setBolsistaId(UUID bolsistaId) { this.bolsistaId = bolsistaId; }

    public String getNomeBolsista() {
        return bolsista != null ? bolsista.getNome() : null;
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
