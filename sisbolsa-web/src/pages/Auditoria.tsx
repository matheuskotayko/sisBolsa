import React, { useEffect, useState } from 'react';
import {
  ShieldAlert,
  Download,
  Filter,
  Loader2,
  Clock,
  User,
} from 'lucide-react';
import { useToast } from '../contexts/ToastContext';
import { auditoriaService } from '../services/auditoriaService';
import type { Auditoria, Paginacao } from '../types';
import { Pagination } from '../components/ui/Pagination';

export const AuditoriaPage: React.FC = () => {
  const { showToast } = useToast();

  const [paginacao, setPaginacao] = useState<Paginacao<Auditoria>>({
    itens: [],
    totalItens: 0,
    pagina: 1,
    totalPaginas: 1,
  });

  const [filtroEntidade, setFiltroEntidade] = useState('');
  const [dataInicio, setDataInicio] = useState('');
  const [dataFim, setDataFim] = useState('');
  const [pagina, setPagina] = useState(1);
  const [loading, setLoading] = useState(true);

  const carregarLogs = async (pag = pagina) => {
    setLoading(true);
    try {
      const dados = await auditoriaService.listar({
        pagina: pag,
        entidade: filtroEntidade || undefined,
        dataInicio: dataInicio || undefined,
        dataFim: dataFim || undefined,
      });
      setPaginacao(dados);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar logs de auditoria';
      showToast(msg, 'erro');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setPagina(1);
    carregarLogs(1);
  }, [filtroEntidade]);

  const formatarDataHora = (iso: string) => {
    if (!iso) return '-';
    try {
      const d = new Date(iso);
      return d.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return iso;
    }
  };

  const getAcaoBadge = (acao: string) => {
    if (acao.startsWith('CRIAR') || acao.startsWith('VINCULAR')) {
      return (
        <span className="badge status-ativo">
          {acao}
        </span>
      );
    }
    if (acao.startsWith('EXCLUIR') || acao.startsWith('DESVINCULAR') || acao.includes('FALHA')) {
      return (
        <span className="badge" style={{ backgroundColor: 'var(--danger-bg)', color: 'var(--danger-color)', border: '1px solid var(--danger-border)' }}>
          {acao}
        </span>
      );
    }
    if (acao.startsWith('ATUALIZAR')) {
      return (
        <span className="badge status-em-pausa">
          {acao}
        </span>
      );
    }
    return (
      <span className="badge badge-bolsista">
        {acao}
      </span>
    );
  };

  const exportUrl = auditoriaService.exportarUrl({
    entidade: filtroEntidade || undefined,
    dataInicio: dataInicio || undefined,
    dataFim: dataFim || undefined,
  });

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Trilha de Auditoria</h1>
          <p className="header-subtitle">
            Histórico cronológico de eventos operacionais, autenticações e alterações administrativas
          </p>
        </div>
        <div className="header-buttons">
          <a
            href={exportUrl}
            className="btn-new btn-export"
            target="_blank"
            rel="noreferrer"
          >
            <Download size={16} />
            <span>Exportar CSV</span>
          </a>
        </div>
      </div>

      <div className="container">
        {/* Filtros de Auditoria */}
        <div style={{ marginBottom: '20px' }}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setPagina(1);
              carregarLogs(1);
            }}
            style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}
          >
            <div style={{ flex: '1 1 180px' }}>
              <select
                value={filtroEntidade}
                onChange={(e) => setFiltroEntidade(e.target.value)}
                style={{ width: '100%', margin: 0 }}
              >
                <option value="">Todas as Entidades</option>
                <option value="AUTH">Autenticação (AUTH)</option>
                <option value="USUARIO">Usuários</option>
                <option value="LABORATORIO">Laboratórios</option>
                <option value="PROJETO">Projetos</option>
                <option value="FREQUENCIA">Frequência</option>
              </select>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flex: '1 1 180px' }}>
              <label htmlFor="audit-inicio" style={{ fontSize: '0.85rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', margin: 0 }}>
                De:
              </label>
              <input
                id="audit-inicio"
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                style={{ width: '100%', margin: 0 }}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flex: '1 1 180px' }}>
              <label htmlFor="audit-fim" style={{ fontSize: '0.85rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', margin: 0 }}>
                Até:
              </label>
              <input
                id="audit-fim"
                type="date"
                value={dataFim}
                onChange={(e) => setDataFim(e.target.value)}
                style={{ width: '100%', margin: 0 }}
              />
            </div>

            <button type="submit" className="btn btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <Filter size={14} />
              <span>Filtrar</span>
            </button>

            {(filtroEntidade || dataInicio || dataFim) && (
              <button
                type="button"
                className="btn btn-cancel"
                onClick={() => {
                  setFiltroEntidade('');
                  setDataInicio('');
                  setDataFim('');
                  setPagina(1);
                  auditoriaService.listar({ pagina: 1 }).then((d) => setPaginacao(d));
                }}
              >
                Limpar
              </button>
            )}
          </form>
        </div>

        {/* Tabela de Logs */}
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Data / Hora</th>
                <th>Usuário</th>
                <th>Ação</th>
                <th>Entidade</th>
                <th>Detalhes da Operação</th>
                <th>IP</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    <Loader2 size={20} className="spin" style={{ margin: '0 auto 8px' }} />
                    <div>Carregando trilha de auditoria...</div>
                  </td>
                </tr>
              ) : paginacao.itens.length === 0 ? (
                <tr>
                  <td colSpan={6} className="empty-state-cell">
                    <ShieldAlert size={36} style={{ margin: '0 auto 8px', color: 'var(--text-tertiary)' }} />
                    <div>Nenhum registro de auditoria encontrado para os filtros selecionados.</div>
                  </td>
                </tr>
              ) : (
                paginacao.itens.map((log) => (
                  <tr key={log.id}>
                    <td style={{ whiteSpace: 'nowrap', fontSize: '0.825rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Clock size={13} style={{ color: 'var(--text-tertiary)' }} />
                        <span>{formatarDataHora(log.dataHora)}</span>
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <User size={13} style={{ color: 'var(--text-muted)' }} />
                        <strong>{log.usuarioNome || 'Sistema'}</strong>
                      </div>
                    </td>
                    <td>{getAcaoBadge(log.acao)}</td>
                    <td>
                      <span className="count-badge" style={{ backgroundColor: 'var(--bg-color)', color: 'var(--text-secondary)', border: '1px solid var(--border-grid)' }}>
                        {log.entidade}
                      </span>
                    </td>
                    <td style={{ maxWidth: '320px', lineHeight: 1.4, fontSize: '0.85rem' }}>
                      {log.detalhes || '-'}
                    </td>
                    <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {log.ipOrigem || '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          page={paginacao.pagina}
          totalPages={paginacao.totalPaginas}
          onPageChange={(novaPag) => {
            setPagina(novaPag);
            carregarLogs(novaPag);
          }}
        />
      </div>
    </div>
  );
};
