import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  FolderKanban,
  Building2,
  Users,
  UserPlus,
  ArrowLeft,
  Trash2,
  User,
  Loader2,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { projetoService } from '../services/projetoService';
import { laboratorioService } from '../services/laboratorioService';
import type { Projeto, MembroProjeto, Usuario } from '../types';
import { Modal } from '../components/ui/Modal';

export const ProjetoDetalhes: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const projId = id || '';

  const { canManage } = useAuth();
  const { showToast } = useToast();

  const [projeto, setProjeto] = useState<Projeto | null>(null);
  const [membros, setMembros] = useState<MembroProjeto[]>([]);
  const [bolsistasLab, setBolsistasLab] = useState<Usuario[]>([]);
  const [loading, setLoading] = useState(true);

  // Bind Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedBolsistaId, setSelectedBolsistaId] = useState<string>('');
  const [saving, setSaving] = useState(false);

  const carregarDados = async () => {
    if (!projId) return;
    setLoading(true);
    try {
      const [proj, memb] = await Promise.all([
        projetoService.buscarPorId(projId),
        projetoService.listarMembros(projId),
      ]);
      setProjeto(proj);
      setMembros(memb);

      if (proj.laboratorioId) {
        const bols = await laboratorioService.listarBolsistas(proj.laboratorioId);
        setBolsistasLab(bols);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao carregar detalhes do projeto';
      showToast(msg, 'erro');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, [projId]);

  const handleVincular = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBolsistaId) {
      showToast('Selecione um bolsista para vincular.', 'erro');
      return;
    }

    setSaving(true);
    try {
      await projetoService.vincularBolsista(projId, selectedBolsistaId);
      showToast('Bolsista vinculado ao projeto com sucesso!', 'sucesso');
      setIsModalOpen(false);
      setSelectedBolsistaId('');
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao vincular bolsista';
      showToast(msg, 'erro');
    } finally {
      setSaving(false);
    }
  };

  const handleDesvincular = async (membro: MembroProjeto) => {
    if (!window.confirm(`Deseja realmente desvincular ${membro.nome} deste projeto?`)) return;
    try {
      await projetoService.desvincularBolsista(projId, membro.id);
      showToast('Bolsista desvinculado com sucesso!', 'sucesso');
      carregarDados();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro ao desvincular bolsista';
      showToast(msg, 'erro');
    }
  };

  if (loading && !projeto) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
        Carregando detalhes do projeto...
      </div>
    );
  }

  if (!projeto) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '48px' }}>
        <h2>Projeto não encontrado</h2>
        <Link to="/projetos" className="btn btn-secondary" style={{ marginTop: '16px' }}>
          <ArrowLeft size={16} />
          <span>Voltar para Projetos</span>
        </Link>
      </div>
    );
  }

  const idsMembros = new Set(membros.map((m) => m.id));
  const bolsistasDisponiveis = bolsistasLab.filter((b) => !idsMembros.has(b.id));

  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <Link
          to="/projetos"
          className="btn btn-secondary"
          style={{ padding: '6px 12px', fontSize: '0.825rem' }}
        >
          <ArrowLeft size={14} />
          <span>Voltar aos Projetos</span>
        </Link>
      </div>

      <div className="header-actions">
        <div>
          <h1>{projeto.nome}</h1>
          <p className="header-subtitle" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Building2 size={16} style={{ color: 'var(--primary-color)' }} />
            <span>Laboratório: <strong>{projeto.nomeLaboratorio}</strong></span>
          </p>
          {(projeto.linkRepositorio || projeto.linkDocumentacao) && (
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginTop: '10px' }}>
              {projeto.linkRepositorio && (
                <a
                  href={projeto.linkRepositorio}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn btn-secondary"
                  style={{ padding: '4px 10px', fontSize: '0.8rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                >
                  <span>Repositório GitHub / GitLab</span>
                  <ArrowLeft size={12} style={{ transform: 'rotate(135deg)' }} />
                </a>
              )}
              {projeto.linkDocumentacao && (
                <a
                  href={projeto.linkDocumentacao}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn btn-secondary"
                  style={{ padding: '4px 10px', fontSize: '0.8rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                >
                  <span>Artigo / Documentação Overleaf</span>
                  <ArrowLeft size={12} style={{ transform: 'rotate(135deg)' }} />
                </a>
              )}
            </div>
          )}
        </div>

        {canManage && (
          <div className="header-buttons">
            <button
              type="button"
              className="btn-new btn-create"
              onClick={() => setIsModalOpen(true)}
            >
              <UserPlus size={16} />
              <span>Vincular Bolsista</span>
            </button>
          </div>
        )}
      </div>

      {/* Descrição do Projeto */}
      <div className="container" style={{ marginBottom: '24px' }}>
        <h2 className="section-title">
          <FolderKanban size={20} />
          <span>Objetivo e Metas da Pesquisa</span>
        </h2>
        <p style={{ margin: 0, color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
          {projeto.descricao || 'Nenhuma descrição detalhada informada.'}
        </p>
      </div>

      {/* Equipe do Projeto */}
      <div className="container">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2 className="section-title" style={{ margin: 0 }}>
            <Users size={20} />
            <span>Bolsistas Alocados ({membros.length})</span>
          </h2>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Curso</th>
                <th>Cargo</th>
                <th>E-mail</th>
                {canManage && <th>Ações</th>}
              </tr>
            </thead>
            <tbody>
              {membros.length === 0 ? (
                <tr>
                  <td colSpan={5} className="empty-state-cell">
                    Nenhum bolsista vinculado a este projeto ainda.
                  </td>
                </tr>
              ) : (
                membros.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {m.fotoUrl ? (
                          <img src={m.fotoUrl} alt={m.nome} className="profile-img" style={{ width: '32px', height: '32px' }} />
                        ) : (
                          <div className="profile-placeholder" style={{ width: '32px', height: '32px' }}>
                            <User size={16} />
                          </div>
                        )}
                        <strong>{m.nome}</strong>
                      </div>
                    </td>
                    <td>{m.curso || '---'}</td>
                    <td>
                      <span className="count-badge count-badge-purple">
                        {m.cargo || 'Pesquisador'}
                      </span>
                    </td>
                    <td>{m.email}</td>
                    {canManage && (
                      <td>
                        <button
                          type="button"
                          className="btn-icon action-link-delete"
                          onClick={() => handleDesvincular(m)}
                          title="Desvincular do projeto"
                          aria-label={`Desvincular ${m.nome}`}
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Vincular Bolsista */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Vincular Bolsista ao Projeto"
        icon={<UserPlus size={20} />}
      >
        <form onSubmit={handleVincular}>
          <div className="modal-body">
            {bolsistasDisponiveis.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', margin: 0 }}>
                Todos os bolsistas deste laboratório já estão vinculados ao projeto ou não há bolsistas cadastrados no laboratório.
              </p>
            ) : (
              <div className="form-group">
                <label htmlFor="select-bolsista">
                  Selecione o Bolsista <span className="asterisco">*</span>
                </label>
                <select
                  id="select-bolsista"
                  required
                  value={selectedBolsistaId}
                  onChange={(e) => setSelectedBolsistaId(e.target.value)}
                >
                  <option value="">Selecione um pesquisador...</option>
                  {bolsistasDisponiveis.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.nome} ({b.cargo || 'Bolsista'} - {b.curso || 'Graduação'})
                    </option>
                  ))}
                </select>
              </div>
            )}
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
              disabled={saving || bolsistasDisponiveis.length === 0}
            >
              {saving ? <Loader2 size={16} className="spin" /> : null}
              <span>Vincular ao Projeto</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
