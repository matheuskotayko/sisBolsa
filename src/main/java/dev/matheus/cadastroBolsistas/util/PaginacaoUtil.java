package dev.matheus.cadastroBolsistas.util;

import dev.matheus.cadastroBolsistas.dto.PaginaResponse;

import java.util.List;
import java.util.function.Function;

/*
 * pagina em memoria uma lista ja carregada do banco. usado pelos endpoints
 * cujo volume de dados nao justifica LIMIT/OFFSET no banco (projetos,
 * laboratorios, usuarios) - todos seguem o mesmo formato de PaginaResponse.
 */
public class PaginacaoUtil {

    private PaginacaoUtil() {
    }

    public static <T, R> PaginaResponse<R> paginar(List<T> lista, int pagina, Integer tamanhoPedido,
                                                    int tamanhoPadrao, int tamanhoMaximo, Function<T, R> mapper) {
        int tamanho = tamanhoPedido != null && tamanhoPedido > 0
                ? Math.min(tamanhoPedido, tamanhoMaximo)
                : tamanhoPadrao;
        int total = lista.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) tamanho));
        int atual = Math.min(Math.max(pagina, 1), totalPaginas);
        int de = (atual - 1) * tamanho;
        int ate = Math.min(de + tamanho, total);

        List<R> itens = de < total
                ? lista.subList(de, ate).stream().map(mapper).toList()
                : List.of();
        return new PaginaResponse<>(itens, atual, totalPaginas, total);
    }

    /* usado pelos endpoints que paginam via LIMIT/OFFSET no banco (frequencias, auditoria) */
    public static int totalPaginas(int total, int tamanhoPagina) {
        return Math.max(1, (int) Math.ceil(total / (double) tamanhoPagina));
    }

    public static int paginaValida(int pagina, int totalPaginas) {
        return Math.min(Math.max(pagina, 1), totalPaginas);
    }
}
