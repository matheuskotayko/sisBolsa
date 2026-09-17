package dev.matheus.cadastroBolsistas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.UUID;

/*
 * laboratorio de pesquisa com id UUID e coordenador_id UUID.
 */
@Entity
@Table(name = "laboratorio")
public class Laboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    @Column(name = "area_pesquisa")
    private String areaPesquisa;

    private String status;
    private int capacidade;

    @Column(name = "coordenador_id")
    private UUID coordenadorId;

    @ManyToOne
    @JoinColumn(name = "coordenador_id", insertable = false, updatable = false)
    private Professor coordenadorProfessor;

    private boolean ativo;

    @Transient
    private int totalBolsistas;
    @Transient
    private ArrayList<Projeto> projetos = new ArrayList<>();

    public Laboratorio() {}

    public Laboratorio(UUID id, String nome, String areaPesquisa, String status, int capacidade,
                       UUID coordenadorId, String coordenador, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.areaPesquisa = areaPesquisa;
        this.status = status;
        this.capacidade = capacidade;
        this.coordenadorId = coordenadorId;
        this.ativo = ativo;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getAreaPesquisa() { return areaPesquisa; }
    public void setAreaPesquisa(String areaPesquisa) { this.areaPesquisa = areaPesquisa; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCapacidade() { return capacidade; }
    public void setCapacidade(int capacidade) { this.capacidade = capacidade; }

    public UUID getCoordenadorId() { return coordenadorId; }
    public void setCoordenadorId(UUID coordenadorId) { this.coordenadorId = coordenadorId; }

    public String getCoordenador() {
        return coordenadorProfessor != null ? coordenadorProfessor.getNome() : null;
    }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public ArrayList<Projeto> getProjetos() { return projetos; }
    public void setProjetos(ArrayList<Projeto> projetos) { this.projetos = projetos; }

    public int getTotalBolsistas() { return totalBolsistas; }
    public void setTotalBolsistas(int totalBolsistas) { this.totalBolsistas = totalBolsistas; }
}
