package dev.matheus.cadastroBolsistas.util;

import java.security.SecureRandom;

/**
 * Utilitário centralizado para geração de identificadores públicos seguros
 * no estilo Stripe (prefixo semântico + sequência aleatória alfanumérica URL-safe).
 * Exemplo: bol_k8s2M4n9P1q3W5z7A0b2
 */
public final class PublicIdGenerator {

    public static final String PREFIX_USUARIO = "usr";
    public static final String PREFIX_ADMINISTRADOR = "adm";
    public static final String PREFIX_PROFESSOR = "prf";
    public static final String PREFIX_BOLSISTA = "bol";
    public static final String PREFIX_LABORATORIO = "lab";
    public static final String PREFIX_PROJETO = "prj";
    public static final String PREFIX_CURSO = "cur";
    public static final String PREFIX_FREQUENCIA = "frq";

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_RANDOM_LENGTH = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIdGenerator() {
        // Utilitário estático
    }

    public static String generate(String prefix) {
        return generate(prefix, DEFAULT_RANDOM_LENGTH);
    }

    public static String generate(String prefix, int randomLength) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefixo do public ID não pode ser vazio.");
        }
        if (randomLength < 8) {
            throw new IllegalArgumentException("Comprimento aleatório mínimo deve ser de 8 caracteres.");
        }

        StringBuilder sb = new StringBuilder(prefix.trim().toLowerCase());
        sb.append('_');
        for (int i = 0; i < randomLength; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public static String generateUsuarioId() {
        return generate(PREFIX_USUARIO);
    }

    public static String generateAdministradorId() {
        return generate(PREFIX_ADMINISTRADOR);
    }

    public static String generateProfessorId() {
        return generate(PREFIX_PROFESSOR);
    }

    public static String generateBolsistaId() {
        return generate(PREFIX_BOLSISTA);
    }

    public static String generateLaboratorioId() {
        return generate(PREFIX_LABORATORIO);
    }

    public static String generateProjetoId() {
        return generate(PREFIX_PROJETO);
    }

    public static String generateCursoId() {
        return generate(PREFIX_CURSO);
    }

    public static String generateFrequenciaId() {
        return generate(PREFIX_FREQUENCIA);
    }

    public static boolean isValid(String publicId, String expectedPrefix) {
        if (publicId == null || publicId.isBlank() || expectedPrefix == null || expectedPrefix.isBlank()) {
            return false;
        }
        String prefix = expectedPrefix.toLowerCase() + "_";
        if (!publicId.startsWith(prefix)) {
            return false;
        }
        String randomPart = publicId.substring(prefix.length());
        if (randomPart.isEmpty()) {
            return false;
        }
        for (char c : randomPart.toCharArray()) {
            if (ALPHABET.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }
}
