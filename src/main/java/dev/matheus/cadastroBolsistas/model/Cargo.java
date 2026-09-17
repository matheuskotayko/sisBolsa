package dev.matheus.cadastroBolsistas.model;

/**
 * Representa os cargos fixos que um usuário pode ter em um laboratório.
 */
public enum Cargo {
    DESENVOLVEDOR("Desenvolvedor"),
    PESQUISADOR("Pesquisador"),
    LIDER_TECNICO("Líder Técnico"),
    DESIGNER("Designer"),
    AUXILIAR("Auxiliar de Laboratório");

    private final String descricao;

    Cargo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Converte uma string (nome do enum ou descrição) para o enum correspondente.
     */
    public static Cargo deString(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String limpo = valor.trim();
        for (Cargo c : Cargo.values()) {
            if (c.name().equalsIgnoreCase(limpo) || c.getDescricao().equalsIgnoreCase(limpo)) {
                return c;
            }
        }
        return null;
    }
}
