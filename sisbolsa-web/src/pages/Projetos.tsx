import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  FolderKanban,
  Plus,
  Edit2,
  Trash2,
  Building2,
  Loader2,
  GitBranch,
  ExternalLink,
  FileText,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { projetoService } from '../services/projetoService';
import { laboratorioService } from '../services/laboratorioService';
import type { Projeto, ProjetoRequest, Laboratorio, Paginacao } from '../types';
import { Modal } from '../components/ui/Modal';
import { Pagination } from '../components/ui/Pagination';

export const Projetos: React.FC = () => {
  const { canManage } = useAuth();
  const { showToast } = useToast();

  const [paginacao, setPaginacao] = useState<Paginacao<Projeto>>({
    itens: [],
    totalItens: 0,
    pagina: 1,
    totalPaginas: 1,
  });
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([]);
  const [buscaNome, setBuscaNome] = useState('');
  const [filtroLab, setFiltroLab] = useState('');
  const [pagina, setPagina] = useState(1);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<ProjetoRequest>({
    nome: '',
    descricao: '',
    laboratorioId: '',
    linkRepositorio: '',
    linkDocumentacao: '',
  });
  const [saving, setSaving] = useState(false);

  const carregarDados = async (pag = pagina) => {
    setLoading(true);
    try {
      const [projs, labs] = await Promise.all([
        projetoService.listar({
          buscaNome: buscaNome.trim() || undefined,
          labId: filtroLab || undefined,
          pagina: pag,
        }),
        laboratorioService.listarTodos(),
      ]);
      setPaginacao(projs);
      setLaboratorios(labs);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar projetos';
      showToast(msg, 'erro');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setPagina(1);
    carregarDados(1);
  }, [buscaNome, filtroLab]);

  const handleOpenCreate = () => {
    setEditingId(null);
    setFormData({
      nome: '',
      descricao: '',
      laboratorioId: laboratorios[0]?.id || '',
      linkRepositorio: '',
      linkDocumentacao: '',
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (proj: Projeto) => {
    setEditingId(proj.id);
    setFormData({
      nome: proj.nome,
      descricao: proj.descricao || '',
      laboratorioId: proj.laboratorioId,
      linkRepositorio: proj.linkRepositorio || '',
      linkDocumentacao: proj.linkDocumentacao || '',
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (proj: Projeto) => {
    if (!window.confirm(`Deseja realmente desativar o projeto "${proj.nome}"?`)) return;
    try {
      await projetoService.excluir(proj.id);
      showToast('Projeto desativado com sucesso!', 'sucesso');
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao excluir projeto';
      showToast(msg, 'erro');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.nome.trim()) {
      showToast('O título do projeto é obrigatório.', 'erro');
      return;
    }
    if (!formData.laboratorioId) {
      showToast('Selecione um laboratório para o projeto.', 'erro');
      return;
    }

    setSaving(true);
    try {
      if (editingId) {
        await projetoService.atualizar(editingId, formData);
        showToast('Projeto atualizado com sucesso!', 'sucesso');
      } else {
        await projetoService.criar(formData);
        showToast('Projeto cadastrado com sucesso!', 'sucesso');
      }
      setIsModalOpen(false);
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao salvar projeto';
      showToast(msg, 'erro');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="header-actions">
        <div>
          <h1>Projetos de Pesquisa</h1>
          <p className="header-subtitle">
            Acompanhamento de planos de trabalho, bolsistas alocados e entregáveis
          </p>
        </div>
        {canManage && (
          <div className="header-buttons">
            <button type="button" className="btn-new btn-create" onClick={handleOpenCreate}>
              <Plus size={16} />
              <span>Novo Projeto</span>
            </button>
          </div>
        )}
      </div>

      <div className="container">
        <div className="search-section">
          <form
            className="search-toolbar"
            onSubmit={(e) => {
              e.preventDefault();
              carregarDados();
            }}
          >
            <div className="search-field" style={{ flex: '1 1 240px' }}>
              <input
                type="text"
                placeholder="Buscar por nome ou descrição do projeto..."
                value={buscaNome}
                onChange={(e) => setBuscaNome(e.target.value)}
              />
            </div>

            {laboratorios.length > 0 && (
              <div className="search-field" style={{ maxWidth: '280px' }}>
                <select
                  value={filtroLab}
                  onChange={(e) => setFiltroLab(e.target.value)}
                >
                  <option value="">Todos os Laboratórios</option>
                  {laboratorios.map((lab) => (
                    <option key={lab.id} value={lab.id}>
                      {lab.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <button type="submit" className="search-button">
              Buscar
            </button>

            {(buscaNome || filtroLab) && (
              <button
                type="button"
                className="reset-button"
                onClick={() => {
                  setBuscaNome('');
                  setFiltroLab('');
                }}
              >
                Limpar Filtros
              </button>
            )}
          </form>
        </div>

        {/* Projects Grid */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
            <Loader2 className="spin" size={28} style={{ margin: '0 auto 12px' }} />
            <p>Carregando projetos de pesquisa...</p>
          </div>
        ) : paginacao.itens.length === 0 ? (
          <div className="empty-state-cell" style={{ padding: '40px', textAlign: 'center' }}>
            <FolderKanban size={48} style={{ margin: '0 auto 12px', color: 'var(--text-muted)' }} />
            <p>Nenhum projeto encontrado com os filtros aplicados.</p>
          </div>
        ) : (
          <div className="projects-grid">
            {paginacao.itens.map((proj) => (
              <div key={proj.id} className="project-card">
                <div>
                  <div className="project-card-header">
                    <h3>
                      <Link
                        to={`/projetos/${proj.id}`}
                        style={{ color: 'var(--text-main)', textDecoration: 'none' }}
                      >
                        {proj.nome}
                      </Link>
                    </h3>
                  </div>

                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      fontSize: '0.8rem',
                      color: 'var(--primary-color)',
                      fontWeight: 600,
                      marginBottom: '10px',
                    }}
                  >
                    <Building2 size={14} />
                    <span>{proj.nomeLaboratorio}</span>
                  </div>

                  <p className="project-desc">
                    {proj.descricao || 'Sem descrição detalhada.'}
                  </p>

                  {/* External Links */}
                  {(proj.linkRepositorio || proj.linkDocumentacao) && (
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '12px', marginBottom: '8px' }}>
                      {proj.linkRepositorio && (
                        <a
                          href={proj.linkRepositorio}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="count-badge count-badge-purple"
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            textDecoration: 'none',
                            fontSize: '0.75rem',
                            padding: '4px 8px',
                          }}
                        >
                          <GitBranch size={12} />
                          <span>Repositório</span>
                          <ExternalLink size={10} />
                        </a>
                      )}
                      {proj.linkDocumentacao && (
                        <a
                          href={proj.linkDocumentacao}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="count-badge"
                          style={{
                            backgroundColor: '#f1f5f9',
                            color: '#475569',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            textDecoration: 'none',
                            fontSize: '0.75rem',
                            padding: '4px 8px',
                          }}
                        >
                          <FileText size={12} />
                          <span>Artigo / Docs</span>
                          <ExternalLink size={10} />
                        </a>
                      )}
                    </div>
                  )}
                </div>

                <div className="project-card-footer">
                  <Link
                    to={`/projetos/${proj.id}`}
                    className="btn btn-secondary"
                    style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                  >
                    <span>Ver Equipe ({proj.totalMembros ?? proj.membros?.length ?? 0})</span>
                  </Link>

                  {canManage && (
                    <div className="actions-cell">
                      <button
                        type="button"
                        className="btn-icon action-link-edit"
                        onClick={() => handleOpenEdit(proj)}
                        title="Editar projeto"
                        aria-label={`Editar ${proj.nome}`}
                      >
                        <Edit2 size={16} />
                      </button>
                      <button
                        type="button"
                        className="btn-icon action-link-delete"
                        onClick={() => handleDelete(proj)}
                        title="Excluir projeto"
                        aria-label={`Excluir ${proj.nome}`}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        <Pagination
          page={paginacao.pagina}
          totalPages={paginacao.totalPaginas}
          onPageChange={(novaPag) => {
            setPagina(novaPag);
            carregarDados(novaPag);
          }}
        />
      </div>

      {/* Modal Criar / Editar Projeto */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingId ? 'Editar Projeto' : 'Cadastrar Novo Projeto'}
        icon={<FolderKanban size={20} />}
      >
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="proj-lab">
                Laboratório Vinculado <span className="asterisco">*</span>
              </label>
              <select
                id="proj-lab"
                required
                value={formData.laboratorioId || ''}
                onChange={(e) =>
                  setFormData({ ...formData, laboratorioId: e.target.value })
                }
              >
                <option value="">Selecione um laboratório...</option>
                {laboratorios.map((lab) => (
                  <option key={lab.id} value={lab.id}>
                    {lab.nome}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="proj-titulo">
                Título do Projeto <span className="asterisco">*</span>
              </label>
              <input
                id="proj-titulo"
                type="text"
                required
                placeholder="Ex: Processamento de Linguagem Natural para Saúde"
                value={formData.nome}
                onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
              />
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="proj-descricao">Descrição / Resumo Científico</label>
              <textarea
                id="proj-descricao"
                rows={3}
                placeholder="Descreva as metas e atividades científicas..."
                value={formData.descricao || ''}
                onChange={(e) => setFormData({ ...formData, descricao: e.target.value })}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label htmlFor="proj-repo">Link do Repositório (GitHub / GitLab)</label>
                <input
                  id="proj-repo"
                  type="url"
                  placeholder="https://github.com/usuario/projeto"
                  value={formData.linkRepositorio || ''}
                  onChange={(e) => setFormData({ ...formData, linkRepositorio: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label htmlFor="proj-doc">Link do Artigo / Documentação (Overleaf/Docs)</label>
                <input
                  id="proj-doc"
                  type="url"
                  placeholder="https://overleaf.com/read/..."
                  value={formData.linkDocumentacao || ''}
                  onChange={(e) => setFormData({ ...formData, linkDocumentacao: e.target.value })}
                />
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
            <button type="submit" className="btn btn-submit" disabled={saving}>
              {saving ? <Loader2 size={16} className="spin" /> : null}
              <span>{editingId ? 'Salvar Alterações' : 'Cadastrar Projeto'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
