package dev.matheus.cadastroBolsistas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/*
 * professor/coordenador com id UUID.
 */
@Entity
@Table(name = "professor")
public class Professor extends Usuario {

    public Professor() {
        super();
        setTipoUsuario("PROFESSOR");
    }

    public Professor(UUID id, String nome, String email, String senha, boolean ativo, String fotoUrl) {
        super(id, nome, email, senha, ativo, "PROFESSOR", fotoUrl, null);
    }
}
