import React, { useEffect, useState } from 'react';
import {
  Building2,
  Users,
  FolderKanban,
  Clock,
  Briefcase,
  Download,
  BarChart2,
  PieChart as PieChartIcon,
  Table as TableIcon,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { useToast } from '../contexts/ToastContext';
import { useTheme } from '../contexts/ThemeContext';
import { BASE_URL } from '../services/api';
import {
  relatorioService,
  type ResumoAdmin,
  type OcupacaoLab,
  type HorasBolsistaMes,
  type ProjetosPorLab,
  type BolsistasPorCargo,
} from '../services/relatorioService';

const CARGO_MAP: Record<string, string> = {
  DESENVOLVEDOR: 'Desenvolvedor',
  PESQUISADOR: 'Pesquisador',
  LIDER_TECNICO: 'Líder Técnico',
  DESIGNER: 'Designer',
  AUXILIAR: 'Auxiliar',
};

const PIE_COLORS = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899', '#6366f1'];

export const Relatorios: React.FC = () => {
  const { showToast } = useToast();
  const { theme } = useTheme();

  const [resumo, setResumo] = useState<ResumoAdmin>({
    totalBolsistas: 0,
    totalLaboratorios: 0,
    totalProjetos: 0,
  });
  const [ocupacoes, setOcupacoes] = useState<OcupacaoLab[]>([]);
  const [horasMes, setHorasMes] = useState<HorasBolsistaMes[]>([]);
  const [projetosPorLab, setProjetosPorLab] = useState<ProjetosPorLab[]>([]);
  const [bolsistasPorCargo, setBolsistasPorCargo] = useState<BolsistasPorCargo[]>([]);
  const [loading, setLoading] = useState(true);
  const [modoExibicao, setModoExibicao] = useState<'graficos' | 'tabelas'>('graficos');

  const isDark = theme === 'dark';
  const gridColor = isDark ? '#243044' : '#e2e8f0';
  const textColor = isDark ? '#94a3b8' : '#64748b';
  const tooltipBg = isDark ? '#151d2c' : '#ffffff';
  const tooltipBorder = isDark ? '#243044' : '#e2e8f0';

  useEffect(() => {
    async function carregarRelatorios() {
      setLoading(true);
      try {
        const [res, ocu, hMes, pLab, bCargo] = await Promise.all([
          relatorioService.obterResumo().catch(() => ({ totalBolsistas: 0, totalLaboratorios: 0, totalProjetos: 0 })),
          relatorioService.obterOcupacao().catch(() => []),
          relatorioService.obterHorasMes().catch(() => []),
          relatorioService.obterProjetosPorLab().catch(() => []),
          relatorioService.obterBolsistasPorCargo().catch(() => []),
        ]);

        setResumo(res);
        setOcupacoes(ocu);
        setHorasMes(hMes);
        setProjetosPorLab(pLab);
        setBolsistasPorCargo(bCargo);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Erro ao carregar dados do relatório';
        showToast(msg, 'erro');
      } finally {
        setLoading(false);
      }
    }

    carregarRelatorios();
  }, []);

  const dadosOcupacaoChart = ocupacoes.map((item) => ({
    nome: item.nome.length > 20 ? `${item.nome.substring(0, 18)}...` : item.nome,
    nomeCompleto: item.nome,
    Bolsistas: item.totalBolsistas,
    Capacidade: item.capacidade,
    Ocupacao: Math.round(item.percentualOcupacao || 0),
  }));

  const dadosCargoChart = bolsistasPorCargo.map((item) => ({
    name: CARGO_MAP[item.cargo] || item.cargo,
    value: item.totalBolsistas,
  }));

  const dadosHorasChart = horasMes.map((item) => ({
    nome: item.nome.split(' ')[0],
    nomeCompleto: item.nome,
    Horas: item.totalHoras,
  }));

  const dadosProjetosChart = projetosPorLab.map((item) => ({
    nome: item.nome.length > 18 ? `${item.nome.substring(0, 16)}...` : item.nome,
    nomeCompleto: item.nome,
    Projetos: item.totalProjetos,
  }));

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Relatórios e Auditoria</h1>
          <p className="header-subtitle">
            Métricas analíticas de ocupação, produtividade de horas e distribuição da pesquisa
          </p>
        </div>
        <div className="header-buttons">
          <div style={{ display: 'flex', gap: '4px', background: 'var(--surface-color)', padding: '3px', borderRadius: '8px', border: '1px solid var(--border-grid)' }}>
            <button
              type="button"
              className={`btn ${modoExibicao === 'graficos' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '6px 12px', fontSize: '0.8rem' }}
              onClick={() => setModoExibicao('graficos')}
            >
              <BarChart2 size={14} />
              <span>Gráficos</span>
            </button>
            <button
              type="button"
              className={`btn ${modoExibicao === 'tabelas' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '6px 12px', fontSize: '0.8rem' }}
              onClick={() => setModoExibicao('tabelas')}
            >
              <TableIcon size={14} />
              <span>Tabelas</span>
            </button>
          </div>

          <a
            href={`${BASE_URL}/usuarios/exportar`}
            className="btn-new btn-export"
            target="_blank"
            rel="noreferrer"
          >
            <Download size={16} />
            <span>Exportar Bolsistas</span>
          </a>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total de Bolsistas</h3>
          <div className="value">{loading ? '...' : resumo.totalBolsistas}</div>
          <p className="stat-desc">
            <Users size={14} />
            <span>Pesquisadores ativos</span>
          </p>
        </div>

        <div className="stat-card">
          <h3>Laboratórios Vinculados</h3>
          <div className="value">{loading ? '...' : resumo.totalLaboratorios}</div>
          <p className="stat-desc">
            <Building2 size={14} />
            <span>Centros de desenvolvimento</span>
          </p>
        </div>

        <div className="stat-card">
          <h3>Projetos Ativos</h3>
          <div className="value">{loading ? '...' : resumo.totalProjetos}</div>
          <p className="stat-desc">
            <FolderKanban size={14} />
            <span>Linhas de pesquisa</span>
          </p>
        </div>
      </div>

      {modoExibicao === 'graficos' ? (
        /* ================= VISUALIZAÇÃO EM GRÁFICOS (RECHARTS) ================= */
        <>
          {/* Linha 1: Ocupação dos Labs e Distribuição por Cargos */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px', marginBottom: '24px' }}>
            {/* Gráfico de Ocupação */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <Building2 size={20} />
                <span>Ocupação dos Laboratórios (Bolsistas x Capacidade)</span>
              </h2>
              {dadosOcupacaoChart.length === 0 ? (
                <div className="empty-state-cell">Nenhum dado disponível.</div>
              ) : (
                <div style={{ width: '100%', height: 280 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dadosOcupacaoChart} margin={{ top: 20, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                      <XAxis dataKey="nome" stroke={textColor} fontSize={12} />
                      <YAxis stroke={textColor} fontSize={12} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          borderColor: tooltipBorder,
                          borderRadius: '8px',
                          color: 'var(--text-main)',
                        }}
                      />
                      <Legend />
                      <Bar dataKey="Bolsistas" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="Capacidade" fill="#94a3b8" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            {/* Gráfico de Cargos (Donut Chart) */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <PieChartIcon size={20} />
                <span>Distribuição de Integrantes por Cargo</span>
              </h2>
              {dadosCargoChart.length === 0 ? (
                <div className="empty-state-cell">Nenhum dado disponível.</div>
              ) : (
                <div style={{ width: '100%', height: 280 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={dadosCargoChart}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={95}
                        paddingAngle={5}
                        dataKey="value"
                        label={({ name, percent }: { name?: string; percent?: number }) => `${name || ''} (${((percent || 0) * 100).toFixed(0)}%)`}
                        labelLine={false}
                      >
                        {dadosCargoChart.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          borderColor: tooltipBorder,
                          borderRadius: '8px',
                          color: 'var(--text-main)',
                        }}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>
          </div>

          {/* Linha 2: Horas no Mês e Projetos por Lab */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px' }}>
            {/* Gráfico de Horas Apontadas */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <Clock size={20} />
                <span>Horas Apontadas no Mês Vigente por Bolsista</span>
              </h2>
              {dadosHorasChart.length === 0 ? (
                <div className="empty-state-cell">Nenhum apontamento registrado no ciclo atual.</div>
              ) : (
                <div style={{ width: '100%', height: 260 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dadosHorasChart} margin={{ top: 20, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                      <XAxis dataKey="nome" stroke={textColor} fontSize={12} />
                      <YAxis stroke={textColor} fontSize={12} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          borderColor: tooltipBorder,
                          borderRadius: '8px',
                          color: 'var(--text-main)',
                        }}
                      />
                      <Bar dataKey="Horas" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>

            {/* Gráfico de Projetos por Lab */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <FolderKanban size={20} />
                <span>Projetos Ativos por Laboratório</span>
              </h2>
              {dadosProjetosChart.length === 0 ? (
                <div className="empty-state-cell">Nenhum dado disponível.</div>
              ) : (
                <div style={{ width: '100%', height: 260 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dadosProjetosChart} margin={{ top: 20, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                      <XAxis dataKey="nome" stroke={textColor} fontSize={12} />
                      <YAxis stroke={textColor} fontSize={12} allowDecimals={false} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: tooltipBg,
                          borderColor: tooltipBorder,
                          borderRadius: '8px',
                          color: 'var(--text-main)',
                        }}
                      />
                      <Bar dataKey="Projetos" fill="#10b981" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </div>
          </div>
        </>
      ) : (
        /* ================= VISUALIZAÇÃO EM TABELAS ================= */
        <>
          {/* Grid 2 colunas superior */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px', marginBottom: '24px' }}>
            {/* Ocupação dos Labs */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <Building2 size={20} />
                <span>Ocupação por Laboratório</span>
              </h2>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Laboratório</th>
                      <th>Ocupação</th>
                      <th>Percentual</th>
                    </tr>
                  </thead>
                  <tbody>
                    {ocupacoes.length === 0 ? (
                      <tr>
                        <td colSpan={3} className="empty-state-cell">
                          Nenhum dado disponível.
                        </td>
                      </tr>
                    ) : (
                      ocupacoes.map((item) => {
                        const percentualArredondado = Math.round(item.percentualOcupacao || 0);
                        return (
                          <tr key={item.id}>
                            <td><strong>{item.nome}</strong></td>
                            <td>{item.totalBolsistas} / {item.capacidade}</td>
                            <td style={{ minWidth: '140px' }}>
                              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '4px' }}>
                                <span>{percentualArredondado}%</span>
                              </div>
                              <div className="progress-bar-container" style={{ margin: 0 }}>
                                <div
                                  className={`progress-bar-fill ${percentualArredondado > 90 ? 'danger' : percentualArredondado > 70 ? 'warning' : 'success'}`}
                                  style={{ width: `${Math.min(100, percentualArredondado)}%` }}
                                />
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Horas no Mês por Bolsista */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <Clock size={20} />
                <span>Horas Apontadas no Mês Vigente</span>
              </h2>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Bolsista</th>
                      <th>Horas Dedicadas</th>
                    </tr>
                  </thead>
                  <tbody>
                    {horasMes.length === 0 ? (
                      <tr>
                        <td colSpan={2} className="empty-state-cell">
                          Nenhum apontamento no mês atual.
                        </td>
                      </tr>
                    ) : (
                      horasMes.map((h, i) => (
                        <tr key={i}>
                          <td><strong>{h.nome}</strong></td>
                          <td>
                            <span className="count-badge count-badge-purple">
                              {h.totalHoras} {h.totalHoras === 1 ? 'hora' : 'horas'}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Grid 2 colunas inferior */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px' }}>
            {/* Projetos por Laboratório */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <FolderKanban size={20} />
                <span>Projetos Ativos por Laboratório</span>
              </h2>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Laboratório</th>
                      <th>Quantidade de Projetos</th>
                    </tr>
                  </thead>
                  <tbody>
                    {projetosPorLab.length === 0 ? (
                      <tr>
                        <td colSpan={2} className="empty-state-cell">
                          Nenhum dado disponível.
                        </td>
                      </tr>
                    ) : (
                      projetosPorLab.map((p, i) => (
                        <tr key={i}>
                          <td><strong>{p.nome}</strong></td>
                          <td>
                            <span className="count-badge count-badge-purple">
                              {p.totalProjetos} {p.totalProjetos === 1 ? 'projeto' : 'projetos'}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Bolsistas por Cargo */}
            <div className="container" style={{ margin: 0 }}>
              <h2 className="section-title">
                <Briefcase size={20} />
                <span>Distribuição de Bolsistas por Cargo</span>
              </h2>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Cargo / Função</th>
                      <th>Total de Bolsistas</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bolsistasPorCargo.length === 0 ? (
                      <tr>
                        <td colSpan={2} className="empty-state-cell">
                          Nenhum dado disponível.
                        </td>
                      </tr>
                    ) : (
                      bolsistasPorCargo.map((c, i) => (
                        <tr key={i}>
                          <td><strong>{CARGO_MAP[c.cargo] || c.cargo}</strong></td>
                          <td>
                            <span className="count-badge count-badge-purple">
                              {c.totalBolsistas} {c.totalBolsistas === 1 ? 'integrante' : 'integrantes'}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
