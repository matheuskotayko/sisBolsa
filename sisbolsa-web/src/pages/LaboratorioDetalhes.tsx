import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  FolderKanban,
  Users,
  Plus,
  ArrowLeft,
  Loader2,
  Trash2,
  User,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { laboratorioService } from '../services/laboratorioService';
import { projetoService } from '../services/projetoService';
import type { Laboratorio, Projeto, Usuario, ProjetoRequest } from '../types';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';

export const LaboratorioDetalhes: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const labId = id || '';

  const { canManage } = useAuth();
  const { showToast } = useToast();

  const [laboratorio, setLaboratorio] = useState<Laboratorio | null>(null);
  const [projetos, setProjetos] = useState<Projeto[]>([]);
  const [bolsistas, setBolsistas] = useState<Usuario[]>([]);
  const [loading, setLoading] = useState(true);

  // Project Modal State
  const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);
  const [projectForm, setProjectForm] = useState<ProjetoRequest>({
    nome: '',
    descricao: '',
    laboratorioId: labId,
  });
  const [savingProject, setSavingProject] = useState(false);

  const carregarDados = async () => {
    if (!labId) return;
    setLoading(true);
    try {
      const [lab, projs, bols] = await Promise.all([
        laboratorioService.buscarPorId(labId),
        laboratorioService.listarProjetos(labId),
        laboratorioService.listarBolsistas(labId),
      ]);
      setLaboratorio(lab);
      setProjetos(projs);
      setBolsistas(bols);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar detalhes do laboratório';
      showToast(msg, 'erro');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, [labId]);

  const handleCreateProject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectForm.nome.trim()) {
      showToast('O título do projeto é obrigatório.', 'erro');
      return;
    }

    setSavingProject(true);
    try {
      await projetoService.criar({ ...projectForm, laboratorioId: labId });
      showToast('Projeto criado com sucesso!', 'sucesso');
      setIsProjectModalOpen(false);
      setProjectForm({ nome: '', descricao: '', laboratorioId: labId });
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao criar projeto';
      showToast(msg, 'erro');
    } finally {
      setSavingProject(false);
    }
  };

  const handleDeleteProject = async (proj: Projeto) => {
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

  if (loading && !laboratorio) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
        Carregando detalhes do laboratório...
      </div>
    );
  }

  if (!laboratorio) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '48px' }}>
        <h2>Laboratório não encontrado</h2>
        <Link to="/laboratorios" className="btn btn-secondary" style={{ marginTop: '16px' }}>
          <ArrowLeft size={16} />
          <span>Voltar para a lista</span>
        </Link>
      </div>
    );
  }

  const percentual = Math.min(
    100,
    Math.round(((laboratorio.totalBolsistas || 0) / (laboratorio.capacidade || 1)) * 100)
  );

  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <Link
          to="/laboratorios"
          className="btn btn-secondary"
          style={{ padding: '6px 12px', fontSize: '0.825rem' }}
        >
          <ArrowLeft size={14} />
          <span>Voltar aos Laboratórios</span>
        </Link>
      </div>

      <div className="header-actions">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '6px' }}>
            <h1>{laboratorio.nome}</h1>
            <Badge type="status" value={laboratorio.status} />
          </div>
          <p className="header-subtitle">
            <strong>Área:</strong> {laboratorio.areaPesquisa} • <strong>Coordenador:</strong>{' '}
            {laboratorio.coordenador || 'Não atribuído'}
          </p>
        </div>

        {canManage && (
          <div className="header-buttons">
            <button
              type="button"
              className="btn-new btn-create"
              onClick={() => setIsProjectModalOpen(true)}
            >
              <Plus size={16} />
              <span>Novo Projeto</span>
            </button>
          </div>
        )}
      </div>

      {/* Info Card */}
      <div className="container" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>
            Ocupação do Laboratório: {laboratorio.totalBolsistas || 0} de {laboratorio.capacidade} bolsas ocupadas
          </span>
          <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>{percentual}%</span>
        </div>
        <div className="progress-bar-container" style={{ margin: 0, height: '10px' }}>
          <div
            className={`progress-bar-fill ${percentual > 90 ? 'danger' : percentual > 70 ? 'warning' : 'success'}`}
            style={{ width: `${percentual}%` }}
          />
        </div>
      </div>

      {/* Projetos Section */}
      <div className="container" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 className="section-title" style={{ margin: 0 }}>
            <FolderKanban size={20} />
            <span>Projetos Desenvolvidos ({projetos.length})</span>
          </h2>
        </div>

        {projetos.length === 0 ? (
          <div className="empty-state-cell">
            <FolderKanban size={36} />
            <p>Nenhum projeto cadastrado neste laboratório.</p>
          </div>
        ) : (
          <div className="projects-grid">
            {projetos.map((proj) => (
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
                  <p className="project-desc">
                    {proj.descricao || 'Sem descrição cadastrada.'}
                  </p>
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
                    <button
                      type="button"
                      className="btn-icon action-link-delete"
                      onClick={() => handleDeleteProject(proj)}
                      title="Excluir projeto"
                      aria-label={`Excluir ${proj.nome}`}
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Bolsistas Section */}
      <div className="container">
        <h2 className="section-title">
          <Users size={20} />
          <span>Bolsistas Integrantes ({bolsistas.length})</span>
        </h2>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Curso</th>
                <th>Cargo</th>
                <th>E-mail</th>
              </tr>
            </thead>
            <tbody>
              {bolsistas.length === 0 ? (
                <tr>
                  <td colSpan={4} className="empty-state-cell">
                    Nenhum bolsista vinculado a este laboratório no momento.
                  </td>
                </tr>
              ) : (
                bolsistas.map((b) => (
                  <tr key={b.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {b.fotoUrl ? (
                          <img src={b.fotoUrl} alt={b.nome} className="profile-img" style={{ width: '32px', height: '32px' }} />
                        ) : (
                          <div className="profile-placeholder" style={{ width: '32px', height: '32px' }}>
                            <User size={16} />
                          </div>
                        )}
                        <strong>{b.nome}</strong>
                      </div>
                    </td>
                    <td>{b.curso || '---'}</td>
                    <td>
                      <span className="count-badge count-badge-purple">
                        {b.cargo || 'Bolsista'}
                      </span>
                    </td>
                    <td>{b.email}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Criar Novo Projeto */}
      <Modal
        isOpen={isProjectModalOpen}
        onClose={() => setIsProjectModalOpen(false)}
        title="Cadastrar Novo Projeto"
        icon={<FolderKanban size={20} />}
      >
        <form onSubmit={handleCreateProject}>
          <div className="modal-body">
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label htmlFor="proj-nome">
                Título do Projeto <span className="asterisco">*</span>
              </label>
              <input
                id="proj-nome"
                type="text"
                required
                placeholder="Ex: Sistema de Monitoramento com IoT"
                value={projectForm.nome}
                onChange={(e) => setProjectForm({ ...projectForm, nome: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label htmlFor="proj-desc">Descrição dos Objetivos</label>
              <textarea
                id="proj-desc"
                rows={4}
                placeholder="Descreva o escopo e as metas científicas deste projeto..."
                value={projectForm.descricao || ''}
                onChange={(e) => setProjectForm({ ...projectForm, descricao: e.target.value })}
              />
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-cancel"
              onClick={() => setIsProjectModalOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn-submit" disabled={savingProject}>
              {savingProject ? <Loader2 size={16} className="spin" /> : null}
              <span>Cadastrar Projeto</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
