import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, Plus, Edit2, Trash2, Loader2 } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { laboratorioService } from '../services/laboratorioService';
import { usuarioService } from '../services/usuarioService';
import type { Laboratorio, LaboratorioRequest, LaboratorioStatus, Usuario, Paginacao } from '../types';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Pagination } from '../components/ui/Pagination';

export const Laboratorios: React.FC = () => {
  const { canManage, isAdmin } = useAuth();
  const { showToast } = useToast();

  /*
   * a busca abaixo e client-side sobre a pagina inteira - o backend nao tem
   * filtro por nome pra laboratorios. por isso pedimos uma pagina grande
   * (tamanho maximo) em vez de paginar de fato: hoje o numero de laboratorios
   * de um departamento fica bem abaixo disso, entao na pratica isso continua
   * trazendo "tudo" como antes. o <Pagination> abaixo so aparece se um dia
   * passar dos 200 registros.
   */
  const [paginacao, setPaginacao] = useState<Paginacao<Laboratorio>>({
    itens: [],
    totalItens: 0,
    pagina: 1,
    totalPaginas: 1,
  });
  const [professores, setProfessores] = useState<Usuario[]>([]);
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(1);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<LaboratorioRequest>({
    nome: '',
    areaPesquisa: '',
    status: 'Ativo',
    capacidade: 10,
    coordenadorId: null,
  });
  const [saving, setSaving] = useState(false);

  const carregarDados = async (pag = pagina) => {
    setLoading(true);
    try {
      const [labs, usersData] = await Promise.all([
        laboratorioService.listar({ pagina: pag, tamanho: 200 }),
        isAdmin ? usuarioService.listar({ tipo: 'PROFESSOR', tamanho: 100 }) : Promise.resolve({ itens: [] }),
      ]);
      setPaginacao(labs);
      setProfessores(usersData.itens || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar laboratórios';
      showToast(msg, 'erro');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

  const handleOpenCreate = () => {
    setEditingId(null);
    setFormData({
      nome: '',
      areaPesquisa: '',
      status: 'Ativo',
      capacidade: 10,
      coordenadorId: professores[0]?.id || null,
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (lab: Laboratorio) => {
    setEditingId(lab.id);
    setFormData({
      nome: lab.nome,
      areaPesquisa: lab.areaPesquisa,
      status: lab.status,
      capacidade: lab.capacidade,
      coordenadorId: lab.coordenadorId,
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (lab: Laboratorio) => {
    if (!window.confirm(`Deseja realmente desativar o laboratório "${lab.nome}"?`)) return;
    try {
      await laboratorioService.excluir(lab.id);
      showToast('Laboratório desativado com sucesso!', 'sucesso');
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao excluir laboratório';
      showToast(msg, 'erro');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.nome.trim()) {
      showToast('O nome do laboratório é obrigatório.', 'erro');
      return;
    }

    setSaving(true);
    try {
      if (editingId) {
        await laboratorioService.atualizar(editingId, formData);
        showToast('Laboratório atualizado com sucesso!', 'sucesso');
      } else {
        await laboratorioService.criar(formData);
        showToast('Laboratório cadastrado com sucesso!', 'sucesso');
      }
      setIsModalOpen(false);
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao salvar laboratório';
      showToast(msg, 'erro');
    } finally {
      setSaving(false);
    }
  };

  const labsFiltrados = paginacao.itens.filter(
    (l) =>
      l.nome.toLowerCase().includes(busca.toLowerCase()) ||
      l.areaPesquisa.toLowerCase().includes(busca.toLowerCase()) ||
      (l.coordenador && l.coordenador.toLowerCase().includes(busca.toLowerCase()))
  );

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Laboratórios de Pesquisa</h1>
          <p className="header-subtitle">
            Gestão de infraestrutura, capacidade de bolsistas e áreas de conhecimento
          </p>
        </div>
        {canManage && (
          <div className="header-buttons">
            <button type="button" className="btn-new btn-create" onClick={handleOpenCreate}>
              <Plus size={16} />
              <span>Novo Laboratório</span>
            </button>
          </div>
        )}
      </div>

      <div className="container">
        <div className="search-section">
          <div className="search-toolbar">
            <div className="search-field">
              <input
                type="text"
                className="search-input"
                placeholder="Pesquisar laboratório por nome, área ou coordenador..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
              />
            </div>
            {busca && (
              <button
                type="button"
                className="reset-button"
                onClick={() => setBusca('')}
              >
                Limpar
              </button>
            )}
          </div>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Laboratório</th>
                <th>Área de Pesquisa</th>
                <th>Coordenador</th>
                <th>Capacidade / Ocupação</th>
                <th>Status</th>
                {canManage && <th>Ações</th>}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Carregando laboratórios...
                  </td>
                </tr>
              ) : labsFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={6} className="empty-state-cell">
                    Nenhum laboratório encontrado.
                  </td>
                </tr>
              ) : (
                labsFiltrados.map((lab) => {
                  const percentual = Math.min(100, Math.round(((lab.totalBolsistas || 0) / (lab.capacidade || 1)) * 100));
                  return (
                    <tr key={lab.id}>
                      <td>
                        <Link
                          to={`/laboratorios/${lab.id}`}
                          style={{ color: 'var(--primary-color)', fontWeight: 600, textDecoration: 'none' }}
                        >
                          {lab.nome}
                        </Link>
                      </td>
                      <td>{lab.areaPesquisa}</td>
                      <td>{lab.coordenador || '---'}</td>
                      <td style={{ minWidth: '160px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '4px' }}>
                          <span>{lab.totalBolsistas || 0} / {lab.capacidade} bolsistas</span>
                          <span>{percentual}%</span>
                        </div>
                        <div className="progress-bar-container" style={{ margin: 0 }}>
                          <div
                            className={`progress-bar-fill ${percentual > 90 ? 'danger' : percentual > 70 ? 'warning' : 'success'}`}
                            style={{ width: `${percentual}%` }}
                          />
                        </div>
                      </td>
                      <td>
                        <Badge type="status" value={lab.status} />
                      </td>
                      {canManage && (
                        <td>
                          <div className="actions-cell">
                            <button
                              type="button"
                              className="btn-icon action-link-edit"
                              onClick={() => handleOpenEdit(lab)}
                              title="Editar laboratório"
                              aria-label={`Editar ${lab.nome}`}
                            >
                              <Edit2 size={16} />
                            </button>
                            <button
                              type="button"
                              className="btn-icon action-link-delete"
                              onClick={() => handleDelete(lab)}
                              title="Excluir laboratório"
                              aria-label={`Excluir ${lab.nome}`}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          page={paginacao.pagina}
          totalPages={paginacao.totalPaginas}
          onPageChange={(novaPag) => {
            setPagina(novaPag);
            carregarDados(novaPag);
          }}
        />
      </div>

      {/* Modal Criar / Editar Laboratório */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingId ? 'Editar Laboratório' : 'Cadastrar Novo Laboratório'}
        icon={<Building2 size={20} />}
      >
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="lab-nome">
                Nome do Laboratório <span className="asterisco">*</span>
              </label>
              <input
                id="lab-nome"
                type="text"
                required
                placeholder="Ex: Laboratório de Inteligência Artificial"
                value={formData.nome}
                onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
              />
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="lab-area">
                Área de Pesquisa <span className="asterisco">*</span>
              </label>
              <input
                id="lab-area"
                type="text"
                required
                placeholder="Ex: Ciência da Computação / Robótica"
                value={formData.areaPesquisa}
                onChange={(e) => setFormData({ ...formData, areaPesquisa: e.target.value })}
              />
            </div>

            {isAdmin && professores.length > 0 && (
              <div className="form-group" style={{ marginBottom: '14px' }}>
                <label htmlFor="lab-coord">Professor Coordenador</label>
                <select
                  id="lab-coord"
                  value={formData.coordenadorId || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      coordenadorId: e.target.value || null,
                    })
                  }
                >
                  <option value="">Selecione um professor...</option>
                  {professores.map((prof) => (
                    <option key={prof.id} value={prof.id}>
                      {prof.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label htmlFor="lab-capacidade">
                  Capacidade Máxima <span className="asterisco">*</span>
                </label>
                <input
                  id="lab-capacidade"
                  type="number"
                  required
                  min={1}
                  max={200}
                  value={formData.capacidade}
                  onChange={(e) =>
                    setFormData({ ...formData, capacidade: Number(e.target.value) })
                  }
                />
              </div>

              <div className="form-group">
                <label htmlFor="lab-status">
                  Status <span className="asterisco">*</span>
                </label>
                <select
                  id="lab-status"
                  value={formData.status}
                  onChange={(e) =>
                    setFormData({ ...formData, status: e.target.value as LaboratorioStatus })
                  }
                >
                  <option value="Ativo">Ativo</option>
                  <option value="Em Pausa">Em Pausa</option>
                  <option value="Concluido">Concluído</option>
                </select>
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-cancel"
              onClick={() => setIsModalOpen(false)}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-submit"
              disabled={saving}
            >
              {saving ? <Loader2 size={16} className="spin" /> : null}
              <span>{editingId ? 'Salvar Alterações' : 'Cadastrar Laboratório'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
