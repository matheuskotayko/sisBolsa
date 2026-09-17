package dev.matheus.cadastroBolsistas.repository;

import dev.matheus.cadastroBolsistas.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/*
 * queries analiticas da tela de relatorios com IDs UUID.
 */
@Repository
public interface RelatorioRepository extends JpaRepository<Laboratorio, UUID> {

    interface HorasBolsista {
        String getNome();
        Double getTotalHoras();
    }

    interface ProjetosPorLaboratorio {
        String getNome();
        Long getTotalProjetos();
    }

    interface BolsistasPorCargo {
        String getCargo();
        Long getTotalBolsistas();
    }

    interface OcupacaoLaboratorio {
        UUID getId();
        String getNome();
        Integer getCapacidade();
        Integer getTotalBolsistas();

        default double getPercentualOcupacao() {
            int cap = getCapacidade() != null ? getCapacidade() : 0;
            if (cap <= 0) {
                return 0.0;
            }
            int total = getTotalBolsistas() != null ? getTotalBolsistas() : 0;
            return (total / (double) cap) * 100.0;
        }
    }

    @Query(value = "SELECT b.nome AS nome, SUM(f.horas_trabalhadas) AS totalHoras "
            + "FROM frequencia f "
            + "JOIN bolsista b ON f.bolsista_id = b.id "
            + "WHERE f.ativo = true AND b.ativo = true "
            + "  AND EXTRACT(MONTH FROM f.data) = EXTRACT(MONTH FROM CURRENT_DATE) "
            + "  AND EXTRACT(YEAR FROM f.data) = EXTRACT(YEAR FROM CURRENT_DATE) "
            + "GROUP BY b.nome ORDER BY totalHoras DESC", nativeQuery = true)
    List<HorasBolsista> horasBolsistasMesCorrente();

    @Query(value = "SELECT l.nome AS nome, COUNT(p.id) AS totalProjetos "
            + "FROM laboratorio l "
            + "LEFT JOIN projeto p ON p.laboratorio_id = l.id AND p.ativo = true "
            + "WHERE l.ativo = true "
            + "GROUP BY l.nome ORDER BY totalProjetos DESC", nativeQuery = true)
    List<ProjetosPorLaboratorio> projetosAtivosPorLaboratorio();

    @Query(value = "SELECT cargo AS cargo, COUNT(*) AS totalBolsistas "
            + "FROM bolsista WHERE ativo = true AND cargo IS NOT NULL "
            + "GROUP BY cargo ORDER BY totalBolsistas DESC", nativeQuery = true)
    List<BolsistasPorCargo> bolsistasPorCargo();

    @Query(value = "SELECT l.id AS id, l.nome AS nome, l.capacidade AS capacidade, "
            + "       COUNT(b.id) AS totalBolsistas "
            + "FROM laboratorio l "
            + "LEFT JOIN bolsista b ON b.laboratorio_id = l.id AND b.ativo = true "
            + "WHERE l.ativo = true "
            + "GROUP BY l.id, l.nome, l.capacidade ORDER BY l.nome", nativeQuery = true)
    List<OcupacaoLaboratorio> laboratoriosOcupacao();
}
