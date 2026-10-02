import React, { useEffect, useState } from 'react';
import {
  Clock,
  Plus,
  Edit2,
  Trash2,
  Calendar,
  Loader2,
  CheckCircle2,
  Download,
  Filter,
  ExternalLink,
  FileText,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { BASE_URL } from '../services/api';
import { frequenciaService } from '../services/frequenciaService';
import { usuarioService } from '../services/usuarioService';
import type { Frequencia, FrequenciaRequest, FrequenciaResumo, Paginacao, Usuario } from '../types';
import { Modal } from '../components/ui/Modal';
import { FormSection } from '../components/ui/FormSection';
import { Pagination } from '../components/ui/Pagination';

export const FrequenciaPage: React.FC = () => {
  const { user, isBolsista, canManage } = useAuth();
  const { showToast } = useToast();

  const [paginacao, setPaginacao] = useState<Paginacao<Frequencia>>({
    itens: [],
    totalItens: 0,
    pagina: 1,
    totalPaginas: 1,
  });

  const [resumo, setResumo] = useState<FrequenciaResumo>({ horasMes: 0, horasTotal: 0 });
  const [bolsistas, setBolsistas] = useState<Usuario[]>([]);
  const [filtroBolsista, setFiltroBolsista] = useState<string>('');
  const [dataInicio, setDataInicio] = useState<string>('');
  const [dataFim, setDataFim] = useState<string>('');
  const [pagina, setPagina] = useState(1);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<FrequenciaRequest>({
    bolsistaId: isBolsista ? user?.id : null,
    data: new Date().toISOString().split('T')[0],
    horasTrabalhadas: 4,
    descricao: '',
    linkComprovante: '',
  });
  const [saving, setSaving] = useState(false);

  const carregarFrequencias = async (pag = pagina) => {
    setLoading(true);
    try {
      const bolsistaParam = filtroBolsista || (isBolsista ? user?.id : undefined);
      const [dados, resumoData] = await Promise.all([
        frequenciaService.listar({
          bolsistaId: bolsistaParam,
          dataInicio: dataInicio || undefined,
          dataFim: dataFim || undefined,
          pagina: pag,
        }),
        frequenciaService.obterResumo(bolsistaParam),
      ]);
      setPaginacao(dados);
      setResumo(resumoData);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar registros de frequência';
      showToast(msg, 'erro');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    async function carregarAux() {
      if (canManage) {
        try {
          const u = await usuarioService.listar({ tipo: 'BOLSISTA', tamanho: 100 });
          setBolsistas(u.itens || []);
        } catch {
          // ignore
        }
      }
    }
    carregarAux();
  }, [canManage]);

  useEffect(() => {
    setPagina(1);
    carregarFrequencias(1);
  }, [filtroBolsista]);

  const handleOpenCreate = () => {
    setEditingId(null);
    setFormData({
      bolsistaId: isBolsista ? user?.id : bolsistas[0]?.id || null,
      data: new Date().toISOString().split('T')[0],
      horasTrabalhadas: 4,
      descricao: '',
      linkComprovante: '',
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (f: Frequencia) => {
    setEditingId(f.id);
    setFormData({
      bolsistaId: f.bolsistaId,
      data: f.data,
      horasTrabalhadas: f.horasTrabalhadas,
      descricao: f.descricao,
      linkComprovante: f.linkComprovante || '',
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (f: Frequencia) => {
    if (!window.confirm(`Deseja realmente desativar este registro de ${f.horasTrabalhadas}h do dia ${f.data}?`)) return;
    try {
      await frequenciaService.excluir(f.id);
      showToast('Registro de frequência desativado com sucesso!', 'sucesso');
      carregarFrequencias(pagina);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao excluir frequência';
      showToast(msg, 'erro');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.data) {
      showToast('A data do apontamento é obrigatória.', 'erro');
      return;
    }
    if (formData.horasTrabalhadas <= 0 || formData.horasTrabalhadas > 24) {
      showToast('Informe uma quantidade válida de horas trabalhadas (1 a 24).', 'erro');
      return;
    }
    if (!formData.descricao.trim()) {
      showToast('A descrição da atividade realizada é obrigatória.', 'erro');
      return;
    }

    setSaving(true);
    try {
      if (editingId) {
        await frequenciaService.atualizar(editingId, formData);
        showToast('Registro de horas atualizado com sucesso!', 'sucesso');
      } else {
        await frequenciaService.criar(formData);
        showToast('Horas registradas com sucesso!', 'sucesso');
      }
      setIsModalOpen(false);
      carregarFrequencias(pagina);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao salvar horas';
      showToast(msg, 'erro');
    } finally {
      setSaving(false);
    }
  };

  const getExportUrl = () => {
    const params = new URLSearchParams();
    const bolsistaParam = filtroBolsista || (isBolsista ? user?.id : undefined);
    if (bolsistaParam) params.append('bolsistaId', bolsistaParam);
    if (dataInicio) params.append('dataInicio', dataInicio);
    if (dataFim) params.append('dataFim', dataFim);
    const qs = params.toString();
    return `${BASE_URL}/frequencias/exportar${qs ? `?${qs}` : ''}`;
  };

  const getPdfUrl = () => {
    const bolsistaParam = filtroBolsista || (isBolsista ? user?.id : undefined);
    return frequenciaService.obterUrlPdf({
      bolsistaId: bolsistaParam || undefined,
      dataInicio: dataInicio || undefined,
      dataFim: dataFim || undefined,
    });
  };

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Controle de Frequência</h1>
          <p className="header-subtitle">
            {isBolsista
              ? 'Aponte suas horas de pesquisa dedicadas e acompanhe seu total mensal'
              : 'Supervisão e auditoria de horas dedicadas pelos bolsistas'}
          </p>
        </div>
        <div className="header-buttons">
          <a
            href={getPdfUrl()}
            className="btn-new btn-export"
            target="_blank"
            rel="noreferrer"
            title="Emitir Comprovante Mensal em PDF"
          >
            <FileText size={16} />
            <span>Comprovante PDF</span>
          </a>

          <a
            href={getExportUrl()}
            className="btn-new btn-export"
            target="_blank"
            rel="noreferrer"
          >
            <Download size={16} />
            <span>Exportar CSV</span>
          </a>

          <button type="button" className="btn-new btn-create" onClick={handleOpenCreate}>
            <Plus size={16} />
            <span>Registrar Horas</span>
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total no Mês Atual</h3>
          <div className="value">{resumo.horasMes}h</div>
          <p className="stat-desc">
            <CheckCircle2 size={14} style={{ color: 'var(--success-color)' }} />
            <span>Horas validadas no ciclo vigente</span>
          </p>
        </div>

        <div className="stat-card">
          <h3>Histórico Total Acumulado</h3>
          <div className="value">{resumo.horasTotal}h</div>
          <p className="stat-desc">
            <Clock size={14} />
            <span>Somatório geral de frequência</span>
          </p>
        </div>
      </div>

      <div className="container">
        {/* Barra de Filtros: Bolsista e Período de Datas */}
        <div style={{ marginBottom: '20px' }}>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setPagina(1);
              carregarFrequencias(1);
            }}
            style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}
          >
            {canManage && bolsistas.length > 0 && (
              <div style={{ flex: '1 1 200px' }}>
                <select
                  value={filtroBolsista}
                  onChange={(e) => setFiltroBolsista(e.target.value)}
                  style={{ width: '100%', margin: 0 }}
                >
                  <option value="">Todos os Bolsistas</option>
                  {bolsistas.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flex: '1 1 180px' }}>
              <label htmlFor="freq-filtro-inicio" style={{ fontSize: '0.85rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', margin: 0 }}>
                De:
              </label>
              <input
                id="freq-filtro-inicio"
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                style={{ width: '100%', margin: 0 }}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flex: '1 1 180px' }}>
              <label htmlFor="freq-filtro-fim" style={{ fontSize: '0.85rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', margin: 0 }}>
                Até:
              </label>
              <input
                id="freq-filtro-fim"
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

            {(dataInicio || dataFim || filtroBolsista) && (
              <button
                type="button"
                className="btn btn-cancel"
                onClick={() => {
                  setDataInicio('');
                  setDataFim('');
                  setFiltroBolsista('');
                  setPagina(1);
                  frequenciaService.listar({
                    bolsistaId: isBolsista ? user?.id : undefined,
                    pagina: 1,
                  }).then((d) => setPaginacao(d));
                }}
              >
                Limpar
              </button>
            )}
          </form>
        </div>

        {/* Tabela de Frequência */}
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Data</th>
                {!isBolsista && <th>Bolsista</th>}
                <th>Horas</th>
                <th>Descrição & Entregáveis</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Carregando registros...
                  </td>
                </tr>
              ) : paginacao.itens.length === 0 ? (
                <tr>
                  <td colSpan={5} className="empty-state-cell">
                    Nenhum registro de frequência encontrado para o período selecionado.
                  </td>
                </tr>
              ) : (
                paginacao.itens.map((f) => (
                  <tr key={f.id}>
                    <td style={{ whiteSpace: 'nowrap', fontWeight: 600 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Calendar size={14} style={{ color: 'var(--text-tertiary)' }} />
                        <span>{f.data}</span>
                      </div>
                    </td>
                    {!isBolsista && <td><strong>{f.nomeBolsista}</strong></td>}
                    <td>
                      <span className="count-badge count-badge-purple" style={{ fontSize: '0.85rem' }}>
                        {f.horasTrabalhadas}h
                      </span>
                    </td>
                    <td>
                      <div>{f.descricao}</div>
                      {f.linkComprovante && (
                        <div style={{ marginTop: '6px' }}>
                          <a
                            href={f.linkComprovante}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                              fontSize: '0.78rem',
                              color: 'var(--primary-color)',
                              textDecoration: 'none',
                              fontWeight: 600,
                            }}
                          >
                            <ExternalLink size={12} />
                            <span>Ver Entregável / PR / Commit</span>
                          </a>
                        </div>
                      )}
                    </td>
                    <td>
                      <div className="actions-cell">
                        <button
                          type="button"
                          className="btn-icon action-link-edit"
                          onClick={() => handleOpenEdit(f)}
                          title="Editar horas"
                          aria-label="Editar registro"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          type="button"
                          className="btn-icon action-link-delete"
                          onClick={() => handleDelete(f)}
                          title="Excluir horas"
                          aria-label="Excluir registro"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
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
            carregarFrequencias(novaPag);
          }}
        />
      </div>

      {/* Modal Registrar / Editar Frequência */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingId ? 'Editar Registro de Horas' : 'Registrar Horas de Pesquisa'}
        icon={<Clock size={20} />}
      >
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <FormSection title="Apontamento de Frequência">
            {canManage && bolsistas.length > 0 && (
              <div className="form-group" style={{ marginBottom: '14px' }}>
                <label htmlFor="freq-bolsista">
                  Bolsista <span className="asterisco">*</span>
                </label>
                <select
                  id="freq-bolsista"
                  required
                  value={formData.bolsistaId || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      bolsistaId: e.target.value || null,
                    })
                  }
                >
                  <option value="">Selecione o bolsista...</option>
                  {bolsistas.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '14px' }}>
              <div className="form-group">
                <label htmlFor="freq-data">
                  Data do Apontamento <span className="asterisco">*</span>
                </label>
                <input
                  id="freq-data"
                  type="date"
                  required
                  value={formData.data}
                  onChange={(e) => setFormData({ ...formData, data: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label htmlFor="freq-horas">
                  Horas Dedicadas <span className="asterisco">*</span>
                </label>
                <input
                  id="freq-horas"
                  type="number"
                  required
                  min={0.5}
                  max={24}
                  step={0.5}
                  value={formData.horasTrabalhadas}
                  onChange={(e) =>
                    setFormData({ ...formData, horasTrabalhadas: Number(e.target.value) })
                  }
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="freq-desc">
                Descrição das Atividades Realizadas <span className="asterisco">*</span>
              </label>
              <textarea
                id="freq-desc"
                rows={3}
                required
                placeholder="Descreva detalhadamente as tarefas, experimentos ou códigos desenvolvidos..."
                value={formData.descricao}
                onChange={(e) => setFormData({ ...formData, descricao: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label htmlFor="freq-link">
                Link do Entregável / PR / Commit (opcional)
              </label>
              <input
                id="freq-link"
                type="url"
                placeholder="https://github.com/usuario/repo/pull/123"
                value={formData.linkComprovante || ''}
                onChange={(e) => setFormData({ ...formData, linkComprovante: e.target.value })}
              />
            </div>
            </FormSection>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-cancel"
              onClick={() => setIsModalOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-submit" disabled={saving}>
              {saving ? <Loader2 size={16} className="spin" /> : null}
              <span>{editingId ? 'Salvar Alterações' : 'Confirmar Apontamento'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
