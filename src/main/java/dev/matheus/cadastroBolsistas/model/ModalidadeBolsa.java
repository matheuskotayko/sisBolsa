package dev.matheus.cadastroBolsistas.model;

public enum ModalidadeBolsa {
    PIBIC("PIBIC - Iniciação Científica"),
    PIBITI("PIBITI - Desenvolvimento Tecnológico"),
    EXTENSAO("Bolsa de Extensão"),
    MONITORIA("Monitoria Acadêmica"),
    INSTITUCIONAL("Bolsa Institucional"),
    VOLUNTARIO("Pesquisador Voluntário");

    private final String descricao;

    ModalidadeBolsa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static ModalidadeBolsa deString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return ModalidadeBolsa.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
